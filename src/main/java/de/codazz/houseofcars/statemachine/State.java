package de.codazz.houseofcars.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** @author rstumm2s */
public interface State<Event, Remote> {
    default void onEnter(Remote remote) {}
    default void onExit() {}

    @SuppressWarnings("unchecked")
    default <Next extends State<Event, Remote>> Next onEvent(final Event event) throws NoSuchMethodException, InvocationTargetException {
        Method action;
        try {
            action = getClass().getDeclaredMethod("on", event.getClass());
            if (!action.isAccessible()) {
                action.setAccessible(true);
            }
        } catch (final NoSuchMethodException e) {
            action = getClass().getMethod("on", event.getClass());
        }

        try {
            return (Next) action.invoke(this, event);
        } catch (final IllegalAccessException e) {
            throw new AssertionError("should never happen!", e);
            // really? what about public methods on private inner classes
        }
    }
}
