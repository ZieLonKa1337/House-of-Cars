package de.codazz.houseofcars.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

/** @param <E> event
 * @param <R> remote
 * @author rstumm2s */
public class StateMachine<S extends State<E, R>, E, R> {
    private R remote;
    private S state;

    public StateMachine(final S state, final R remote) {
        this.state = Objects.requireNonNull(state);
        this.remote = remote;
    }

    public void fire(final E event) throws NoSuchMethodException, InvocationTargetException {
        final S next = state.onEvent(event);
        state.onExit();
        state = next;
        state.onEnter(remote);
    }

    public S state() {
        return state;
    }

    public R remote() {
        return remote;
    }

    public class CheckedEvent {
        protected CheckedEvent(final S state) {
            if (!state.equals(state())) throw new IllegalStateException(state().toString());
        }

        @SafeVarargs
        protected CheckedEvent(final S... states) {
            this(Arrays.asList(states));
        }

        protected CheckedEvent(final Iterable<S> states) {
            boolean allowed = false;
            for (final S state : states) {
                if (state.equals(state())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) throw new IllegalStateException(state().toString());
        }
    }
}
