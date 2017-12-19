package de.codazz.houseofcars.business;

import de.codazz.houseofcars.GarageMock;
import de.codazz.houseofcars.domain.Vehicle;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import static de.codazz.houseofcars.domain.SpotTest.NUM_SPOTS;
import static de.codazz.houseofcars.domain.SpotTest.NUM_TOTAL;
import static org.junit.Assert.assertEquals;

/** @author rstumm2s */
public class GateTest {
    static GarageMock garage;

    Gate machine;

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
        machine = new Gate();
    }

    /** HOC-4 */
    @Test
    public void US4() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        assertEquals(Gate.State.Closed, machine.state());
        assertEquals(0, Vehicle.countPending());

        machine.new OpenedEvent().fire();
        assertEquals(Gate.State.Open, machine.state());
        assertEquals(0, Vehicle.count(Vehicle.State.LookingForSpot));

        machine.new EnteredEvent("US4").fire();
        assertEquals(Gate.State.Closed, machine.state());
        assertEquals(1, Vehicle.count(Vehicle.State.LookingForSpot));
    }
}
