package de.codazz.houseofcars.statemachine;

/** @author rstumm2s */
public abstract class EnumStateMachine<State extends Enum<State> & de.codazz.houseofcars.statemachine.State<Data, Event>, Data, Event> extends StateMachine<State, Data, Event> {
    public EnumStateMachine(final State state, final Data data) {
        super(state, data);
    }

    public EnumStateMachine(final Transition<Event, State, Data> init) {
        super(init);
    }
}
