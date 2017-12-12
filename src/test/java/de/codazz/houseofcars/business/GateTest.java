package de.codazz.houseofcars.business;

import de.codazz.houseofcars.GarageImplMock;
import de.codazz.houseofcars.GarageImplTest;
import de.codazz.houseofcars.statemachine.AbstractStateMachineTest;
import de.codazz.houseofcars.statemachine.StateMachineException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileNotFoundException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public final class GateTest extends AbstractStateMachineTest<String, Boolean, Void> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
            {false}, {true}
        });
    }

    static GarageImplMock garage;

    public GateTest(final boolean lazy) {
        super(Gate.class, lazy);
    }

    @BeforeClass
    public static void setUpClass() throws FileNotFoundException {
        garage = new GarageImplMock();
    }

    @AfterClass
    public static void tearDownClass() {
        garage.close();
    }

    @Override
    public void setUp() throws StateMachineException {
        garage.reset(GarageImplTest.NUM_TOTAL, GarageImplTest.NUM_SPOTS);
        super.setUp();
        machine().start();
    }

    /** HOC-4 */
    @Test
    public void US4() throws StateMachineException {
        assertTrue(machine().onEvent("ABXY0000").get());
        assertEquals(Gate.Open.class, machine().state().getClass());
        assertEquals(0, garage.numParking());

        machine().onEvent("ABXY0000");
        assertEquals(Gate.class, machine().state().getClass());
        assertEquals(1, garage.numParking());
    }
}
