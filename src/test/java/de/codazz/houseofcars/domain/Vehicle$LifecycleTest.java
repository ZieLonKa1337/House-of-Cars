package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.GarageMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.beans.Transient;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.codazz.houseofcars.domain.SpotTest.NUM_SPOTS;
import static de.codazz.houseofcars.domain.SpotTest.NUM_TOTAL;
import static org.junit.Assert.*;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class Vehicle$LifecycleTest {
    @Parameterized.Parameters
    public static Iterable data() {
        final Vehicle.State[] states = Vehicle.State.values(), data = new Vehicle.State[states.length + 1];
        System.arraycopy(Vehicle.State.values(), 0, data, 0, states.length);
        return Arrays.asList(data);
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

            // set up parking vehicles to restore
            if (start == null) return v; // none

            final Vehicle.State.Data data = new Vehicle.State.Data();

            if (start.ordinal() >= Vehicle.State.LookingForSpot.ordinal()) {
                em.persist(new VehicleTransition(v, Vehicle.State.LookingForSpot, new Vehicle.State.Data()));
                Transition.tick(Duration.ofMinutes(1));
            }

            if (start.ordinal() >= Vehicle.State.Parking.ordinal()) {
                data.spot = Spot.anyFree().orElseThrow(AssertionError::new);
                em.persist(new VehicleTransition(v, Vehicle.State.Parking, data));
                Transition.tick(Duration.ofHours(2));
            }

            if (start.ordinal() >= Vehicle.State.Leaving.ordinal()) {
                data.spot = null;
                em.persist(new VehicleTransition(v, Vehicle.State.Leaving, data));
                Transition.tick(Duration.ofMinutes(2));
            }

            if (start == Vehicle.State.Away) {
                em.persist(new VehicleTransition(v, Vehicle.State.Away, data));
            }

            return v;
        });
    }

    @Test
    public void lifecycle() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (start == null || start == Vehicle.State.Away) {
            assertSame(Vehicle.State.Away, vehicle.state().state());
        }

        if (start == null || start.ordinal() < Vehicle.State.LookingForSpot.ordinal()) {
            vehicle.state().new EnteredEvent().fire();
            assertSame(Vehicle.State.LookingForSpot, vehicle.state().state());
        }

        if (start == null || start.ordinal() < Vehicle.State.Parking.ordinal()) {
            vehicle.state().new ParkedEvent(Spot.anyFree().orElseThrow(AssertionError::new)).fire();
            assertSame(Vehicle.State.Parking, vehicle.state().state());
        }

        if (start == null || start.ordinal() < Vehicle.State.Leaving.ordinal()) {
            vehicle.state().new LeftSpotEvent().fire();
            assertSame(Vehicle.State.Leaving, vehicle.state().state());
        }

        vehicle.state().new LeftEvent().fire();
        assertSame(Vehicle.State.Away, vehicle.state().state());
    }
}
