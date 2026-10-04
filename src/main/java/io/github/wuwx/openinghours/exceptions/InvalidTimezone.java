package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a timezone given to the {@code OpeningHours} constructor cannot be resolved.
 */
public class InvalidTimezone extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidTimezone(String message) {
        super(message);
    }

    /**
     * Creates the exception for an unresolvable timezone.
     *
     * @return the exception to throw
     */
    public static InvalidTimezone create() {
        return new InvalidTimezone("Invalid Timezone");
    }
}
