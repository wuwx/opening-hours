package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidTimeString;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class TimeTest {

    @Test
    public void testFromString() {
        assertEquals(9, Time.fromString("09:00").hours());
        assertEquals(0, Time.fromString("09:00").minutes());
        assertEquals(24, Time.fromString("24:00").hours());
        assertEquals(0, Time.fromString("24:00").minutes());
        assertEquals("09:00", Time.fromString("09:00").toString());
        assertEquals("24:00", Time.fromString("24:00").toString());
    }

    @Test(expected = InvalidTimeString.class)
    public void testInvalidTimeString() {
        Time.fromString("25:00");
    }

    @Test(expected = InvalidTimeString.class)
    public void testInvalidTimeStringNoColon() {
        Time.fromString("0900");
    }

    @Test
    public void testFromDateTime() {
        Time time = Time.fromDateTime(LocalDateTime.of(2016, 12, 24, 13, 37, 59));
        assertEquals(13, time.hours());
        assertEquals(37, time.minutes());
    }

    @Test
    public void testToDateTime() {
        LocalDate date = LocalDate.of(2020, 1, 1);

        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0), Time.MIDNIGHT.toDateTime(date));
        assertEquals(LocalDateTime.of(2020, 1, 1, 9, 30), Time.fromString("09:30").toDateTime(date));
        assertEquals(LocalDateTime.of(2020, 1, 1, 23, 59), Time.fromString("23:59").toDateTime(date));
        // 24:00 is midnight of the next day, not midnight of the same day
        assertEquals(LocalDateTime.of(2020, 1, 2, 0, 0), Time.fromString("24:00").toDateTime(date));
        assertNotEquals(Time.MIDNIGHT.toDateTime(date), Time.fromString("24:00").toDateTime(date));
        // and it rolls the month and year over correctly
        assertEquals(LocalDateTime.of(2020, 3, 1, 0, 0), Time.fromString("24:00").toDateTime(LocalDate.of(2020, 2, 29)));
        assertEquals(LocalDateTime.of(2021, 1, 1, 0, 0), Time.fromString("24:00").toDateTime(LocalDate.of(2020, 12, 31)));
    }

    @Test
    public void testComparisons() {
        Time nine = Time.fromString("09:00");
        Time midnightEnd = Time.fromString("24:00");
        assertTrue(nine.isBefore(midnightEnd));
        assertTrue(midnightEnd.isAfter(nine));
        assertTrue(nine.isSameOrAfter(nine));
        assertTrue(nine.isSameOrBefore(nine));
        assertFalse(nine.isSame(midnightEnd));
        assertTrue(Time.MIDNIGHT.isBefore(nine));
    }

    @Test
    public void testFormat() {
        assertEquals("09:00", Time.fromString("09:00").format("HH:mm"));
        assertEquals("24:00", Time.fromString("24:00").format("HH:mm"));
        assertEquals("24:00:00", Time.fromString("24:00").format("HH:mm:ss"));
        assertEquals("09:00:00", Time.fromString("09:00").format("HH:mm:ss"));
        assertEquals("9 o'clock", Time.fromString("09:00").format("H 'o''clock'"));
    }

    @Test
    public void testDiff() {
        assertEquals(Duration.ofMinutes(90), Time.fromString("09:00").diff(Time.fromString("10:30")));
        assertEquals(Duration.ofMinutes(-90), Time.fromString("10:30").diff(Time.fromString("09:00")));
        assertEquals(Duration.ZERO, Time.fromString("09:00").diff(Time.fromString("09:00")));
        assertEquals(Duration.ofHours(15), Time.fromString("09:00").diff(Time.fromString("24:00")));
    }

    @Test
    public void testEqualsAndHashCode() {
        assertEquals(Time.fromString("09:00"), Time.fromString("09:00"));
        assertNotEquals(Time.fromString("09:00"), Time.fromString("24:00"));
        assertEquals(Time.fromString("09:00").hashCode(), Time.fromString("09:00").hashCode());
    }
}
