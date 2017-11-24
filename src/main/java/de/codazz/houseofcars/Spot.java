package de.codazz.houseofcars;

/** @author rstumm2s */
public interface Spot {
    enum Type {
        CAR,
        BIKE,
        HANDICAP
    }

    /** @return garage-local ID */
    int id();

    Type type();
}
