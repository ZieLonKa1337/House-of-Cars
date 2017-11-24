package de.codazz.houseofcars;

import javax.persistence.EntityManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/** @author rstumm2s */
public interface Garage extends Runnable, Closeable {
    EntityManager entityManager();

    int numTotal();
    int numUsed();
    int numFree();

    Optional<Spot> nextFree();

    @Override
    default void close() throws IOException {
        entityManager().close();
    }
}
