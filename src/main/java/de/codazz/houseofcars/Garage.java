package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import javax.persistence.EntityManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/** @author rstumm2s */
public interface Garage extends Runnable, Closeable {
    EntityManager entityManager();

    /** @return the total number of spots */
    int numTotal();
    int numTotal(Spot.Type type);

    /** @return the number of occupied spots */
    int numUsed();
    int numUsed(Spot.Type type);

    /** @return the number of unoccupied spots */
    default int numFree() {
        return numTotal() - numUsed();
    }
    default int numFree(final Spot.Type type) {
        return numTotal(type) - numUsed(type);
    }

    /** @return the number of vehicles not on a spot */
    default int numPending() {
        return numVehicles() - numUsed();
    }

    /** @return the number of vehicles looking for a spot */
    int numParking();

    /** @return the number of vehicles leaving the garage */
    default int numLeaving() {
        return numPending() - numParking();
    }

    /** @return the number of vehicles in the garage */
    int numVehicles();

    Optional<Spot> nextFree(Spot.Type type);

    @Override
    default void close() throws IOException {
        entityManager().close();
    }
}
