package de.codazz.houseofcars;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** runs code on the persistence context thread
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
                        new PerstistenceUnitInfoImpl(
                                "HouseOfCars",
                                HibernatePersistenceProvider.class.getName()),
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
