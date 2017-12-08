package de.codazz.houseofcars.statemachine;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** @author rstumm2s */
public abstract class AbstractStateMachineTest {
    /** Whether the machine started lazy.
     * Does not mean it will still be lazy. */
    protected final boolean lazy;

    private Class root;
    private StateMachine machine;

    protected AbstractStateMachineTest(final Class root, final boolean lazy) {
        this.root = root;
        this.lazy = lazy;
    }

    protected StateMachine machine() {
        return machine;
    }

    protected StateMachine instantiate(Class root) throws StateMachineException {
        return new StateMachine<>(root);
    }

    @Before
    public void setUp() throws StateMachineException {
        machine = instantiate(root);
    }
}
