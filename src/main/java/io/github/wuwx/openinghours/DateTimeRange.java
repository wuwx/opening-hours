package io.github.wuwx.openinghours;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A {@link TimeRange} projected on the calendar, with a concrete start and end {@link ZonedDateTime}.
 *
 * <p>This is what {@code OpeningHours.currentOpenRange()} returns: the same information as a
 * {@code TimeRange}, but with the dates resolved and the timezones applied. Unlike the original
 * library, where {@code DateTimeRange} extends {@code TimeRange} and carries the dates inside its
 * {@code Time}s, this class composes a {@link TimeRange} and exposes the resolved moments.</p>
 */
public class DateTimeRange {

    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final TimeRange timeRange;

    DateTimeRange(ZonedDateTime start, ZonedDateTime end, TimeRange timeRange) {
        this.start = start;
        this.end = end;
        this.timeRange = timeRange;
    }

    /**
     * Projects a time range on the given date, the counterpart of the original library's
     * {@code DateTimeRange::fromTimeRange()}.
     *
     * <p>The date acts as a reference point: when the start time is later than the reference time it
     * is placed on the <em>previous</em> day, and when the end time is earlier it is placed on the
     * <em>next</em> day. A reversed range such as {@code 22:00-02:00} therefore resolves to
     * {@code 22:00} on the reference date and {@code 02:00} on the day after.</p>
     *
     * @param date      the reference date time, its zone is kept
     * @param timeRange the range to project
     * @return the resulting date time range
     */
    public static DateTimeRange fromTimeRange(ZonedDateTime date, TimeRange timeRange) {
        return fromTimeRange(date, timeRange, timeRange.data());
    }

    /**
     * Projects a time range on the given date, attached to some data.
     *
     * @param date      the reference date time, its zone is kept
     * @param timeRange the range to project
     * @param data      the data attached to the resulting range
     * @return the resulting date time range
     * @see #fromTimeRange(ZonedDateTime, TimeRange)
     */
    public static DateTimeRange fromTimeRange(ZonedDateTime date, TimeRange timeRange, Object data) {
        LocalDate day = date.toLocalDate();
        ZoneId zone = date.getZone();
        Time reference = Time.fromDateTime(date.toLocalDateTime());

        ZonedDateTime start = timeRange.start().toDateTime(day).atZone(zone);
        if (timeRange.start().isAfter(reference)) {
            start = start.minusDays(1);
        }

        ZonedDateTime end = timeRange.end().toDateTime(day).atZone(zone);
        if (timeRange.end().isBefore(reference)) {
            end = end.plusDays(1);
        }

        return new DateTimeRange(start, end, new TimeRange(timeRange.start(), timeRange.end(), data));
    }

    /** @return the start of the range, with its date and timezone */
    public ZonedDateTime start() {
        return start;
    }

    /** @return the end of the range, with its date and timezone */
    public ZonedDateTime end() {
        return end;
    }

    /** @return the underlying {@link TimeRange} this range was built from */
    public TimeRange timeRange() {
        return timeRange;
    }

    /** @return the data attached to the underlying range, or null when there is none */
    public Object data() {
        return timeRange.data();
    }

    /**
     * @return the data attached to the underlying range, or null when there is none
     * @deprecated use {@link #data()} instead, as in the original library where {@code getData()} is
     * deprecated in favour of the {@code data} property
     */
    @Deprecated
    public Object getData() {
        return data();
    }

    /** @return true if the underlying range spills over to the next day */
    public boolean overflowsNextDay() {
        return timeRange.overflowsNextDay();
    }

    @Override
    public String toString() {
        return start + " - " + end;
    }

    /**
     * Two date time ranges are equal when they have the same start, end and underlying time range.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateTimeRange other = (DateTimeRange) o;
        return start.equals(other.start) && end.equals(other.end) && timeRange.equals(other.timeRange);
    }

    @Override
    public int hashCode() {
        return ((31 * start.hashCode()) + end.hashCode()) * 31 + timeRange.hashCode();
    }
}
