package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a time range string is not formatted as {@code HH:mm-HH:mm}.
 */
public class InvalidTimeRangeString extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidTimeRangeString(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given invalid range string.
     *
     * @param string the offending range string
     * @return the exception to throw
     */
    public static InvalidTimeRangeString forString(String string) {
        return new InvalidTimeRangeString("The string `" + string + "` isn't a valid time range string. A time string must be a formatted as `H:i-H:i`, e.g. `09:00-18:00`.");
    }
}
