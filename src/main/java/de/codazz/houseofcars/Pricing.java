package de.codazz.houseofcars;

import java.time.Instant;

/** @author rstumm2s */
public interface Pricing {
    default int euroCents(final Instant from) {
        return euroCents(from, Instant.now());
    }

    int euroCents(final Instant from, final Instant to);
}
