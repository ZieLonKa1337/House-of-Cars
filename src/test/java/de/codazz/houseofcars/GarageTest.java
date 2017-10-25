package de.codazz.houseofcars;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class GarageTest {
    Garage garage;

    @Before
    public void setUp() {}

    @Test
    public void numFree_empy_total() {
        assertEquals(garage.numFree(), garage.numTotal());
    }

    @Test
    public void numUsed_empty_0() {
        assertEquals(garage.numUsed(), 0);
    }

    @Test
    public void nextFree_empty_spot() {
        assertTrue(garage.nextFree().isPresent());
    }
}
