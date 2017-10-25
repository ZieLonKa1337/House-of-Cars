package de.codazz.houseofcars;

import java.time.Instant;

/** @author rstumm2s  */
public abstract class AbstractPricing implements Pricing {
    @Override
    public int euroCents(final Instant from) {
        return euroCents(from, Instant.now());
    }
}
