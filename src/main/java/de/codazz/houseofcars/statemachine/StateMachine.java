package de.codazz.houseofcars.statemachine;

import java.util.Arrays;

/** @author rstumm2s */
public abstract class StateMachine<State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data, Event> {
    /** the last successfully finished transition */
    private Transition<Event, State, Data> transition;

    /** The last data from {@link State#onExit()}, kept for inner transitions.
     * This will be passed to {@link #transition(State, Object) transition()}
     * when an inner transition takes place.
     * Subclasses must update this when necessary! */
    protected Data data;

    public StateMachine(final State state, final Data data) {
        state.onEnter(data);
        transition = transition(state, data);
        this.data = data;
    }

    public StateMachine(final Transition<Event, State, Data> init) {
        init.state().onEnter(init.data());
        transition = init;
        data = init.data();
    }

    public void fire(final Event event) {
        final State state = state(), next = state.onEvent(event); // inner transition
        if (next != null) { // outer transition
            data = state.onExit();
            next.onEnter(data);
        }
        transition = transition(next, data);
    }

    /** @param state {@code null} in case of an inner transition */
    protected Transition<Event, State, Data> transition(final State state, final Data data) {
        return new Transition<Event, State, Data>() {
            final State s = state != null ? state : state();
            @Override
            public State state() {
                return s;
            }
            @Override
            public Data data() {
                return data;
            }
        };
    }

    public State state() {
        return transition.state();
    }

    public class CheckedEvent {
        protected CheckedEvent(final State state) {
            if (!state.equals(state())) throw new IllegalStateException(state().toString());
        }

        @SafeVarargs
        protected CheckedEvent(final State... states) {
            this(Arrays.asList(states));
        }

        protected CheckedEvent(final Iterable<State> states) {
            boolean allowed = false;
            for (final State state : states) {
                if (state.equals(state())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) throw new IllegalStateException(state().toString());
        }
    }
}
