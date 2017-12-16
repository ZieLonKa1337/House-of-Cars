package de.codazz.houseofcars.statemachine;

/** @author rstumm2s */
public class EnumStateMachine<S extends Enum<S> & State<E, R>, E, R> extends StateMachine<S, E, R> {
    public EnumStateMachine(final S state, final R remote) {
        super(state, remote);
    }

    @SuppressWarnings("unchecked")
    public class CheckedEvent extends StateMachine.CheckedEvent {
        protected CheckedEvent(final S state) {
            super(state);
        }

        protected CheckedEvent(final S[] states) {
            super(states);
        }

        protected CheckedEvent(final Iterable<S> states) {
            super(states);
        }
    }
}
