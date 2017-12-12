package de.codazz.houseofcars.statemachine;

/** <p>
 * A state machine that is its own root state.
 * Extend this and propagate the {@link StateMachineException}
 * through the no-arg and/or one-arg (remote) constructors.
 * </p><p>
 * You can omit the {@link State} annotation on the subclass,
 * but if you supply it {@code root = true} will not be inherited.
 * </p>
 * @author rstumm2s
 * @see StateMachine#StateMachine(Class, boolean, Object, int)
 * @author rstumm2s */
@State(root = true)
public abstract class RootStateMachine<Event, Response, Remote> extends StateMachine<Event, Response, Remote> {
    /** root {@link State} constructor
     * @throws StateMachineException never */
    public RootStateMachine() throws StateMachineException {
        this(Void.class, true, null, -1);
    }

    /** root {@link State} constructor
     * @throws StateMachineException never */
    public RootStateMachine(final Remote remote) throws StateMachineException {
        this(Void.class, true, remote, -1);
    }

    /** {@link StateMachine} constructor */
    protected RootStateMachine(final Class<?> root) throws StateMachineException {
        super(root);
    }

    /** {@link StateMachine} constructor */
    protected RootStateMachine(final Class<?> root, final boolean lazy) throws StateMachineException {
        super(root, lazy);
    }

    /** {@link StateMachine} constructor */
    public RootStateMachine(final Class<?> root, final Remote remote) throws StateMachineException {
        super(root, remote);
    }

    /** {@link StateMachine} constructor */
    public RootStateMachine(final Class<?> root, final boolean lazy, final Remote remote) throws StateMachineException {
        super(root, lazy, remote);
    }

    /** {@link StateMachine} constructor */
    protected RootStateMachine(final Class<?> root, final boolean lazy, final Remote remote, final int statesCapacity) throws StateMachineException {
        super(root, lazy, remote, statesCapacity);
    }
}
