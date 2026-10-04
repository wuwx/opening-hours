package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when an exception date in an opening hours definition is not a valid {@code yyyy-MM-dd} or
 * {@code MM-dd} date.
 */
public class InvalidDate extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidDate(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given invalid date.
     *
     * @param date the offending date
     * @return the exception to throw
     */
    public static InvalidDate invalidDate(String date) {
        return new InvalidDate("Date `" + date + "` isn't a valid date. Dates should be formatted as Y-m-d, e.g. `2016-12-25`.");
    }
}
