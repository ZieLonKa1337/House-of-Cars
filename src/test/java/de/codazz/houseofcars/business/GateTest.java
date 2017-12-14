package de.codazz.houseofcars.business;

import de.codazz.houseofcars.GarageMock;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.AbstractStateMachineTest;
import de.codazz.houseofcars.statemachine.StateMachineException;
import org.junit.AfterClass;
import org.junit.Before;
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
public final class GateTest extends AbstractStateMachineTest<Gate.Event, Void, Void> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
            {false}, {true}
        });
    }

    static GarageMock garage;

    public GateTest(final boolean lazy) {
        super(Gate.class, lazy);
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
    @Override
    public void setUp() throws StateMachineException {
        garage.reset(NUM_TOTAL, NUM_SPOTS);
        super.setUp();
        machine().start();
    }

    /** HOC-4 */
    @Test
    public void US4() throws StateMachineException {
        assertEquals(Gate.class, machine().state().getClass());
        assertEquals(0, Vehicle.countPending());

        assertFalse(machine().onEvent(((Gate) machine().state()).new OpenedEvent()).isPresent());
        assertEquals(Gate.Open.class, machine().state().getClass());
        assertEquals(0, Vehicle.count(Vehicle.Lifecycle.LookingForSpot.class));

        assertFalse(machine().onEvent(((Gate.Open) machine().state()).new EnteredEvent("US4")).isPresent());
        assertEquals(Gate.class, machine().state().getClass());
        assertEquals(1, Vehicle.count(Vehicle.Lifecycle.LookingForSpot.class));
    }
}
