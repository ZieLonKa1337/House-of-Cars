package de.codazz.houseofcars.statemachine;

/** @param <E> the event type
 * @param <R> the event response type
 * @author rstumm2s */
@FunctionalInterface
public interface EventHandler<E, R> {
    R onEvent(E event) throws StateMachineException;
}
