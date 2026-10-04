package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when an opening hours definition carries a {@code dateTimeClass} key.
 *
 * <p>The original library uses that key to pick the date time implementation it works with. The Java
 * implementation is built on {@code java.time} instead and cannot honour a custom class, so any
 * non-null value is refused rather than silently ignored.</p>
 */
public class InvalidDateTimeClass extends Exception {

    /**
     * @param message the detail message
     */
    public InvalidDateTimeClass(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given class name.
     *
     * @param string the offending value of the {@code dateTimeClass} key
     * @return the exception to throw
     */
    public static InvalidDateTimeClass forString(String string) {
        return new InvalidDateTimeClass("The string `" + string + "` isn't a valid class implementing DateTimeInterface.");
    }
}
