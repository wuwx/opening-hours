package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidTimeRangeArray;
import io.github.wuwx.openinghours.exceptions.InvalidTimeRangeList;
import io.github.wuwx.openinghours.exceptions.InvalidTimeRangeString;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * A period between two {@link Time}s, written as {@code HH:mm-HH:mm}.
 *
 * <p>A range is <em>reversed</em> when its end is before its start, for example {@code 22:00-02:00}.
 * Such a range only covers {@code 22:00} to midnight on its own day; the {@code 00:00-02:00} part
 * belongs to the next day and is only taken into account when overflow is enabled on the
 * {@link OpeningHours} instance.</p>
 *
 * <p>The end time may be {@code 24:00}, which means midnight <em>included</em>, so
 * {@code 09:00-24:00} covers the whole evening.</p>
 *
 * <p>Instances are immutable and can only be created through the static factory methods.</p>
 */
public class TimeRange {

    private final Time start;
    private final Time end;
    private final Object data;

    TimeRange(Time start, Time end) {
        this(start, end, null);
    }

    TimeRange(Time start, Time end, Object data) {
        this.start = start;
        this.end = end;
        this.data = data;
    }

    /**
     * Creates a range from a {@code HH:mm-HH:mm} string.
     *
     * @param timeRangeString the string to parse
     * @return the parsed range
     * @throws InvalidTimeRangeString if the string is not a valid range
     */
    public static TimeRange fromString(String timeRangeString) {
        return fromString(timeRangeString, null);
    }

    /**
     * Creates a range from a {@code HH:mm-HH:mm} string, attached to some data.
     *
     * @param timeRangeString the string to parse
     * @param data            arbitrary data to attach to the range, may be null
     * @return the parsed range
     * @throws InvalidTimeRangeString if the string is not a valid range
     */
    public static TimeRange fromString(String timeRangeString, Object data) {
        String[] times = timeRangeString.split("-");
        if (times.length != 2) {
            throw InvalidTimeRangeString.forString(timeRangeString);
        }
        return new TimeRange(Time.fromString(times[0]), Time.fromString(times[1]), data);
    }

    /**
     * Creates a range from a map holding the hours under the {@code hours} key, and optional
     * {@code data}. When there is no {@code hours} key, the first value that is not {@code data}
     * is used.
     *
     * @param array the map describing the range
     * @return the parsed range
     * @throws InvalidTimeRangeArray if the map does not hold a string range
     */
    public static TimeRange fromArray(Map<String, Object> array) {
        Object hours = array.get("hours");
        Object data = array.get("data");
        if (hours == null) {
            for (Map.Entry<String, Object> entry : array.entrySet()) {
                if (!"data".equals(entry.getKey())) {
                    hours = entry.getValue();
                    break;
                }
            }
        }
        if (!(hours instanceof String)) {
            throw InvalidTimeRangeArray.create();
        }
        return fromString((String) hours, data);
    }

    /**
     * Creates a range from any supported definition: an existing {@link TimeRange}, a map, or a
     * {@code HH:mm-HH:mm} string.
     *
     * @param value the definition to convert
     * @return the equivalent range
     * @throws InvalidTimeRangeString if a string definition is malformed
     */
    @SuppressWarnings("unchecked")
    public static TimeRange fromDefinition(Object value) {
        if (value instanceof TimeRange) {
            return (TimeRange) value;
        }
        if (value instanceof Map) {
            return fromArray((Map<String, Object>) value);
        }
        return fromString((String) value);
    }

    /**
     * Creates a range starting at midnight.
     *
     * @param end the end of the range
     * @return a range from {@code 00:00} to the given end
     */
    public static TimeRange fromMidnight(Time end) {
        return fromMidnight(end, null);
    }

    /**
     * Creates a range starting at midnight, attached to some data.
     *
     * @param end  the end of the range
     * @param data arbitrary data to attach to the range, may be null
     * @return a range from {@code 00:00} to the given end
     */
    public static TimeRange fromMidnight(Time end, Object data) {
        return new TimeRange(Time.MIDNIGHT, end, data);
    }

    /**
     * Creates the smallest range covering all the given ranges, from the earliest start to the
     * latest end.
     *
     * @param ranges the ranges to merge
     * @return a single range covering all of them
     * @throws InvalidTimeRangeList if the list is null or empty
     */
    public static TimeRange fromList(List<TimeRange> ranges) {
        return fromList(ranges, null);
    }

