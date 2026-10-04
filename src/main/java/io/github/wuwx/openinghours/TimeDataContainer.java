package io.github.wuwx.openinghours;

/**
 * Contract shared by the time value objects, the counterpart of the original library's
 * {@code TimeDataContainer} interface.
 *
 * <p>It carries {@link #TIME_FORMAT} and {@link #MIDNIGHT}, so both
 * {@code TimeDataContainer.TIME_FORMAT} and {@code Time.TIME_FORMAT} resolve, exactly as they do in
 * the original library where {@code Time} inherits those constants from the interface.</p>
 */
public interface TimeDataContainer {

    /** The format used when a time is rendered as a string. */
    String TIME_FORMAT = "HH:mm";

    /** {@code 00:00}, the start of a day. */
    Time MIDNIGHT = Time.fromString("00:00");

    /**
     * Parses a time from a string.
     *
     * @param string the string to parse
     * @return the parsed time
     * @throws io.github.wuwx.openinghours.exceptions.InvalidTimeString if the string is not a valid time
     */
    static Time fromString(String string) {
        return Time.fromString(string);
    }

    /** @return the time as a string, in {@link #TIME_FORMAT} */
    String toString();
}
