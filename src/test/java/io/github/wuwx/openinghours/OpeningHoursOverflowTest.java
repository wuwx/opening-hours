package io.github.wuwx.openinghours;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;

public class OpeningHoursOverflowTest {

    @Test
    public void testFillsOpeningHoursWithOverflow() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("09:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("09:00-02:00", openingHours.forDay("monday").get(0).toString());
    }

    @Test
    public void testCheckOpenWithOverflow() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("09:00-02:00"));
        data.put("tuesday", Arrays.asList("19:00-04:00"));
        data.put("wednesday", Arrays.asList("09:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        // 2019-04-23 is a Tuesday
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 23, 1, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 23, 3, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 23, 18, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 23, 20, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 23, 23, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 24, 2, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 24, 3, 59)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 24, 4, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2019, 4, 24, 9, 0)));
    }

    @Test
    public void testNextCloseWithOverflow() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("09:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2019, 4, 23, 2, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 4, 23, 1, 0)));
    }

    @Test
    public void testNextCloseWithOverflowImmutability() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("09:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2019, 4, 23, 2, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 4, 23, 1, 0)));
        assertEquals(LocalDateTime.of(2019, 4, 23, 2, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 4, 22, 7, 0)));

        assertEquals(LocalDateTime.of(2019, 4, 29, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 4, 23, 1, 0)));
        assertEquals(LocalDateTime.of(2019, 4, 22, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 4, 22, 7, 0)));

        assertEquals(LocalDateTime.of(2019, 4, 22, 9, 0),
                openingHours.previousOpen(LocalDateTime.of(2019, 4, 22, 23, 30)));
        assertEquals(LocalDateTime.of(2019, 4, 22, 9, 0),
                openingHours.previousOpen(LocalDateTime.of(2019, 4, 23, 1, 0)));
        assertEquals(LocalDateTime.of(2019, 4, 22, 9, 0),
                openingHours.previousOpen(LocalDateTime.of(2019, 4, 23, 5, 0)));

        assertEquals(LocalDateTime.of(2019, 4, 22, 2, 0),
                openingHours.previousClose(LocalDateTime.of(2019, 4, 22, 23, 30)));
        assertEquals(LocalDateTime.of(2019, 4, 22, 2, 0),
                openingHours.previousClose(LocalDateTime.of(2019, 4, 23, 1, 0)));
        assertEquals(LocalDateTime.of(2019, 4, 22, 2, 0),
                openingHours.previousClose(LocalDateTime.of(2019, 4, 23, 5, 0)));
    }

    @Test
    public void testNextOpenStaysOnTheSameDayWhenStillClosed() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("09:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2019, 4, 22, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 4, 22, 7, 0)));
        assertEquals(LocalDateTime.of(2019, 4, 29, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 4, 22, 23, 0)));
    }

    @Test
    public void testPreviousOpenAndCloseWithOverflow() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("18:00-05:00"));
        data.put("tuesday", Arrays.asList("18:00-05:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        LocalDateTime tuesday = LocalDateTime.of(2024, 6, 11, 6, 0);
        assertEquals(LocalDateTime.of(2024, 6, 10, 18, 0), openingHours.previousOpen(tuesday));
        assertEquals(LocalDateTime.of(2024, 6, 11, 5, 0), openingHours.previousClose(tuesday));
    }

    @Test
    public void testOverflowOnSimpleRanges() {
        Map<String, Object> withOverflow = new LinkedHashMap<>();
        withOverflow.put("overflow", true);
        withOverflow.put("monday", Arrays.asList("11:00-18:00"));
        withOverflow.put("tuesday", Arrays.asList("13:37-15:37"));

        Map<String, Object> withoutOverflow = new LinkedHashMap<>(withOverflow);
        withoutOverflow.put("overflow", false);

        // 2019-06-04 is a Tuesday, 11:35
        LocalDateTime time = LocalDateTime.of(2019, 6, 4, 11, 35);
        assertFalse(OpeningHours.create(withOverflow).isOpenAt(time));
        assertFalse(OpeningHours.create(withoutOverflow).isOpenAt(time));
    }

    @Test
    public void testOverflowNextClose() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("18:00-05:00"));
        data.put("tuesday", Arrays.asList("17:00-06:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2024, 11, 12, 5, 0),
                openingHours.nextClose(LocalDateTime.of(2024, 11, 12, 4, 0)));
        assertEquals(LocalDateTime.of(2024, 11, 13, 6, 0),
                openingHours.nextClose(LocalDateTime.of(2024, 11, 12, 5, 30)));
    }

    @Test
    public void testOverflowNextCloseWithEarlyRange() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("18:00-05:00"));
        data.put("tuesday", Arrays.asList("05:40-05:50", "17:00-06:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2024, 11, 12, 5, 50),
                openingHours.nextClose(LocalDateTime.of(2024, 11, 12, 5, 30)));
    }

    @Test
    public void testOverflowNextCloseWithSplitRanges() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("18:00-22:00", "23:00-05:00"));
        data.put("tuesday", Arrays.asList("17:00-06:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2024, 11, 12, 5, 0),
                openingHours.nextClose(LocalDateTime.of(2024, 11, 11, 23, 30)));
        assertEquals(LocalDateTime.of(2024, 11, 12, 5, 0),
                openingHours.nextClose(LocalDateTime.of(2024, 11, 12, 4, 0)));
    }

    @Test
    public void testOverflowAcceptsTruthyValues() {
        Map<String, Object> truthyNumber = new LinkedHashMap<>();
        truthyNumber.put("overflow", 1);
        truthyNumber.put("friday", Arrays.asList("20:00-03:00"));
        assertTrue(OpeningHours.create(truthyNumber).isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));

        Map<String, Object> truthyString = new LinkedHashMap<>();
        truthyString.put("overflow", "yes");
        truthyString.put("friday", Arrays.asList("20:00-03:00"));
        assertTrue(OpeningHours.create(truthyString).isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));

        Map<String, Object> zeroString = new LinkedHashMap<>(truthyString);
        zeroString.put("overflow", "0");
        assertFalse(OpeningHours.create(zeroString).isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));

        Map<String, Object> zeroNumber = new LinkedHashMap<>(truthyString);
        zeroNumber.put("overflow", 0);
        assertFalse(OpeningHours.create(zeroNumber).isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));

        Map<String, Object> emptyString = new LinkedHashMap<>(truthyString);
        emptyString.put("overflow", "");
        assertFalse(OpeningHours.create(emptyString).isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));
    }

    @Test
    public void testNightRangeWithoutOverflow() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("22:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 23, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 1, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 20, 1, 0)));
    }

    @Test
    public void testOverflowForNightRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("overflow", true);
        data.put("friday", Arrays.asList("20:00-03:00"));
        data.put("saturday", Arrays.asList("20:00-03:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 21, 23, 0)));
        // Saturday 01:00 is open from Friday's night range
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 4, 0)));
    }

    @Test
    public void testOverflowDisabled() {
        Map<String, Object> data = new HashMap<>();
        data.put("friday", Arrays.asList("20:00-03:00"));
        data.put("saturday", Arrays.asList("20:00-03:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 21, 23, 0)));
        // Without overflow, Saturday 01:00 is only covered by Saturday's own ranges
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0)));
    }

    @Test
    public void testOverflowNextCloseAcrossDays() {
        Map<String, Object> data = new HashMap<>();
        data.put("overflow", true);
        data.put("friday", Arrays.asList("20:00-03:00"));
        data.put("saturday", Arrays.asList("20:00-03:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2023, 7, 22, 3, 0),
                openingHours.nextClose(LocalDateTime.of(2023, 7, 21, 21, 0)));
    }

    @Test
    public void testSpecialMidnightTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("wednesday", Arrays.asList("22:00-24:00"));
        data.put("thursday", Arrays.asList("00:00-07:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 5, 21, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 5, 23, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 6, 1, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 6, 8, 0)));
    }

    @Test
    public void testCurrentOpenRangeWithNightRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("22:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        // Tuesday 01:00 belongs to Monday's night range
        Optional<TimeRange> range = openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 20, 1, 0));
        assertTrue(range.isPresent());
        assertEquals("22:00", range.get().start().toString());

        assertEquals(LocalDateTime.of(2016, 12, 19, 22, 0),
                openingHours.currentOpenRangeStart(LocalDateTime.of(2016, 12, 20, 1, 0)).get());
        assertEquals(LocalDateTime.of(2016, 12, 20, 2, 0),
                openingHours.currentOpenRangeEnd(LocalDateTime.of(2016, 12, 20, 1, 0)).get());
    }

    @Test
    public void testForDateTimeWithNightRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("22:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(1, openingHours.forDateTime(LocalDateTime.of(2016, 12, 20, 1, 0)).size());
        assertTrue(openingHours.forDateTime(LocalDateTime.of(2016, 12, 20, 12, 0)).isEmpty());
    }
}
