package de.codazz.houseofcars.statemachine;

/** There is a problem with the configuration of your state machine.
 * <ul>
 *     Possible reasons:
 *     <li>Wrong annotations.</li>
 *     <li>Two root states in one state machine.</li>
 *     <li>Method takes wrong event type.</li>
 *     <li>Method returns wrong response type.</li>
 *     <li>Method has too restrictive access.</li>
 * </ul>
 * @author rstumm2s */
public class StateMachineException extends Exception {
    public StateMachineException() {}

    public StateMachineException(final String message) {
        super(message);
    }

    public StateMachineException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public StateMachineException(final Throwable cause) {
        super(cause);
    }

    public StateMachineException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
