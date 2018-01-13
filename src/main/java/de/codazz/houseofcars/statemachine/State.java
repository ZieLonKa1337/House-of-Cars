package de.codazz.houseofcars.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** @author rstumm2s */
public interface State<Data, Event> {
    /** @param data the object returned from
     *     the previous {@link #onExit()} */
    default void onEnter(final Data data) {}
    /** @return the object to pass into
     *     the next {@link #onEnter(Object)} */
    default Data onExit() { return null; }

    /** @return the next state for an outer transition
     *     or {@code null} for an inner transition
     * @throws RuntimeException if the {@code on(event)}
     *     action method could not be called */
    @SuppressWarnings("unchecked")
    default <S extends State<Data, Event>> S onEvent(final Event event) {
        Method action;
        try {
            action = getClass().getDeclaredMethod("on", event.getClass());
            if (!action.isAccessible()) {
                action.setAccessible(true);
            }
        } catch (final NoSuchMethodException noDeclaredAction) {
            try {
                action = getClass().getMethod("on", event.getClass());
            } catch (final NoSuchMethodException noAction) {
                if (getClass().getSuperclass().isEnum()) {
                    try {
                        action = getClass().getSuperclass().getDeclaredMethod("on", event.getClass());
                        action.setAccessible(true);
                    } catch (final NoSuchMethodException noEnumAction) {
                        throw new RuntimeException(noEnumAction);
                    }
                } else {
                    throw new RuntimeException(noAction);
                }
            }
        }

        try {
            return (S) action.invoke(this, event);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
