package de.codazz.houseofcars;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class PricingTest {
    Pricing pricing;

    @Before
    public void setUp() throws Exception {}

    @Test
    public void euroCents_1h_1eur() {
        final Instant now = Instant.now(),
                h1 = now.plusSeconds(3600);
        assertEquals(100, pricing.euroCents(h1));
    }
}
