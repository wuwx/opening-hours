package io.github.wuwx.openinghours.exceptions;

/**
 * Thrown when a caller tries to modify a read-only collection, such as the ranges returned by
 * {@code OpeningHoursForDay#getTimeRanges()}.
 */
public class NonMutableOffsets extends Exception {

    /**
     * @param message the detail message
     */
    public NonMutableOffsets(String message) {
        super(message);
    }

    /**
     * Creates the exception for the given class.
     *
     * @param className the class whose collection is read-only
     * @return the exception to throw
     */
    public static NonMutableOffsets forClass(String className) {
        return new NonMutableOffsets("Offsets of `" + className + "` objects are not mutable.");
    }
}
