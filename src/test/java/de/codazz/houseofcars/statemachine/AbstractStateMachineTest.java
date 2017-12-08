package de.codazz.houseofcars.statemachine;

import org.junit.Before;

/** @author rstumm2s */
public abstract class AbstractStateMachineTest<Event, Response, Remote> {
    /** Whether the machine started lazy.
     * Does not mean it will still be lazy. */
    protected final boolean lazy;

    private Class root;
    private StateMachine<Event, Response, Remote> machine;
    private Remote remote;

    protected AbstractStateMachineTest(final Class root, final boolean lazy, final Remote remote) {
        this.root = root;
        this.lazy = lazy;
        this.remote = remote;
    }

    protected StateMachine<Event, Response, Remote> machine() {
        return machine;
    }

    protected Remote remote() {
        return remote;
    }

    protected StateMachine<Event, Response, Remote> instantiate(Class root) throws StateMachineException {
        return new StateMachine<>(root, lazy);
    }

    @Before
    public void setUp() throws StateMachineException {
        machine = instantiate(root);
    }
}