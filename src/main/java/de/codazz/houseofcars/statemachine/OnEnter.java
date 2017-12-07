package de.codazz.houseofcars.statemachine;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Annotated methods must have one of the following signatures:
 * <ul>
 *     <li>{@code public void name();}</li>
 *     <li>{@code public void name(Object);}</li>
 * </ul>
 * The argument is passed in from the previous {@link OnExit} method, if any.
 * <p>Only one {@link OnEnter} method is allowed per {@link State}.</p>
 * @author rstumm2s */
@Target(METHOD)
@Retention(RUNTIME)
public @interface OnEnter {}
