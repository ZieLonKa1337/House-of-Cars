package de.codazz.houseofcars;

import java.util.Optional;

/** @author rstumm2s */
public interface Garage {
    int numTotal();
    int numUsed();
    int numFree();

    Optional<Spot> nextFree();
}
