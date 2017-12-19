package de.codazz.houseofcars.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/** @author rstumm2s */
public class StateMachine<State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data, Event> {
    private Transition<Event, State, Data> transition;

    public StateMachine(final State state, final Data data) {
        state.onEnter(data);
        transition = transition(state, data);
    }

    public StateMachine(final Transition<Event, State, Data> init) {
        init.state().onEnter(init.data());
        transition = init;
    }

    public void fire(final Event event) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final State state = state(), next = state.onEvent(event); // inner transition
        if (next != null) { // outer transition
            final Data data = state.onExit();
            next.onEnter(data);
            transition = transition(next, data);
        }
    }

    protected Transition<Event, State, Data> transition(final State state, final Data data) {
        return new Transition<Event, State, Data>() {
            @Override
            public State state() {
                return state;
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
