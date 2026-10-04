package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a https://schema.org/OpeningHoursSpecification object is malformed: unknown day,
 * unsupported {@code PublicHolidays} day, invalid hours, or invalid valid-from/valid-through dates.
 */
public class InvalidOpeningHoursSpecification extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidOpeningHoursSpecification(String message) {
        super(message);
    }
}
