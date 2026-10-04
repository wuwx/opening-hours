package io.github.wuwx.openinghours;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.*;

// PHP 用独立的 PreciseTime 保存秒级精度，Java 中这一职责由 Time 承担
public class PreciseTimeTest {

    @Test
    public void testKeepsHourAndMinuteFromDateTime() {
        Time time = Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 13, 37, 42, 123_000_000));

        assertEquals(13, time.hours());
        assertEquals(37, time.minutes());
        assertEquals(42, time.seconds());
    }

    @Test
    public void testKeepsSecondsFromLocalTime() {
        Time time = Time.fromLocalTime(LocalTime.of(23, 59, 59, 999_000_000));

        assertEquals(23, time.hours());
        assertEquals(59, time.minutes());
        assertEquals(59, time.seconds());
    }

    @Test
    public void testComparesWithSecondsPrecision() {
        Time lastMinute = Time.fromString("23:59");
        Time lastSecond = Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 23, 59, 59));

        assertTrue(lastSecond.isAfter(lastMinute));
        assertFalse(lastMinute.isAfter(lastSecond));
        assertFalse(lastSecond.isSame(lastMinute));
    }

    @Test
    public void testRangeEndingAtLastMinuteExcludesTheLastSecond() {
        TimeRange range = TimeRange.fromString("10:00-23:59");

        assertTrue(range.containsTime(Time.fromString("23:58")));
        assertTrue(range.containsTime(Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 23, 58, 59))));
        assertFalse(range.containsTime(Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 23, 59, 59))));
    }

    @Test
    public void testRangeUntilMidnightIncludesTheLastSecond() {
        TimeRange range = TimeRange.fromString("10:00-24:00");

        assertTrue(range.containsTime(Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 23, 59, 59))));
        assertFalse(range.containsTime(Time.fromString("24:00")));
    }

    @Test
    public void testFormatsWithoutSecondsByDefault() {
        Time time = Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 23, 59, 59));

        assertEquals("23:59", time.toString());
        assertEquals("23:59:59", time.format("HH:mm:ss"));
    }
}