    /**
     * Creates the smallest range covering all the given ranges, attached to some data.
     *
     * @param ranges the ranges to merge
     * @param data   arbitrary data to attach to the resulting range, may be null
     * @return a single range covering all of them
     * @throws InvalidTimeRangeList if the list is null or empty
     */
    public static TimeRange fromList(List<TimeRange> ranges, Object data) {
        if (ranges == null || ranges.isEmpty()) {
            throw InvalidTimeRangeList.create();
        }
        Time start = ranges.get(0).start();
        Time end = ranges.get(0).end();
        for (TimeRange range : ranges) {
            if (range.start().isBefore(start)) {
                start = range.start();
            }
            if (range.end().isAfter(end)) {
                end = range.end();
            }
        }
        return new TimeRange(start, end, data);
    }

    /** @return the start of the range */
    public Time start() {
        return start;
    }

    /** @return the end of the range */
    public Time end() {
        return end;
    }

    /** @return the data attached to the range, or null when there is none */
    public Object data() {
        return data;
    }

    /**
     * @return the data attached to the range, or null when there is none
     * @deprecated use {@link #data()} instead; the original library deprecates {@code getData()} in
     * favour of the {@code data} property, and this method is only kept for compatibility
     */
    @Deprecated
    public Object getData() {
        return data();
    }

    /** @return true if the end is before the start, for example {@code 22:00-02:00} */
    public boolean isReversed() {
        return start.isAfter(end);
    }

    /**
     * Alias of {@link #isReversed()}.
     *
     * @return true if the range spills over to the next day
     */
    public boolean overflowsNextDay() {
        return isReversed();
    }

    /**
     * Alias of {@link #isReversed()}.
     *
     * @return true if the range spills over to the next day
     */
    public boolean spillsOverToNextDay() {
        return isReversed();
    }

    /**
     * Checks whether the given time falls inside the range on the range's own day.
     *
     * <p>For a reversed range only the part up to midnight is covered; use
     * {@link #containsNightTime(Time)} for the part after midnight.</p>
     *
     * @param time the time to check
     * @return true if the time is inside the range
     */
    public boolean containsTime(Time time) {
        return time.isSameOrAfter(start) && (overflowsNextDay() || time.isBefore(end));
    }

    /**
     * Checks whether the given local time falls inside the range on the range's own day.
     *
     * @param localTime the time to check
     * @return true if the time is inside the range
     */
    public boolean containsTime(LocalTime localTime) {
        return containsTime(Time.fromLocalTime(localTime));
    }

    /**
     * Checks whether the given time falls in the part of a reversed range that spills over to the
     * next day, between midnight and the end of the range.
     *
     * @param time the time to check, typically an early morning time
     * @return true if the time belongs to the night part of a reversed range
     */
    public boolean containsNightTime(Time time) {
        return overflowsNextDay() && new TimeRange(Time.MIDNIGHT, end).containsTime(time);
    }

    /**
     * Checks whether this range overlaps another one.
     *
     * @param other the range to compare with
     * @return true if the two ranges share at least one time
     */
    public boolean overlaps(TimeRange other) {
        return containsTime(other.start()) || containsTime(other.end());
    }

    /**
     * Formats the range as {@code HH:mm-HH:mm}.
     *
     * @return the formatted range
     */
    public String format() {
        return format("HH:mm", "%s-%s");
    }

    /**
     * Formats the range with a custom time format.
     *
     * @param timeFormat the {@link java.time.format.DateTimeFormatter} pattern for both times
     * @return the formatted range
     */
    public String format(String timeFormat) {
        return format(timeFormat, "%s-%s");
    }

    /**
     * Formats the range with a custom time format and separator.
     *
     * @param timeFormat  the {@link java.time.format.DateTimeFormatter} pattern for both times
     * @param rangeFormat a {@link String#format} pattern receiving the start and end as arguments,
     *                    for example {@code "%s - %s"}
     * @return the formatted range
     */
    public String format(String timeFormat, String rangeFormat) {
        return String.format(rangeFormat, start.format(timeFormat), end.format(timeFormat));
    }

    @Override
    public String toString() {
        return format();
    }

    /**
     * Two ranges are equal when they have the same start and end; the attached data is ignored.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRange other = (TimeRange) o;
        return start.equals(other.start) && end.equals(other.end);
    }

    @Override
    public int hashCode() {
        return 31 * start.hashCode() + end.hashCode();
    }
}
