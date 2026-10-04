package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a list of time ranges is null or empty, while at least one range was expected.
 */
public class InvalidTimeRangeList extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidTimeRangeList(String message) {
        super(message);
    }

    /**
     * Creates the exception for an empty list of ranges.
     *
     * @return the exception to throw
     */
    public static InvalidTimeRangeList create() {
        return new InvalidTimeRangeList("The given list is not a valid list of TimeRange instance containing at least one range.");
    }
}
