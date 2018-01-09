package de.codazz.houseofcars;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Runs transactions on a dedicated thread.
 * <p><strong>
 * If your sync calls never return you're
 * probably nesting calls, causing a deadlock.
 * </strong></p>
 * @author rstumm2s */
// TODO consumer alternatives
public class Persistence implements Closeable {
    ExecutorService executor = Executors.newSingleThreadExecutor(); // package scope and non-final for tests

    private volatile EntityManager entityManager;

    public Persistence(final String jdbcUrl, final String jdbcUser, final String jdbcPassword) {
        final Map<String, Object> factoryProps = new HashMap<>();
        factoryProps.put(AvailableSettings.JPA_JDBC_URL, jdbcUrl);
        factoryProps.put(AvailableSettings.JPA_JDBC_DRIVER, "org.postgresql.Driver");
        factoryProps.put(AvailableSettings.JPA_JDBC_USER, jdbcUser);
        factoryProps.put(AvailableSettings.JPA_JDBC_PASSWORD, jdbcPassword);
        entityManager = new HibernatePersistenceProvider()
            .createContainerEntityManagerFactory(
                new PerstistenceUnitInfoImpl(HibernatePersistenceProvider.class.getName()),
                factoryProps)
            .createEntityManager();
    }

    public void refresh() {
        try {
            final EntityManagerFactory factory = entityManager.getEntityManagerFactory();
            executor.submit(() -> {
                if (entityManager != null) {
                    entityManager.close();
                }
                entityManager = factory.createEntityManager();
            }).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private volatile Thread thread;

    /** run async */
    public <T> Future<T> submit(final Function<EntityManager, T> function) {
        if (Thread.currentThread() == thread) throw new IllegalStateException("dead-lock! nested call to single thread executor");
        return executor.submit(() -> {
            thread = Thread.currentThread();
            try {
                return function.apply(entityManager);
            } finally {
                thread = null;
            }
        });
    }

    /** wait for the function to return */
    public <T> T execute(final Function<EntityManager, T> function) {
        return await(submit(function));
    }

    /** like {@link #submit(Function)} but async */
    public <T> Future<T> submitTransact(final BiFunction<EntityManager, EntityTransaction, T> transact) {
        return submit(em -> {
            final EntityTransaction transaction = em.getTransaction();
            final T result; {
                if (!transaction.isActive()) {
                    transaction.begin();
                }
                result = transact.apply(em, transaction);
                transaction.commit();
            }
            return result;
        });
    }

    /** {@link EntityTransaction#begin() Begin} a transaction,
     * wait for the given function to return and {@link EntityTransaction#commit() commit}. */
    public <T> T transact(final BiFunction<EntityManager, EntityTransaction, T> transact) {
        return await(submitTransact(transact));
    }

    private static <T> T await(final Future<T> future) {
        try {
            return future.get();
        } catch (final InterruptedException ignore) {
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void close() {
        executor.shutdown();
        entityManager.getEntityManagerFactory().close();
        entityManager.close();
    }
}
