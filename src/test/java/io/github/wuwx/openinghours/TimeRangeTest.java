package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidTimeRangeString;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TimeRangeTest {

    @Test
    public void testFromString() {
        TimeRange range = TimeRange.fromString("09:00-18:00");
        assertEquals(Time.fromString("09:00"), range.start());
        assertEquals(Time.fromString("18:00"), range.end());
        assertEquals("09:00-18:00", range.toString());
        assertEquals("09:00-24:00", TimeRange.fromString("09:00-24:00").toString());
    }

    @Test(expected = InvalidTimeRangeString.class)
    public void testInvalidRangeString() {
        TimeRange.fromString("09:00");
    }

    @Test
    public void testContainsTime() {
        TimeRange range = TimeRange.fromString("09:00-18:00");
        assertFalse(range.containsTime(Time.fromString("08:59")));
        assertTrue(range.containsTime(Time.fromString("09:00")));
        assertTrue(range.containsTime(Time.fromString("17:59")));
        assertFalse(range.containsTime(Time.fromString("18:00")));
    }

    @Test
    public void testContainsTimeUntilMidnight() {
        TimeRange range = TimeRange.fromString("09:00-24:00");
        assertFalse(range.containsTime(Time.fromString("08:00")));
        assertTrue(range.containsTime(Time.fromString("09:00")));
        assertTrue(range.containsTime(Time.fromString("23:59")));
        assertFalse(range.overflowsNextDay());
    }

    @Test
    public void testReversedRange() {
        TimeRange range = TimeRange.fromString("22:00-02:00");
        assertTrue(range.overflowsNextDay());
        assertTrue(range.containsTime(Time.fromString("22:00")));
        assertTrue(range.containsTime(Time.fromString("23:30")));
        assertFalse(range.containsTime(Time.fromString("02:00")));
        assertFalse(range.containsTime(Time.fromString("01:00")));
        assertTrue(range.containsNightTime(Time.fromString("01:00")));
        assertFalse(range.containsNightTime(Time.fromString("23:00")));
    }

    @Test
    public void testOverlaps() {
        TimeRange morning = TimeRange.fromString("09:00-12:00");
        assertTrue(morning.overlaps(TimeRange.fromString("11:00-14:00")));
        assertFalse(morning.overlaps(TimeRange.fromString("13:00-15:00")));
        assertFalse(morning.overlaps(TimeRange.fromString("12:00-15:00")));
    }

    @Test
    public void testCustomFormats() {
        TimeRange range = TimeRange.fromString("09:00-18:00");

        assertEquals("09:00-18:00", range.format());
        assertEquals("09:00-18:00", range.format("HH:mm"));
        assertEquals("09:00 – 18:00", range.format("HH:mm", "%s – %s"));
        assertEquals("9-18", range.format("H", "%s-%s"));
        assertEquals("09:00-24:00", TimeRange.fromString("09:00-24:00").format("HH:mm", "%s-%s"));
    }

    @Test
    public void testFromMidnight() {
        TimeRange range = TimeRange.fromMidnight(Time.fromString("02:00"));

        assertEquals("00:00-02:00", range.toString());
        assertEquals(Time.MIDNIGHT, range.start());
        assertNull(range.data());
        assertEquals("data", TimeRange.fromMidnight(Time.fromString("02:00"), "data").data());
    }

    @Test
    public void testFromArray() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("hours", "09:00-18:00");
        definition.put("data", "hello");

        TimeRange range = TimeRange.fromArray(definition);

        assertEquals("09:00-18:00", range.toString());
        assertEquals("hello", range.data());
    }

    @Test
    public void testFromDefinition() {
        assertEquals("09:00-18:00", TimeRange.fromDefinition("09:00-18:00").toString());

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("hours", "09:00-18:00");
        assertEquals("09:00-18:00", TimeRange.fromDefinition(definition).toString());

        TimeRange range = TimeRange.fromString("09:00-18:00");
        assertEquals(range, TimeRange.fromDefinition(range));
    }

    @Test
    public void testFromList() {
        TimeRange merged = TimeRange.fromList(Arrays.asList(
                TimeRange.fromString("13:00-18:00"),
                TimeRange.fromString("09:00-12:00")));

        assertEquals("09:00-18:00", merged.toString());
        assertNull(merged.data());
        assertEquals("data", TimeRange.fromList(
                Arrays.asList(TimeRange.fromString("09:00-12:00")), "data").data());
    }

    @Test
    public void testData() {
        TimeRange range = TimeRange.fromString("09:00-18:00", "hello");
        assertEquals("hello", range.data());
        assertNull(TimeRange.fromString("09:00-18:00").data());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedGetDataStillWorks() {
        assertEquals("hello", TimeRange.fromString("09:00-18:00", "hello").getData());
        assertNull(TimeRange.fromString("09:00-18:00").getData());
    }

    @Test
    public void testDateTimeRangeFromTimeRange() {
        ZoneId zone = ZoneId.of("UTC");

        // the reference moment falls inside a plain range
        DateTimeRange plain = DateTimeRange.fromTimeRange(
                ZonedDateTime.of(2020, 1, 6, 10, 0, 0, 0, zone),
                TimeRange.fromString("09:00-18:00"));
        assertEquals(ZonedDateTime.of(2020, 1, 6, 9, 0, 0, 0, zone), plain.start());
        assertEquals(ZonedDateTime.of(2020, 1, 6, 18, 0, 0, 0, zone), plain.end());
        assertFalse(plain.overflowsNextDay());

        // late evening inside 22:00-02:00: the end belongs to the next day
        DateTimeRange late = DateTimeRange.fromTimeRange(
                ZonedDateTime.of(2020, 1, 6, 23, 0, 0, 0, zone),
                TimeRange.fromString("22:00-02:00"));
        assertEquals(ZonedDateTime.of(2020, 1, 6, 22, 0, 0, 0, zone), late.start());
        assertEquals(ZonedDateTime.of(2020, 1, 7, 2, 0, 0, 0, zone), late.end());
        assertTrue(late.overflowsNextDay());

        // early morning inside 22:00-02:00: the start belongs to the previous day
        DateTimeRange early = DateTimeRange.fromTimeRange(
                ZonedDateTime.of(2020, 1, 7, 1, 0, 0, 0, zone),
                TimeRange.fromString("22:00-02:00"));
        assertEquals(ZonedDateTime.of(2020, 1, 6, 22, 0, 0, 0, zone), early.start());
        assertEquals(ZonedDateTime.of(2020, 1, 7, 2, 0, 0, 0, zone), early.end());

        // data defaults to the one of the range, and can be overridden
        assertEquals("hello", DateTimeRange.fromTimeRange(
                ZonedDateTime.of(2020, 1, 6, 10, 0, 0, 0, zone),
                TimeRange.fromString("09:00-18:00", "hello")).data());
        assertEquals("other", DateTimeRange.fromTimeRange(
                ZonedDateTime.of(2020, 1, 6, 10, 0, 0, 0, zone),
                TimeRange.fromString("09:00-18:00", "hello"), "other").data());
    }
}
