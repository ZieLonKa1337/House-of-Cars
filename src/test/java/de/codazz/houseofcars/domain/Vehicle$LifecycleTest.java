package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.GarageMock;
import de.codazz.houseofcars.statemachine.AbstractStateMachineTest;
import de.codazz.houseofcars.statemachine.StateMachine;
import de.codazz.houseofcars.statemachine.StateMachineException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileNotFoundException;
import java.util.Arrays;

import static de.codazz.houseofcars.domain.SpotTest.NUM_SPOTS;
import static de.codazz.houseofcars.domain.SpotTest.NUM_TOTAL;
import static org.junit.Assert.*;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class Vehicle$LifecycleTest extends AbstractStateMachineTest<Vehicle.Lifecycle.Event, Void, Void> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
            {Vehicle.Lifecycle.Away.class, false}, {Vehicle.Lifecycle.Away.class, true},
            {Vehicle.Lifecycle.LookingForSpot.class, false}, {Vehicle.Lifecycle.LookingForSpot.class, true},
            {Vehicle.Lifecycle.Parking.class, false}, {Vehicle.Lifecycle.Parking.class, true},
            {Vehicle.Lifecycle.Leaving.class, false}, {Vehicle.Lifecycle.Leaving.class, true}
        });
    }

    static GarageMock garage;

    static Vehicle vehicle;

    public Vehicle$LifecycleTest(final Class root, final boolean lazy) {
        super(root, lazy);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected StateMachine<Vehicle.Lifecycle.Event, Void, Void> instantiate(final Class root) throws StateMachineException {
        if (comesAfter(root, Vehicle.Lifecycle.LookingForSpot.class)) {
            // set up past Parkings to restore

            final Parking parking = garage.persistence.transact((em, __) -> {
                final Parking p = new Parking(vehicle);
                em.persist(p);
                p.park(Spot.anyFree(Spot.Type.CAR).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                return p;
            });

            if (root.equals(Vehicle.Lifecycle.Leaving.class)) {
                garage.persistence.<Void>transact((em, __) -> {
                    parking.free();
                    return null;
                });
            }
        }

        return vehicle.state(root);
    }

    @BeforeClass
    public static void setUpClass() throws FileNotFoundException {
        garage = new GarageMock();
        garage.reset(NUM_TOTAL, NUM_SPOTS);

        vehicle = garage.persistence.transact((em, __) -> {
            final Vehicle v = new Vehicle("LifecycleTest");
            em.persist(v);
            return v;
        });
    }

    @AfterClass
    public static void tearDownClass() {
        garage.close();
    }

    @Test
    public void lifecycle() throws StateMachineException {
        if (root.equals(Vehicle.Lifecycle.Away.class)) {
            assertEquals(Vehicle.Lifecycle.Away.class, machine().state().getClass());
            assertEquals(Vehicle.Lifecycle.Away.class.getSimpleName(), vehicle.persistentState());
            assertTrue(machine().valid());
        }

        if (notYetVisited(Vehicle.Lifecycle.LookingForSpot.class)) {
            assertFalse(machine().onEvent(((Vehicle.Lifecycle.Away) machine().state()).new EnteredEvent()).isPresent());
            assertEquals(Vehicle.Lifecycle.LookingForSpot.class, machine().state().getClass());
            assertEquals(Vehicle.Lifecycle.LookingForSpot.class.getSimpleName(), vehicle.persistentState());
            assertFalse(machine().valid());
        }

        if (notYetVisited(Vehicle.Lifecycle.Parking.class)) {
            assertFalse(machine().onEvent(((Vehicle.Lifecycle.LookingForSpot) machine().state()).new ParkedEvent(Spot.anyFree(Spot.Type.CAR)
                .orElseThrow(() -> new IllegalStateException("likely bug in test code")))).isPresent());
            assertEquals(Vehicle.Lifecycle.Parking.class, machine().state().getClass());
            assertEquals(Vehicle.Lifecycle.Parking.class.getSimpleName(), vehicle.persistentState());
            assertTrue(machine().valid());
        }

        if (notYetVisited(Vehicle.Lifecycle.Leaving.class)) {
            assertFalse(machine().onEvent(((Vehicle.Lifecycle.Parking) machine().state()).new LeftSpotEvent()).isPresent());
            assertEquals(Vehicle.Lifecycle.Leaving.class, machine().state().getClass());
            assertEquals(Vehicle.Lifecycle.Leaving.class.getSimpleName(), vehicle.persistentState());
            assertFalse(machine().valid());
        }

        assertFalse(machine().onEvent(((Vehicle.Lifecycle.Leaving) machine().state()).new LeftEvent()).isPresent());
        assertEquals(Vehicle.Lifecycle.Away.class, machine().state().getClass());
        assertEquals(Vehicle.Lifecycle.Away.class.getSimpleName(), vehicle.persistentState());
        assertTrue(machine().valid());
    }

    @SuppressWarnings("unchecked")
    private boolean notYetVisited(final Class<? extends Vehicle.Lifecycle.PersistentState> current) {
        return comesAfter(current, root);
    }

    public static boolean comesAfter(final Class<? extends Vehicle.Lifecycle.PersistentState> a, final Class<? extends Vehicle.Lifecycle.PersistentState> b) {
        return ordinal(a) > ordinal(b);
    }

    public static int ordinal(final Class<? extends Vehicle.Lifecycle.PersistentState> state) {
        if (state == null) return 4; // always greater
        if (state.equals(Vehicle.Lifecycle.Away.class)) return 0;
        if (state.equals(Vehicle.Lifecycle.LookingForSpot.class)) return 1;
        if (state.equals(Vehicle.Lifecycle.Parking.class)) return 2;
        if (state.equals(Vehicle.Lifecycle.Leaving.class)) return 3;
        throw new IllegalStateException("likely bug in test code");
    }
}
