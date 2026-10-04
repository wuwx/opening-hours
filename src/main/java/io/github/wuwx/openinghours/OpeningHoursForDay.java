package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.NonMutableOffsets;
import io.github.wuwx.openinghours.exceptions.OverlappingTimeRanges;

import java.time.LocalTime;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The opening hours of a single day, as an immutable set of non-overlapping {@link TimeRange}s.
 *
 * <p>Ranges are sorted by start time when the day is created, and overlapping ranges are refused:
 * building such a day throws {@link OverlappingTimeRanges}. This means the lookups below can rely
 * on the order of the ranges.</p>
 *
 * <p>The class is read-only and iterable, so {@code for (TimeRange range : monday)} is valid.</p>
 */
public class OpeningHoursForDay implements Iterable<TimeRange> {

    private final List<TimeRange> timeRanges;
    private final Object data;

    /** Creates an empty day, i.e. a day that is closed all day. */
    public OpeningHoursForDay() {
        this(new ArrayList<>(), null);
    }

    /**
     * Creates a day from the given ranges.
     *
     * @param timeRanges the ranges of the day
     * @throws OverlappingTimeRanges if two ranges overlap
     */
    public OpeningHoursForDay(List<TimeRange> timeRanges) {
        this(timeRanges, null);
    }

    /**
     * Creates a day from the given ranges, attached to some data.
     *
     * <p>The list is copied and sorted by start time; the caller keeps ownership of the original.</p>
     *
     * @param timeRanges the ranges of the day
     * @param data       arbitrary data to attach to the day, may be null
     * @throws OverlappingTimeRanges if two ranges overlap
     */
    public OpeningHoursForDay(List<TimeRange> timeRanges, Object data) {
        List<TimeRange> sorted = new ArrayList<>(timeRanges);
        sorted.sort(Comparator.comparing(TimeRange::start));
        guardAgainstTimeRangeOverlaps(sorted);
        this.timeRanges = sorted;
        this.data = data;
    }

    /**
     * Creates a day from a definition.
     *
     * <p>Accepted values are a list of range definitions, a single {@code HH:mm-HH:mm} string, a
     * map holding the ranges (or an {@code hours} key together with optional {@code data}), or an
     * existing {@link OpeningHoursForDay} which is returned as is.</p>
     *
     * @param value the definition to convert
     * @return the resulting day
     * @throws IllegalArgumentException if the definition has an unsupported type
     */
    public static OpeningHoursForDay fromStrings(Object value) {
        return fromStrings(value, null);
    }

