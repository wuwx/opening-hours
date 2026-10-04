package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.NonMutableOffsets;
import io.github.wuwx.openinghours.exceptions.OverlappingTimeRanges;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class OpeningHoursForDayTest {

    @Test
    public void testFromStrings() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("13:00-18:00", "09:00-12:00"));
        assertEquals(2, day.size());
        assertEquals("09:00-12:00", day.get(0).toString());
        assertEquals("13:00-18:00", day.get(1).toString());
        assertEquals("09:00-12:00,13:00-18:00", day.toString());
    }

    @Test
    public void testFromMapWithHoursAndData() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hours", Arrays.asList("09:00-12:00"));
        map.put("data", "morning");
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(map);
        assertEquals(1, day.size());
        assertEquals("morning", day.getData());
    }

    @Test
    public void testFromMixedList() {
        List<Object> list = new ArrayList<>();
        list.add("09:00-12:00");
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("hours", "13:00-15:00");
        slot.put("data", "afternoon");
        list.add(slot);
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(list);
        assertEquals(2, day.size());
        assertEquals("afternoon", day.get(1).getData());
    }

    @Test(expected = OverlappingTimeRanges.class)
    public void testOverlappingRangesThrow() {
        OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "11:00-14:00"));
    }

    @Test
    public void testIsOpenAt() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));
        assertTrue(day.isOpenAt(Time.fromString("09:00")));
        assertFalse(day.isOpenAt(Time.fromString("12:30")));
        assertTrue(day.isOpenAt(Time.fromString("13:00")));
        assertFalse(day.isOpenAt(Time.fromString("18:00")));
    }

    @Test
    public void testIsOpenAtTheEndOfTheDay() {
        assertFalse(OpeningHoursForDay.fromStrings(Arrays.asList("09:00-18:00")).isOpenAtTheEndOfTheDay());
        assertTrue(OpeningHoursForDay.fromStrings(Arrays.asList("09:00-24:00")).isOpenAtTheEndOfTheDay());
    }

    @Test
    public void testIsOpenAtNight() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("22:00-02:00"));
        assertTrue(day.isOpenAtNight(Time.fromString("01:00")));
        assertFalse(day.isOpenAtNight(Time.fromString("23:00")));
    }

    @Test
    public void testNextOpen() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));
        assertEquals(Time.fromString("09:00"), day.nextOpen(Time.fromString("08:00")).get());
        assertEquals(Time.fromString("13:00"), day.nextOpen(Time.fromString("09:30")).get());
        assertFalse(day.nextOpen(Time.fromString("13:30")).isPresent());
    }

    @Test
    public void testNextClose() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));
        assertEquals(Time.fromString("12:00"), day.nextClose(Time.fromString("08:00")).get());
        assertEquals(Time.fromString("12:00"), day.nextClose(Time.fromString("10:00")).get());
        assertEquals(Time.fromString("18:00"), day.nextClose(Time.fromString("14:00")).get());
        assertFalse(day.nextClose(Time.fromString("18:00")).isPresent());
    }

    @Test
    public void testPreviousOpen() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));
        assertEquals(Time.fromString("13:00"), day.previousOpen(Time.fromString("15:00")).get());
        assertEquals(Time.fromString("09:00"), day.previousOpen(Time.fromString("12:30")).get());
        assertFalse(day.previousOpen(Time.fromString("08:00")).isPresent());
    }

    @Test
    public void testPreviousClose() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));
        assertEquals(Time.fromString("12:00"), day.previousClose(Time.fromString("15:00")).get());
        assertFalse(day.previousClose(Time.fromString("11:00")).isPresent());
    }

    @Test
    public void testMap() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));

        assertEquals(Arrays.asList(9, 13), day.map(range -> range.start().hours()));
        assertEquals(Arrays.asList("09:00-12:00", "13:00-18:00"), day.map(TimeRange::toString));
        assertTrue(OpeningHoursForDay.fromStrings(Collections.emptyList()).map(TimeRange::toString).isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(OpeningHoursForDay.fromStrings(Collections.emptyList()).isEmpty());
        assertFalse(OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00")).isEmpty());
    }

    @Test
    public void testIsIterable() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));

        List<String> ranges = new ArrayList<>();
        for (TimeRange range : day) {
            ranges.add(range.toString());
        }
        assertEquals(Arrays.asList("09:00-12:00", "13:00-18:00"), ranges);
    }

    @Test
    public void testNextOpenRangeAndCloseRange() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-24:00"));

        assertEquals("09:00-24:00", day.nextOpenRange(Time.fromString("08:00")).get().toString());
        assertFalse(day.nextOpenRange(Time.fromString("10:00")).isPresent());
        assertFalse(day.previousOpenRange(Time.fromString("10:00")).isPresent());
        assertEquals("09:00-24:00", day.nextCloseRange(Time.fromString("08:00")).get().toString());
        assertEquals("09:00-24:00", day.nextCloseRange(Time.fromString("10:00")).get().toString());
        assertFalse(day.previousCloseRange(Time.fromString("10:00")).isPresent());
    }

    @Test
    public void testPreviousRanges() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00", "13:00-18:00"));

        assertEquals("09:00-12:00", day.previousCloseRange(Time.fromString("13:00")).get().toString());
        assertEquals("13:00-18:00", day.previousOpenRange(Time.fromString("12:30")).get().toString());
        assertEquals(Arrays.asList("09:00-12:00"), day.forTime(Time.fromString("10:00")).stream()
                .map(TimeRange::toString).collect(java.util.stream.Collectors.toList()));
        assertTrue(day.forNightTime(Time.fromString("10:00")).isEmpty());
    }

    @Test(expected = io.github.wuwx.openinghours.exceptions.InvalidTimeRangeString.class)
    public void testMapWithoutHoursIsNotSilentlyIgnored() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("09:00-12:00", "not-a-range");
        OpeningHoursForDay.fromStrings(map);
    }

    @Test
    public void testTimeRangesAreReadOnly() {
        OpeningHoursForDay day = OpeningHoursForDay.fromStrings(Arrays.asList("09:00-12:00"));

        List<TimeRange> ranges = day.getTimeRanges();
        assertEquals(1, ranges.size());
        assertEquals("09:00-12:00", ranges.get(0).toString());

        try {
            ranges.add(TimeRange.fromString("13:00-14:00"));
            fail("Expected NonMutableOffsets");
        } catch (NonMutableOffsets expected) {
            assertTrue(expected.getMessage().contains("OpeningHoursForDay"));
        }

        Iterator<TimeRange> iterator = day.getTimeRanges().iterator();
        iterator.next();
        try {
            iterator.remove();
            fail("Expected NonMutableOffsets");
        } catch (NonMutableOffsets expected) {
            // read-only through the iterator as well
        }
    }
}
