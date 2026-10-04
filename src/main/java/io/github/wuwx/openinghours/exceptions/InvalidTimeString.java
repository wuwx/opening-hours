package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a time string is not formatted as {@code HH:mm} (or as {@code 24:00}).
 */
public class InvalidTimeString extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidTimeString(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given invalid time string.
     *
     * @param string the offending time string
     * @return the exception to throw
     */
    public static InvalidTimeString forString(String string) {
        return new InvalidTimeString("The string `" + string + "` isn't a valid time string. A time string must be a formatted as `H:i`, e.g. `06:00`, `18:00`.");
    }
}
