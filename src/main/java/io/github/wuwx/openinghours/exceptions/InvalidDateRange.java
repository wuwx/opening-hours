package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a date range in an opening hours definition would override a day or another date that
 * is already defined.
 */
public class InvalidDateRange extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidDateRange(String message) {
        super(message);
    }

    /**
     * Creates the exception for a definition overriding an existing one.
     *
     * @param entry the offending definition key
     * @param date  the date it would override
     * @return the exception to throw
     */
    public static InvalidDateRange invalidDateRange(String entry, String date) {
        return new InvalidDateRange("Unable to record `" + entry + "` as it would override `" + date + "`.");
    }
}
