package de.codazz.houseofcars;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author rstumm2s */
public class GarageTest {
    Garage emptyGarage;

    @Before
    public void setUp() {
        emptyGarage = new Garage() {
            @Override
            public int numTotal() {
                return 100;
            }

            @Override
            public int numUsed() {
                return 0;
            }

            @Override
            public int numFree() {
                return 100;
            }

            @Override
            public Optional<Spot> nextFree() {
                return Optional.of(new Spot() {});
            }
        };
    }

    @Test
    public void numFree_empy_total() {
        assertEquals(emptyGarage.numFree(), emptyGarage.numTotal());
    }

    @Test
    public void numUsed_empty_0() {
        assertEquals(emptyGarage.numUsed(), 0);
    }

    @Test
    public void nextFree_empty_spot() {
        assertTrue(emptyGarage.nextFree().isPresent());
    }
}
