package de.codazz.houseofcars.statemachine;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Annotated methods must have one of the following signatures:
 * <ul>
 *     <li>{@code public void name();}</li>
 *     <li>{@code public Object name();}</li>
 * </ul>
 * <p>The return value is passed to the next state's {@link OnEnter} method.</p>
 * <p>A {@code void} return type is only a shortcut for returning {@code null}.
 * {@code null} will never be passed to the next {@link OnEnter} method.<br/>
 * If you need a {@code null} equivalent in your alphabet please declare one yourself.</p>
 * Only one {@link OnExit} method is allowed per {@link State}.
 * @author rstumm2s */
@Target(METHOD)
@Retention(RUNTIME)
public @interface OnExit {}
