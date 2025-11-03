package io.github.wuwx.openinghours;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalTime;

public class TimeRangeTest {

    @Test
    public void testFromString() {
        TimeRange timeRange = TimeRange.fromString("20:00-21:00");

        assertEquals(Time.fromString("20:00"), timeRange.start());
        assertEquals(Time.fromString("21:00"), timeRange.end());
        assertEquals("20:00-21:00", timeRange.toString());
    }

    @Test
    public void testContainsTime() {
        TimeRange timeRange = TimeRange.fromString("20:00-21:00");

        assertTrue(timeRange.containsTime(LocalTime.of(20, 30)));
        assertFalse(timeRange.containsTime(LocalTime.of(21, 30)));
        assertFalse(timeRange.containsTime(LocalTime.of(19, 30)));
    }
    
    @Test
    public void testOvernightRange() {
        TimeRange timeRange = TimeRange.fromString("22:00-02:00");
        
        // Should be open at 23:00
        assertTrue(timeRange.containsTime(LocalTime.of(23, 0)));
        // Should be open at 01:00
        assertTrue(timeRange.containsTime(LocalTime.of(1, 0)));
        // Should be closed at 03:00
        assertFalse(timeRange.containsTime(LocalTime.of(3, 0)));
        // Should be closed at 10:00
        assertFalse(timeRange.containsTime(LocalTime.of(10, 0)));
    }

}