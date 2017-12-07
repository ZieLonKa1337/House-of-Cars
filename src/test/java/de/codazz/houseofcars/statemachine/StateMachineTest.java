package de.codazz.houseofcars.statemachine;

import org.junit.Before;

import static org.junit.Assert.*;

/** @author rstumm2s */
public abstract class StateMachineTest<T extends StateMachine> {
    /** Whether the machine started lazy.
     * Does not mean it will still be lazy. */
    protected final boolean lazy;

    private T machine;

    protected StateMachineTest(final T machine, final boolean lazy) {
        this.machine = machine;
        this.lazy = lazy;
    }

    protected T machine() {
        return machine;
    }

    @Before
    public void start() throws StateMachineException {
        if (lazy) {
            assertTrue(machine.lazy());
            assertNull(machine.state());
            assertNull(machine.metaState());
            assertFalse(machine.valid());
        } else {
            assertFalse(machine.lazy());
            assertNotNull(machine.state());
            assertNotNull(machine.metaState());
        }
        assertSame(machine.state(), machine.root());
        machine.start();
        assertSame(machine.state(), machine.root());
        if (lazy) {
            assertTrue(machine.lazy());
            assertNotNull(machine.state());
            assertNotNull(machine.metaState());
        }
    }
}
