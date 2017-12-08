package de.codazz.houseofcars.statemachine;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <p>
 * On classes, transition to the next state.<br/>
 * On methods, call the method and transition to the next state.
 * </p><p>
 * Annotated methods must have the following signature:<br/>
 * {@code public Object name(Object);}
 * </p><p>
 * The return value is returned from {@link StateMachine#onEvent(Object)}.
 * </p>
 * @author rstumm2s */
@Inherited
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface OnEvent {
    /** event type */
    Class<?> value();

    /** The next state to transition to. Must be annotated with {@link State}!
     * <p>None for an inner transition, this state for an outer transition.</p> */
    Class<?> next() default void.class;
}
