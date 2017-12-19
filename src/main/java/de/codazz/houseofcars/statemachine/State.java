package de.codazz.houseofcars.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** @author rstumm2s */
public interface State<Data, Event> {
    default void onEnter(Data data) {}
    default Data onExit() { return null; }

    @SuppressWarnings("unchecked")
    default <S extends State<Data, Event>> S onEvent(final Event event) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Method action;
        try {
            action = getClass().getDeclaredMethod("on", event.getClass());
            if (!action.isAccessible()) {
                action.setAccessible(true);
            }
        } catch (final NoSuchMethodException e) {
            action = getClass().getMethod("on", event.getClass());
        }

        return (S) action.invoke(this, event);
    }
}
