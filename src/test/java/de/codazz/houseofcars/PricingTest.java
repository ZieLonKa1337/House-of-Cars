package de.codazz.houseofcars;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

/** @author rstumm2s */
public class PricingTest {
    Pricing pricing;

    @Before
    public void setUp() throws Exception {
        pricing = new AbstractPricing() {
            @Override
            public int euroCents(final Instant from, final Instant to) {
                return (int) Duration.between(from, to).toHours() * 100;
            }
        };
    }

    @Test
    public void euroCents_1h_1eur() {
        final Instant now = Instant.now(),
                h1 = now.minusSeconds(3600);
        assertEquals(100, pricing.euroCents(h1, now));
    }
}
