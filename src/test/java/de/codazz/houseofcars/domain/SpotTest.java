package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.GarageMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class SpotTest {
    public static final int NUM_TOTAL = 100;
    public static final Map<Spot.Type, Integer> NUM_SPOTS; static {
        final Map<Spot.Type, Integer> spots = new HashMap<>();
        spots.put(Spot.Type.BIKE, NUM_TOTAL / 10);
        spots.put(Spot.Type.HANDICAP, NUM_TOTAL / 6);
        spots.put(Spot.Type.CAR, NUM_TOTAL - spots.get(Spot.Type.BIKE) - spots.get(Spot.Type.HANDICAP));
        NUM_SPOTS = Collections.unmodifiableMap(spots);
    }

    static GarageMock garage;

    @BeforeClass
    public static void setUpClass() throws FileNotFoundException {
        garage = new GarageMock();
    }

    @AfterClass
    public static void tearDownClass() {
        garage.close();
    }

    @Before
    public void setUp() {
        garage.reset(NUM_TOTAL, NUM_SPOTS);
    }

    @Test
    public void count() {
        assertEquals(NUM_TOTAL, Spot.count());
        for (final Spot.Type type : Spot.Type.values()) {
            assertEquals(NUM_SPOTS.get(type).longValue(), Spot.count(type));
        }
    }

    @Test
    public void countFree() throws NoSuchMethodException, InvocationTargetException {
        assertEquals(NUM_TOTAL, Spot.countFree());

        int total = 0;
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) - 1;

            assertEquals(NUM_TOTAL - total, Spot.countFree());
            assertEquals(NUM_SPOTS.get(type).longValue(), Spot.countFree(type));

            garage.park(type, Vehicle.State.Parking, num);
            total += num;
            assertEquals(1, Spot.countFree(type));

            garage.park(type, Vehicle.State.Parking,1);
            total += 1;
            assertEquals(0, Spot.countFree(type));
        }

        assertEquals(0, Spot.countFree());
    }

    @Test
    public void countUsed() throws NoSuchMethodException, InvocationTargetException {
        assertEquals(0, Spot.countUsed());

        int total = 0;
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) - 1;

            assertEquals(total, Spot.countUsed());
            assertEquals(0, Spot.countUsed(type));

            garage.park(type, Vehicle.State.Parking, num);
            total += num;
            assertEquals(num, Spot.countUsed(type));

            garage.park(type, Vehicle.State.Parking, 1);
            total += 1;
            assertEquals(num + 1, Spot.countUsed(type));
        }

        assertEquals(NUM_TOTAL, Spot.countUsed());
    }

    @Test
    public void anyFree() throws NoSuchMethodException, InvocationTargetException {
        for (final Spot.Type type : Spot.Type.values()) {
            assertTrue(Spot.anyFree(type).isPresent());

            garage.park(type, Vehicle.State.Parking, NUM_SPOTS.get(type) - 1);
            assertTrue(Spot.anyFree(type).isPresent());

            garage.park(type, Vehicle.State.Parking, 1);
            assertFalse(Spot.anyFree(type).isPresent());
        }
    }
}
