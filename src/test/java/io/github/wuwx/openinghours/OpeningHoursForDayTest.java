package io.github.wuwx.openinghours;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

public class OpeningHoursForDayTest {
    
    @Test
    public void testFromStrings() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00"));
        assertEquals(1, openingHoursForDay1.size());

        OpeningHoursForDay openingHoursForDay2 = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00", "10:00-11:00"));
        assertEquals(2, openingHoursForDay2.size());
    }

    @Test
    public void testIsEmpty() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00"));
        assertFalse(openingHoursForDay1.isEmpty());

        OpeningHoursForDay openingHoursForDay2 = OpeningHoursForDay.fromStrings(Collections.emptyList());
        assertTrue(openingHoursForDay2.isEmpty());
    }

    @Test
    public void testIsOpenAt() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00"));
        assertTrue(openingHoursForDay1.isOpenAt(LocalTime.of(8, 30)));
        assertFalse(openingHoursForDay1.isOpenAt(LocalTime.of(9, 30)));
    }
    
    @Test
    public void testGetTimeRanges() {
        OpeningHoursForDay openingHoursForDay = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00", "10:00-11:00"));
        assertEquals(2, openingHoursForDay.getTimeRanges().size());
    }
    
    @Test
    public void testGet() {
        OpeningHoursForDay openingHoursForDay = OpeningHoursForDay.fromStrings(Arrays.asList("08:00-09:00", "10:00-11:00"));
        TimeRange firstRange = openingHoursForDay.get(0);
        assertNotNull(firstRange);
        assertEquals("08:00-09:00", firstRange.toString());
    }
}