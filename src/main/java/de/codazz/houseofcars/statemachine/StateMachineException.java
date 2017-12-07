package de.codazz.houseofcars.statemachine;

/** There is a problem with the configuration of your state machine.
 * <ul>
 *     Possible reasons:
 *     <li>Wrong annotations.</li>
 *     <li>Method takes wrong event type.</li>
 *     <li>Method returns wrong response type.</li>
 *     <li>Method has too restrictive access.</li>
 * </ul>
 * @author rstumm2s */
public class StateMachineException extends Exception {
    public StateMachineException() {}

    public StateMachineException(final String s) {
        super(s);
    }

    public StateMachineException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public StateMachineException(final Throwable throwable) {
        super(throwable);
    }

    public StateMachineException(final String s, final Throwable throwable, final boolean b, final boolean b1) {
        super(s, throwable, b, b1);
    }
}
