package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.GarageMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static de.codazz.houseofcars.domain.SpotTest.NUM_SPOTS;
import static de.codazz.houseofcars.domain.SpotTest.NUM_TOTAL;
import static org.junit.Assert.*;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class Vehicle$LifecycleTest {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(Vehicle.State.values());
    }

    static GarageMock garage;

    Vehicle vehicle;
    Vehicle.State start;

    public Vehicle$LifecycleTest(final Vehicle.State state) {
        start = state;
    }

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
        vehicle = garage.persistence.transact((em, __) -> {
            final Vehicle v = new Vehicle("LifecycleTest");
            em.persist(v);

            // set up past Parking to restore
            final Parking parking;
            switch (start) {
                case Parking:
                case Leaving:
                    parking = new Parking(v);
                    em.persist(parking);
                    parking.park(Spot.anyFree(Spot.Type.CAR).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                    break;
                default:
                    parking = null;
            }
            if (start == Vehicle.State.Leaving) {
                parking.free();
            }

            return v;
        });
        vehicle.state(start);
    }

    @Test
    public void lifecycle() throws NoSuchMethodException, InvocationTargetException {
        if (start == Vehicle.State.Away) {
            assertSame(Vehicle.State.Away, vehicle.state().state());
            assertEquals(Vehicle.State.Away.name(), vehicle.persistentState());
        }

        if (start.ordinal() < Vehicle.State.LookingForSpot.ordinal()) {
            vehicle.state().fire(vehicle.state().new EnteredEvent());
            assertSame(Vehicle.State.LookingForSpot, vehicle.state().state());
            assertEquals(Vehicle.State.LookingForSpot.name(), vehicle.persistentState());
        }

        if (start.ordinal() < Vehicle.State.Parking.ordinal()) {
            vehicle.state().fire(vehicle.state().new ParkedEvent(Spot.anyFree(Spot.Type.CAR)
                .orElseThrow(() -> new IllegalStateException("likely bug in test code"))));
            assertSame(Vehicle.State.Parking, vehicle.state().state());
            assertEquals(Vehicle.State.Parking.name(), vehicle.persistentState());
        }

        if (start.ordinal() < Vehicle.State.Leaving.ordinal()) {
            vehicle.state().fire(vehicle.state().new LeftSpotEvent());
            assertSame(Vehicle.State.Leaving, vehicle.state().state());
            assertEquals(Vehicle.State.Leaving.name(), vehicle.persistentState());
        }

        vehicle.state().fire(vehicle.state().new LeftEvent());
        assertSame(Vehicle.State.Away, vehicle.state().state());
        assertEquals(Vehicle.State.Away.name(), vehicle.persistentState());
    }
}
