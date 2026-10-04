package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidTimeString;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * A time of day, with minute resolution, and support for the special value {@code 24:00}.
 *
 * <p>Unlike {@link LocalTime}, a {@code Time} can represent the end of a day as {@code 24:00},
 * which means midnight <em>included</em>. That is what makes a range like {@code 09:00-24:00}
 * cover the whole evening, while {@code 00:00-24:00} covers a whole day.</p>
 *
 * <p>Instances are immutable. Comparisons ignore seconds and nanoseconds unless two times share
 * the same hour and minute.</p>
 */
public final class Time implements Comparable<Time>, TimeDataContainer {

    private static final Pattern FORMAT = Pattern.compile("^(([0-1][0-9]|2[0-3]):[0-5][0-9]|24:00)$");

    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int nanos;

    private Time(int hours, int minutes, int seconds, int nanos) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.nanos = nanos;
    }

    /**
     * Parses a {@code HH:mm} string, or the special value {@code 24:00}.
     *
     * @param timeString the string to parse
     * @return the parsed time
     * @throws InvalidTimeString if the string is null or not a valid time
     */
    public static Time fromString(String timeString) {
        if (timeString == null || !FORMAT.matcher(timeString).matches()) {
            throw InvalidTimeString.forString(String.valueOf(timeString));
        }
        int separator = timeString.indexOf(':');
        return new Time(
                Integer.parseInt(timeString.substring(0, separator)),
                Integer.parseInt(timeString.substring(separator + 1)),
                0, 0);
    }

    /**
     * Creates a {@code Time} from a {@link LocalTime}, keeping the seconds and nanoseconds.
     *
     * @param time the local time to copy
     * @return the equivalent {@code Time}
     */
    public static Time fromLocalTime(LocalTime time) {
        return new Time(time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
    }

    /**
     * Creates a {@code Time} from the time part of a {@link LocalDateTime}.
     *
     * @param dateTime the date time to read the time from
     * @return the equivalent {@code Time}
     */
    public static Time fromDateTime(LocalDateTime dateTime) {
        return fromLocalTime(dateTime.toLocalTime());
    }

    /** @return the hour part, from {@code 0} to {@code 24} */
    public int hours() {
        return hours;
    }

    /** @return the minute part, from {@code 0} to {@code 59} */
    public int minutes() {
        return minutes;
    }

    /** @return the second part; always {@code 0} for times parsed from a string */
    public int seconds() {
        return seconds;
    }

    /**
     * Applies this time to the given date, as the original library's {@code Time::toDateTime()}
     * does.
     *
     * <p>The special value {@code 24:00} is midnight of the <em>next</em> day, so
     * {@code Time.fromString("24:00").toDateTime(LocalDate.of(2020, 1, 1))} is {@code 2020-01-02T00:00}.
     * This keeps {@code 24:00} distinct from {@code 00:00}; use {@link #hours()} and
     * {@link #minutes()} when you need the wall clock value instead.</p>
     *
     * @param date the date the time belongs to
     * @return the resulting date time
     */
    public LocalDateTime toDateTime(LocalDate date) {
        return date.atStartOfDay()
                .plusMinutes(toMinutes())
                .plusSeconds(seconds)
                .plusNanos(nanos);
    }

    int toMinutes() {
        return hours * 60 + minutes;
    }

    int hhmm() {
        return hours * 100 + minutes;
    }

    /**
     * @param other the time to compare with
     * @return true if both times are equal
     */
    public boolean isSame(Time other) {
        return compareTo(other) == 0;
    }

    /**
     * @param other the time to compare with
     * @return true if this time is strictly earlier than the given one
     */
    public boolean isBefore(Time other) {
        return compareTo(other) < 0;
    }

    /**
     * @param other the time to compare with
     * @return true if this time is strictly later than the given one
     */
    public boolean isAfter(Time other) {
        return compareTo(other) > 0;
    }

    /**
     * @param other the time to compare with
     * @return true if this time is earlier than or equal to the given one
     */
    public boolean isSameOrBefore(Time other) {
        return compareTo(other) <= 0;
    }

    /**
     * @param other the time to compare with
     * @return true if this time is later than or equal to the given one
     */
    public boolean isSameOrAfter(Time other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Time other) {
        int result = Integer.compare(toMinutes(), other.toMinutes());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(seconds, other.seconds);
        if (result != 0) {
            return result;
        }
        return Integer.compare(nanos, other.nanos);
    }

    /**
     * Computes the duration between this time and another one.
     *
     * @param other the time to subtract
     * @return the duration from this time to {@code other}, which is negative if {@code other} is earlier
     */
    public Duration diff(Time other) {
        long seconds = (other.toMinutes() * 60L + other.seconds) - (toMinutes() * 60L + this.seconds);
        return Duration.ofSeconds(seconds, other.nanos - this.nanos);
    }

    /**
     * Formats this time with a {@link DateTimeFormatter} pattern.
     *
     * <p>{@code 24:00} is preserved when the pattern starts with {@code HH:mm}; any remaining
     * pattern is applied to midnight instead.</p>
     *
     * @param pattern the format pattern, for example {@code HH:mm:ss} or {@code hh:mm a}
     * @return the formatted time
     */
    public String format(String pattern) {
        if (hours == 24 && minutes == 0 && pattern.startsWith(TIME_FORMAT)) {
            String rest = pattern.length() > 5
                    ? LocalTime.MIDNIGHT.format(DateTimeFormatter.ofPattern(pattern.substring(5)))
                    : "";
            return "24:00" + rest;
        }
        return LocalTime.of(hours % 24, minutes, seconds, nanos).format(DateTimeFormatter.ofPattern(pattern));
    }

    @Override
    public String toString() {
        return format(TIME_FORMAT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time other = (Time) o;
        return hours == other.hours
                && minutes == other.minutes
                && seconds == other.seconds
                && nanos == other.nanos;
    }

    @Override
    public int hashCode() {
        return ((31 * hours + minutes) * 31 + seconds) * 31 + nanos;
    }
}
