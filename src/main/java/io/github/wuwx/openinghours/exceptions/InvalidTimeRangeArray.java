package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a map used as a time range definition does not hold an {@code hours} property, or any
 * usable range value.
 */
public class InvalidTimeRangeArray extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidTimeRangeArray(String message) {
        super(message);
    }

    /**
     * Creates the exception for a map without a usable range definition.
     *
     * @return the exception to throw
     */
    public static InvalidTimeRangeArray create() {
        return new InvalidTimeRangeArray("TimeRange array definition must at least contains an \"hours\" property.");
    }
}
