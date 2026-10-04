package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a key of an opening hours definition is not a valid english day name.
 */
public class InvalidDayName extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidDayName(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given invalid day name.
     *
     * @param name the offending day name
     * @return the exception to throw
     */
    public static InvalidDayName invalidDayName(String name) {
        return new InvalidDayName("Day `" + name + "` isn't a valid day name. Valid day names are lowercase english words, e.g. `monday`, `thursday`.");
    }
}
