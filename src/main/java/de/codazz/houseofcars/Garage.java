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
    int numTotal(Spot.Type type);

    int numUsed();
    int numUsed(Spot.Type type);

    default int numFree() {
        return numTotal() - numUsed();
    }
    default int numFree(final Spot.Type type) {
        return numTotal(type) - numUsed(type);
    }

    Optional<Spot> nextFree(final Spot.Type type);

    @Override
    default void close() throws IOException {
        entityManager().close();
    }
}
