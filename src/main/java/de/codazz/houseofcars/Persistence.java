package de.codazz.houseofcars;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.EntityManager;
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
public class Persistence implements Closeable {
    ExecutorService executor = Executors.newSingleThreadExecutor(); // package scope and non-final for tests

    private final EntityManager entityManager;

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

    /** run async */
    public <T> Future<T> submit(final Function<EntityManager, T> function) {
        return executor.submit(() -> function.apply(entityManager));
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
        entityManager.close();
    }
}
