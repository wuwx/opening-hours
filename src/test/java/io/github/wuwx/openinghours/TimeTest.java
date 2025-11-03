package io.github.wuwx.openinghours;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalTime;

/**
 * Tests for Time class
 */
public class TimeTest {
    
    @Test
    public void testFromString() {
        Time time = Time.fromString("14:30");
        assertEquals("14:30", time.toString());
    }
    
    @Test
    public void testSpecialMidnight() {
        Time midnight = Time.fromString("24:00");
        assertNotNull(midnight);
        // 24:00 should be treated as end of day
        assertEquals(LocalTime.MAX, midnight.toLocalTime());
    }
    
    @Test
    public void testComparison() {
        Time time1 = Time.fromString("09:00");
        Time time2 = Time.fromString("12:00");
        Time time3 = Time.fromString("09:00");
        
        assertTrue(time1.isBefore(time2));
        assertFalse(time2.isBefore(time1));
        assertTrue(time2.isAfter(time1));
        assertFalse(time1.isAfter(time2));
        assertTrue(time1.isSameOrAfter(time3));
        assertTrue(time2.isSameOrAfter(time1));
    }
    
    @Test
    public void testFormat() {
        Time time = Time.fromString("14:30");
        assertEquals("14:30", time.format("HH:mm"));
        // Note: Skip locale-specific format test as it varies by system locale
        // assertEquals("02:30 PM", time.format("hh:mm a")); // Would fail in non-English locale
    }
    
    @Test
    public void testEquals() {
        Time time1 = Time.fromString("09:00");
        Time time2 = Time.fromString("09:00");
        Time time3 = Time.fromString("10:00");
        
        assertEquals(time1, time2);
        assertNotEquals(time1, time3);
    }
}

