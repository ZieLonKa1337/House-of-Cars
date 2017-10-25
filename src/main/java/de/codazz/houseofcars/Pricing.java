package de.codazz.houseofcars;

import java.time.Instant;

/** @author rstumm2s */
public interface Pricing {
    int euroCents(final Instant from);
    int euroCents(final Instant from, final Instant to);
}
