package de.codazz.houseofcars.statemachine;

/** @author rstumm2s */
public interface Transition<Event, State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data> {
    State state();
    Data data();
}
