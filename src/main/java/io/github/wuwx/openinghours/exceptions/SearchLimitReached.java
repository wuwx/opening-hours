package io.github.wuwx.openinghours.exceptions;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Thrown when a next or previous search reaches the {@code searchUntil} limit without finding an
 * open or close time.
 */
public class SearchLimitReached extends Exception {

    /**
     * @param message the detail message
     */
    public SearchLimitReached(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given limit.
     *
     * @param dateTime the limit that was reached
     * @return the exception to throw
     */
    public static SearchLimitReached forDate(LocalDateTime dateTime) {
        return new SearchLimitReached("Search reached the limit: " + dateTime);
    }

    /**
     * Creates the exception for the given limit.
     *
     * @param dateTime the limit that was reached
     * @return the exception to throw
     */
    public static SearchLimitReached forDate(ZonedDateTime dateTime) {
        return new SearchLimitReached("Search reached the limit: " + dateTime);
    }
}