    /**
     * Creates a day from a definition, attached to some data.
     *
     * @param value the definition to convert
     * @param data  arbitrary data to attach to the day when the definition does not provide its own
     * @return the resulting day
     * @throws IllegalArgumentException if the definition has an unsupported type
     * @see #fromStrings(Object)
     */
    @SuppressWarnings("unchecked")
    public static OpeningHoursForDay fromStrings(Object value, Object data) {
        if (value == null) {
            return new OpeningHoursForDay(new ArrayList<>(), data);
        }
        if (value instanceof OpeningHoursForDay) {
            return (OpeningHoursForDay) value;
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey("hours")) {
                return fromStrings(map.get("hours"), map.containsKey("data") ? map.get("data") : data);
            }
            List<TimeRange> ranges = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!"data".equals(entry.getKey())) {
                    ranges.add(TimeRange.fromDefinition(entry.getValue()));
                }
            }
            return new OpeningHoursForDay(ranges, map.containsKey("data") ? map.get("data") : data);
        }
        if (value instanceof List) {
            List<TimeRange> ranges = new ArrayList<>();
            for (Object item : (List<?>) value) {
                ranges.add(TimeRange.fromDefinition(item));
            }
            return new OpeningHoursForDay(ranges, data);
        }
        if (value instanceof String) {
            return new OpeningHoursForDay(Collections.singletonList(TimeRange.fromString((String) value)), data);
        }
        throw new IllegalArgumentException("Unsupported opening hours definition: " + value);
    }

    /**
     * Checks whether the day is open at the given time.
     *
     * <p>Only ranges of this day are considered; a reversed range does not cover its after-midnight
     * part here, see {@link #isOpenAtNight(Time)}.</p>
     *
     * @param time the time to check
     * @return true if any range of the day contains the time
     */
    public boolean isOpenAt(Time time) {
        for (TimeRange range : timeRanges) {
            if (range.containsTime(time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the day is open at the given local time.
     *
     * @param time the time to check
     * @return true if any range of the day contains the time
     */
    public boolean isOpenAt(LocalTime time) {
        return isOpenAt(Time.fromLocalTime(time));
    }

    /**
     * Checks whether the day is still open at the very end of the day, i.e. at {@code 23:59}.
     *
     * @return true when a range is running at {@code 23:59}
     */
    public boolean isOpenAtTheEndOfTheDay() {
        return isOpenAt(Time.fromString("23:59"));
    }

    /**
     * Checks whether the given time belongs to the after-midnight part of a reversed range of this
     * day, for example {@code 01:00} for the range {@code 22:00-02:00}.
     *
     * @param time the time to check
     * @return true if a reversed range of the day spills over the given time
     */
    public boolean isOpenAtNight(Time time) {
        for (TimeRange range : timeRanges) {
            if (range.containsNightTime(time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the next opening time of the day after the given time.
     *
     * @param time the time to search from
     * @return the start of the next range, or empty when the day is already open or closed for good
     */
    public Optional<Time> nextOpen(Time time) {
        for (TimeRange range : timeRanges) {
            if (time.isBefore(range.start())) {
                return Optional.of(range.start());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the next range starting after the given time.
     *
     * @param time the time to search from
     * @return the next range, or empty when there is none left today
     */
    public Optional<TimeRange> nextOpenRange(Time time) {
        for (TimeRange range : timeRanges) {
            if (time.isBefore(range.start())) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the next closing time of the day at or after the given time: the end of the range
     * that is currently running, or the end of the next one when the day is closed.
     *
     * @param time the time to search from
     * @return the next closing time, or empty when there is none left today
     */
    public Optional<Time> nextClose(Time time) {
        for (TimeRange range : timeRanges) {
            if (range.containsTime(time) || time.isBefore(range.start())) {
                return Optional.of(range.end());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the range used to determine the {@link #nextClose(Time)} time.
     *
     * @param time the time to search from
     * @return the running or next range, or empty when there is none left today
     */
    public Optional<TimeRange> nextCloseRange(Time time) {
        for (TimeRange range : timeRanges) {
            if (range.containsTime(time) || time.isBefore(range.start())) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the opening time of the range running at the given time, or of the closest earlier
     * range.
     *
     * @param time the time to search from
     * @return the previous opening time, or empty when the day had not opened yet
     */
    public Optional<Time> previousOpen(Time time) {
        for (int i = timeRanges.size() - 1; i >= 0; i--) {
            TimeRange range = timeRanges.get(i);
            if (time.isAfter(range.start())) {
                return Optional.of(range.start());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the range used to determine the {@link #previousOpen(Time)} time, searching the day
     * backwards.
     *
     * @param time the time to search from
     * @return the previous range, or empty when there is none before the given time
     */
    public Optional<TimeRange> previousOpenRange(Time time) {
        for (int i = timeRanges.size() - 1; i >= 0; i--) {
            TimeRange range = timeRanges.get(i);
            if (time.isBefore(range.start())) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the closing time of the closest range ending before the given time.
     *
     * @param time the time to search from
     * @return the previous closing time, or empty when the day had not closed yet
     */
    public Optional<Time> previousClose(Time time) {
        for (int i = timeRanges.size() - 1; i >= 0; i--) {
            TimeRange range = timeRanges.get(i);
            if (time.isAfter(range.end())) {
                return Optional.of(range.end());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the range used to determine the {@link #previousClose(Time)} time, searching the day
     * backwards.
     *
     * @param time the time to search from
     * @return the previous closed range, or empty when there is none before the given time
     */
    public Optional<TimeRange> previousCloseRange(Time time) {
        for (int i = timeRanges.size() - 1; i >= 0; i--) {
            TimeRange range = timeRanges.get(i);
            if (time.isAfter(range.end()) && time.isAfter(range.start())) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns every range of the day containing the given time.
     *
     * @param time the time to look for
     * @return the matching ranges, in order
     */
    public List<TimeRange> forTime(Time time) {
        List<TimeRange> result = new ArrayList<>();
        for (TimeRange range : timeRanges) {
            if (range.containsTime(time)) {
                result.add(range);
            }
        }
        return result;
    }

    /**
     * Returns every reversed range of the day whose after-midnight part contains the given time.
     *
     * @param time the time to look for
     * @return the matching ranges, in order
     */
    public List<TimeRange> forNightTime(Time time) {
        List<TimeRange> result = new ArrayList<>();
        for (TimeRange range : timeRanges) {
            if (range.containsNightTime(time)) {
                result.add(range);
            }
        }
        return result;
    }

    /** @return true when the day holds no range at all, i.e. it is closed all day */
    public boolean isEmpty() {
        return timeRanges.isEmpty();
    }

    /** Iterates over the ranges of the day, in chronological order. */
    @Override
    public Iterator<TimeRange> iterator() {
        return getTimeRanges().iterator();
    }

    /**
     * Returns the ranges of the day, sorted by start time.
     *
     * <p>The returned list is read-only: any attempt to modify it throws a {@link NonMutableOffsets}
     * exception, as the original library does through its read-only {@code ArrayAccess}.</p>
     *
     * @return the ranges of the day
     */
    public List<TimeRange> getTimeRanges() {
        return new NonMutableRangeList(timeRanges);
    }

    /**
     * Returns the range at the given position, in chronological order.
     *
     * @param index the position of the range
     * @return the range at that position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public TimeRange get(int index) {
        return timeRanges.get(index);
    }

    /** @return the number of ranges in the day */
    public int size() {
        return timeRanges.size();
    }

    /** @return the data attached to the day, or null when there is none */
    public Object data() {
        return data;
    }

    /**
     * @return the data attached to the day, or null when there is none
     * @deprecated use {@link #data()} instead; the original library deprecates {@code getData()} in
     * favour of the {@code data} property, and this method is only kept for compatibility
     */
    @Deprecated
    public Object getData() {
        return data();
    }

    /**
     * Two days are equal when they hold the same ranges and the same data.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpeningHoursForDay other = (OpeningHoursForDay) o;
        return timeRanges.equals(other.timeRanges)
                && java.util.Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return 31 * timeRanges.hashCode() + java.util.Objects.hashCode(data);
    }

    /**
     * Applies a callback to every range of the day.
     *
     * @param callback the callback to apply
     * @param <T>      the type returned by the callback
     * @return the mapped values, in the order of the ranges
     */
    public <T> List<T> map(java.util.function.Function<TimeRange, T> callback) {
        List<T> result = new ArrayList<>();
        for (TimeRange range : timeRanges) {
            result.add(callback.apply(range));
        }
        return result;
    }

    /** @return the ranges of the day as a comma separated string, for example {@code "09:00-12:00,13:00-18:00"} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TimeRange range : timeRanges) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(range.toString());
        }
        return sb.toString();
    }

    private static void guardAgainstTimeRangeOverlaps(List<TimeRange> ranges) {
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(i).overlaps(ranges.get(j))) {
                    throw OverlappingTimeRanges.forRanges(ranges.get(i).toString(), ranges.get(j).toString());
                }
            }
        }
    }

    /**
     * Read-only view over the ranges of a day. Every mutating operation throws
     * {@link NonMutableOffsets}, mirroring the original library's read-only {@code ArrayAccess}.
     */
    private static final class NonMutableRangeList extends AbstractList<TimeRange> {

        private final List<TimeRange> delegate;

        NonMutableRangeList(List<TimeRange> delegate) {
            this.delegate = delegate;
        }

        @Override
        public TimeRange get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public TimeRange set(int index, TimeRange element) {
            throw NonMutableOffsets.forClass("OpeningHoursForDay");
        }

        @Override
        public void add(int index, TimeRange element) {
            throw NonMutableOffsets.forClass("OpeningHoursForDay");
        }

        @Override
        public TimeRange remove(int index) {
            throw NonMutableOffsets.forClass("OpeningHoursForDay");
        }
    }
}
