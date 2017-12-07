package de.codazz.houseofcars.statemachine;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** @author rstumm2s */
@Target(TYPE)
@Retention(RUNTIME)
public @interface State {
    /** defaults to the simple class name */
    String name() default "";
    String description() default "";

    /** Only one initial state is allowed in each {@link State}.
     * @return whether this must be the initial state */
    boolean root() default false;

    /** @return whether this is an accepted final state */
    boolean end() default false;

    /** A state can take an instance of this class, given to the state machine on
     * {@link StateMachine#StateMachine(Class, boolean, Object) construction},
     * into its one-arg constructor. It is an error to declare a remote
     * without implementing a constructor taking only an object of this class.
     * @return the required remote class */
    Class<?> remote() default void.class;
}
