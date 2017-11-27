package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import javax.persistence.EntityManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/** @author rstumm2s */
public interface Garage extends Runnable, Closeable {
    EntityManager entityManager();

    int numTotal();
    int numUsed();

    default int numFree() {
        return numTotal() - numUsed();
    }

    Optional<Spot> nextFree();

    @Override
    default void close() throws IOException {
        entityManager().close();
    }
}
