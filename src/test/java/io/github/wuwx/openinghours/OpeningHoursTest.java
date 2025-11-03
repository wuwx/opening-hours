package io.github.wuwx.openinghours;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Unit tests for OpeningHours
 */
public class OpeningHoursTest {
    
    @Test
    public void testBasicOpeningHours() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00"));
        data.put("wednesday", Arrays.asList("09:00-12:00"));
        data.put("thursday", Arrays.asList("09:00-12:00"));
        data.put("friday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Collections.emptyList());
        data.put("sunday", Collections.emptyList());

        OpeningHours openingHours = OpeningHours.create(data);

        // Test isOpenOn/isClosedOn
        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("tuesday"));
        assertTrue(openingHours.isOpenOn("wednesday"));
        assertTrue(openingHours.isOpenOn("thursday"));
        assertTrue(openingHours.isOpenOn("friday"));
        assertFalse(openingHours.isOpenOn("saturday"));
        assertFalse(openingHours.isOpenOn("sunday"));

        assertFalse(openingHours.isClosedOn("monday"));
        assertFalse(openingHours.isClosedOn("tuesday"));
        assertFalse(openingHours.isClosedOn("wednesday"));
        assertFalse(openingHours.isClosedOn("thursday"));
        assertFalse(openingHours.isClosedOn("friday"));
        assertTrue(openingHours.isClosedOn("saturday"));
        assertTrue(openingHours.isClosedOn("sunday"));

        // Test isOpenAt/isClosedAt
        // 2023-07-21 is Friday, 2023-07-22 is Saturday
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 21, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 10, 0)));
        assertFalse(openingHours.isClosedAt(LocalDateTime.of(2023, 7, 21, 10, 0)));
        assertTrue(openingHours.isClosedAt(LocalDateTime.of(2023, 7, 22, 10, 0)));
    }
    
    @Test
    public void testMultipleTimeRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Test morning hours (open)
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 3, 10, 0))); // Monday 10:00
        // Test lunch break (closed)
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 3, 12, 30))); // Monday 12:30
        // Test afternoon hours (open)
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 3, 15, 0))); // Monday 15:00
        // Test after hours (closed)
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 3, 19, 0))); // Monday 19:00
    }
    
    @Test
    public void testForDay() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        OpeningHoursForDay monday = openingHours.forDay("monday");
        
        assertNotNull(monday);
        assertEquals(2, monday.size());
    }
    
    @Test
    public void testForWeek() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        Map<String, OpeningHoursForDay> week = openingHours.forWeek();
        
        assertNotNull(week);
        assertEquals(7, week.size()); // All 7 days should be present
        assertTrue(week.containsKey("monday"));
        assertTrue(week.containsKey("sunday"));
    }
    
    @Test
    public void testExceptions() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList()); // Christmas closed
        exceptions.put("2016-11-11", Arrays.asList("09:00-12:00")); // Special hours
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Christmas should be closed
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 25, 10, 0)));
        
        // Nov 11 should have special hours (closed in afternoon)
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 11, 11, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 11, 11, 15, 0)));
    }
    
    @Test
    public void testRecurringExceptions() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("01-01", Collections.emptyList()); // New Year's Day every year
        exceptions.put("12-25", Arrays.asList("09:00-12:00")); // Christmas every year
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Test New Year's Day in different years
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 1, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2021, 1, 1, 10, 0)));
        
        // Test Christmas in different years
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 15, 0)));
    }
    
    // ========== Data Association Tests ==========
    
    @Test
    public void testDataAssociation() {
        Map<String, Object> data = new HashMap<>();
        
        // Monday with data
        Map<String, Object> mondayData = new HashMap<>();
        mondayData.put("data", "Typical Monday");
        mondayData.put("hours", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("monday", mondayData);
        
        // Tuesday with mixed format - time ranges with individual data
        List<Object> tuesdayHours = new ArrayList<>();
        tuesdayHours.add("09:00-12:00");
        tuesdayHours.add("13:00-18:00");
        Map<String, Object> eveningSlot = new HashMap<>();
        eveningSlot.put("hours", "19:00-21:00");
        eveningSlot.put("data", "Extra on Tuesday evening");
        tuesdayHours.add(eveningSlot);
        data.put("tuesday", tuesdayHours);
        
        // Exception with data
        Map<String, Object> exceptions = new HashMap<>();
        Map<String, Object> christmasData = new HashMap<>();
        christmasData.put("data", "Closed for Christmas");
        christmasData.put("hours", Collections.emptyList());
        exceptions.put("2016-12-25", christmasData);
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Test day-level data
        assertEquals("Typical Monday", openingHours.forDay("monday").getData());
        
        // Test exception data
        assertEquals("Closed for Christmas", 
            openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0)).getData());
        
        // Test individual time range data
        assertEquals("Extra on Tuesday evening", 
            openingHours.forDay("tuesday").get(2).getData());
    }
    
    // ========== Day Range Tests ==========
    
    @Test
    public void testDayRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday to friday", Arrays.asList("09:00-17:00"));
        data.put("saturday to sunday", Collections.emptyList());
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // All weekdays should be open
        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("tuesday"));
        assertTrue(openingHours.isOpenOn("wednesday"));
        assertTrue(openingHours.isOpenOn("thursday"));
        assertTrue(openingHours.isOpenOn("friday"));
        
        // Weekends should be closed
        assertFalse(openingHours.isOpenOn("saturday"));
        assertFalse(openingHours.isOpenOn("sunday"));
    }
    
    @Test
    public void testExceptionDateRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        
        // Recurring date range (every year)
        Map<String, Object> holidaysData = new HashMap<>();
        holidaysData.put("hours", Collections.emptyList());
        holidaysData.put("data", "Holidays");
        exceptions.put("12-24 to 12-26", holidaysData);
        
        data.put("exceptions", exceptions);
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Test Christmas holidays are closed
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 24, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 26, 10, 0)));
        
        // Test data is preserved
        assertEquals("Holidays", 
            openingHours.forDate(LocalDateTime.of(2020, 12, 25, 0, 0)).getData());
    }
    
    // ========== Overflow Tests ==========
    
    @Test
    public void testOverflow() {
        Map<String, Object> data = new HashMap<>();
        data.put("overflow", true);
        data.put("friday", Arrays.asList("20:00-03:00"));
        data.put("saturday", Arrays.asList("20:00-03:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Friday night 23:00 should be open
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 21, 23, 0))); // Friday
        
        // Saturday morning 01:00 should be open (from Friday night)
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 1, 0))); // Saturday
        
        // Saturday morning 04:00 should be closed
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 22, 4, 0))); // Saturday
    }
    
    // ========== Special Time Tests ==========
    
    @Test
    public void testSpecialMidnightTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("wednesday", Arrays.asList("22:00-24:00"));
        data.put("thursday", Arrays.asList("00:00-07:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Wednesday 23:00 should be open
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 5, 23, 0))); // Wednesday
        
        // Thursday 01:00 should be open
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 7, 6, 1, 0))); // Thursday
    }
    
    // ========== Current State Tests ==========
    
    @Test
    public void testIsOpenClosed() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        data.put("wednesday", Arrays.asList("09:00-17:00"));
        data.put("thursday", Arrays.asList("09:00-17:00"));
        data.put("friday", Arrays.asList("09:00-17:00"));
        data.put("saturday", Collections.emptyList());
        data.put("sunday", Collections.emptyList());
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // isOpen() and isClosed() should work with current time
        // Note: This will depend on when the test runs, so we just test they don't throw
        boolean isOpen = openingHours.isOpen();
        boolean isClosed = openingHours.isClosed();
        
        // They should be opposites
        assertEquals(isOpen, !isClosed);
    }
    
    @Test
    public void testIsAlwaysOpenClosed() {
        // Test always closed
        Map<String, Object> closedData = new HashMap<>();
        closedData.put("monday", Collections.emptyList());
        closedData.put("tuesday", Collections.emptyList());
        closedData.put("wednesday", Collections.emptyList());
        closedData.put("thursday", Collections.emptyList());
        closedData.put("friday", Collections.emptyList());
        closedData.put("saturday", Collections.emptyList());
        closedData.put("sunday", Collections.emptyList());
        
        OpeningHours closedHours = OpeningHours.create(closedData);
        assertTrue(closedHours.isAlwaysClosed());
        assertFalse(closedHours.isAlwaysOpen());
        
        // Test always open (24/7)
        Map<String, Object> alwaysOpenData = new HashMap<>();
        alwaysOpenData.put("monday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("tuesday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("wednesday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("thursday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("friday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("saturday", Arrays.asList("00:00-24:00"));
        alwaysOpenData.put("sunday", Arrays.asList("00:00-24:00"));
        
        OpeningHours alwaysOpen = OpeningHours.create(alwaysOpenData);
        assertTrue(alwaysOpen.isAlwaysOpen());
        assertFalse(alwaysOpen.isAlwaysClosed());
    }
    
    // ========== Date String Support Tests ==========
    
    @Test
    public void testIsOpenOnDateString() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2020-09-03", Collections.emptyList()); // Thursday, closed
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Test with full date string
        assertFalse(openingHours.isOpenOn("2020-09-03"));
        
        // Test with MM-dd format (current year)
        // This would need current year context, skipping for now
    }
    
    // ========== Next/Previous Open/Close Tests ==========
    
    @Test
    public void testNextOpen() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("wednesday", Collections.emptyList());
        data.put("thursday", Collections.emptyList());
        data.put("friday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("sunday", Collections.emptyList());
        
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        LocalDateTime result1 = openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 11, 0));
        assertEquals(LocalDateTime.of(2016, 12, 24, 13, 0), result1);
        
        LocalDateTime result2 = openingHours.nextOpen(LocalDateTime.of(2016, 12, 25, 10, 0));
        assertEquals(LocalDateTime.of(2016, 12, 26, 9, 0), result2);
    }
    
    @Test
    public void testNextClose() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        LocalDateTime result1 = openingHours.nextClose(LocalDateTime.of(2016, 12, 24, 10, 0));
        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0), result1);
        
        LocalDateTime result2 = openingHours.nextClose(LocalDateTime.of(2016, 12, 25, 15, 0));
        assertEquals(LocalDateTime.of(2016, 12, 26, 12, 0), result2);
    }
    
    @Test
    public void testPreviousOpen() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        LocalDateTime result = openingHours.previousOpen(LocalDateTime.of(2016, 12, 24, 15, 0));
        assertEquals(LocalDateTime.of(2016, 12, 24, 13, 0), result);
    }
    
    @Test
    public void testPreviousClose() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        LocalDateTime result = openingHours.previousClose(LocalDateTime.of(2016, 12, 24, 15, 0));
        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0), result);
    }
    
    // ========== Current Open Range Tests ==========
    
    @Test
    public void testCurrentOpenRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        Optional<TimeRange> range1 = openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 24, 10, 0));
        assertTrue(range1.isPresent());
        assertEquals("09:00", range1.get().start().toString());
        assertEquals("12:00", range1.get().end().toString());
        
        Optional<TimeRange> range2 = openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 24, 12, 30));
        assertFalse(range2.isPresent());
    }
    
    @Test
    public void testCurrentOpenRangeStart() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        Optional<LocalDateTime> start = openingHours.currentOpenRangeStart(
            LocalDateTime.of(2016, 12, 24, 10, 0));
        assertTrue(start.isPresent());
        assertEquals(LocalDateTime.of(2016, 12, 24, 9, 0), start.get());
        
        Optional<LocalDateTime> start2 = openingHours.currentOpenRangeStart(
            LocalDateTime.of(2016, 12, 24, 12, 30));
        assertFalse(start2.isPresent());
    }
    
    @Test
    public void testCurrentOpenRangeEnd() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        Optional<LocalDateTime> end = openingHours.currentOpenRangeEnd(
            LocalDateTime.of(2016, 12, 24, 10, 0));
        assertTrue(end.isPresent());
        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0), end.get());
        
        Optional<LocalDateTime> end2 = openingHours.currentOpenRangeEnd(
            LocalDateTime.of(2016, 12, 24, 12, 30));
        assertFalse(end2.isPresent());
    }
    
    // ========== Diff Calculation Tests ==========
    
    @Test
    public void testDiffInOpenHours() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        double hours = openingHours.diffInOpenHours(
            LocalDateTime.of(2016, 12, 24, 9, 0),
            LocalDateTime.of(2016, 12, 24, 16, 0)
        );
        assertEquals(6.0, hours, 0.01);
    }
    
    @Test
    public void testDiffInOpenMinutes() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Arrays.asList("09:00-12:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        double minutes = openingHours.diffInOpenMinutes(
            LocalDateTime.of(2016, 12, 24, 9, 0),
            LocalDateTime.of(2016, 12, 24, 10, 30)
        );
        assertEquals(90.0, minutes, 0.01);
    }
    
    @Test
    public void testDiffInOpenSeconds() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Arrays.asList("09:00-12:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        double seconds = openingHours.diffInOpenSeconds(
            LocalDateTime.of(2016, 12, 24, 9, 0),
            LocalDateTime.of(2016, 12, 24, 9, 1)
        );
        assertEquals(60.0, seconds, 0.01);
    }
    
    @Test
    public void testDiffInClosedHours() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        double hours = openingHours.diffInClosedHours(
            LocalDateTime.of(2016, 12, 24, 11, 0),
            LocalDateTime.of(2016, 12, 24, 14, 0)
        );
        assertEquals(1.0, hours, 0.01);
    }
    
    // ========== Filters Tests ==========
    
    @Test
    public void testFilters() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        
        List<java.util.function.Function<java.time.LocalDate, Map<String, Object>>> filters = new ArrayList<>();
        filters.add(date -> {
            if (date.equals(java.time.LocalDate.of(2020, 1, 1))) {
                return Collections.emptyMap();
            }
            return null;
        });
        
        data.put("filters", filters);
        OpeningHours openingHours = OpeningHours.create(data);
        
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 6, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 1, 10, 0)));
    }
    
    // ========== Merge Overlapping Ranges Tests ==========
    
    @Test
    public void testMergeOverlappingRanges() {
        Map<String, List<String>> ranges = new HashMap<>();
        ranges.put("monday", Arrays.asList("08:00-11:00", "10:00-12:00"));
        
        Map<String, List<String>> merged = OpeningHours.mergeOverlappingRanges(ranges);
        
        assertEquals(1, merged.get("monday").size());
        assertEquals("08:00-12:00", merged.get("monday").get(0));
    }
    
    @Test
    public void testCreateAndMergeOverlappingRanges() {
        Map<String, List<String>> ranges = new HashMap<>();
        ranges.put("monday", Arrays.asList("08:00-11:00", "10:00-12:00", "13:00-17:00"));
        
        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(ranges);
        
        OpeningHoursForDay monday = openingHours.forDay("monday");
        assertEquals(2, monday.size());
    }
    
    // ========== Week Combined Tests ==========
    
    @Test
    public void testForWeekCombined() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        data.put("wednesday", Arrays.asList("09:00-17:00"));
        data.put("thursday", Arrays.asList("09:00-12:00"));
        data.put("friday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Collections.emptyList());
        data.put("sunday", Collections.emptyList());
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        Map<String, Object> combined = openingHours.forWeekCombined();
        
        assertNotNull(combined);
        assertTrue(combined.size() >= 2);
    }
    
    @Test
    public void testForWeekConsecutiveDays() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        data.put("wednesday", Arrays.asList("09:00-17:00"));
        data.put("thursday", Arrays.asList("09:00-12:00"));
        data.put("friday", Arrays.asList("09:00-12:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        Map<String, Object> consecutive = openingHours.forWeekConsecutiveDays();
        
        assertNotNull(consecutive);
        assertTrue(consecutive.size() >= 2);
    }
    
    // ========== Fill Method Test ==========
    
    @Test
    public void testFillMethod() {
        // Create an initial instance
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("wednesday", Arrays.asList("09:00-17:00"));
        OpeningHours openingHours = OpeningHours.create(initialData);
        
        // Fill with new data
        Map<String, Object> newData = new HashMap<>();
        newData.put("monday", Arrays.asList("09:00-17:00"));
        newData.put("tuesday", Arrays.asList("09:00-17:00"));
        
        OpeningHours filled = openingHours.fill(newData);
        
        assertNotNull(filled);
        assertTrue(filled.isOpenOn("monday"));
        assertTrue(filled.isOpenOn("tuesday"));
        // Original instance should be unchanged (immutable)
        assertTrue(openingHours.isOpenOn("wednesday"));
    }
    
    // ========== Additional Diff Tests ==========
    
    @Test
    public void testDiffInClosedMinutes() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // From 11:00 to 14:00 = 60 minutes closed (12:00-13:00 lunch break)
        double minutes = openingHours.diffInClosedMinutes(
            LocalDateTime.of(2016, 12, 24, 11, 0),
            LocalDateTime.of(2016, 12, 24, 14, 0)
        );
        assertEquals(60.0, minutes, 0.01);
    }
    
    @Test
    public void testDiffInClosedSeconds() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // From 12:00 to 12:01 = 60 seconds closed (in lunch break)
        double seconds = openingHours.diffInClosedSeconds(
            LocalDateTime.of(2016, 12, 24, 12, 0),
            LocalDateTime.of(2016, 12, 24, 12, 1)
        );
        assertEquals(60.0, seconds, 0.01);
    }
    
    // ========== Search Limit Tests ==========
    
    @Test
    public void testNextOpenWithCap() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Collections.emptyList());
        data.put("tuesday", Collections.emptyList());
        data.put("wednesday", Collections.emptyList());
        data.put("thursday", Collections.emptyList());
        data.put("friday", Collections.emptyList());
        data.put("saturday", Collections.emptyList());
        data.put("sunday", Collections.emptyList());
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Always closed - should return cap
        LocalDateTime cap = LocalDateTime.of(2020, 12, 31, 23, 59);
        LocalDateTime result = openingHours.nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0), null, cap);
        assertEquals(cap, result);
    }
    
    @Test(expected = io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded.class)
    public void testNextOpenMaximumLimitExceeded() {
        Map<String, Object> data = new HashMap<>();
        // All days closed
        for (String day : Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) {
            data.put(day, Collections.emptyList());
        }
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Should throw MaximumLimitExceeded because never opens
        openingHours.nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0));
    }
    
    @Test(expected = io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded.class)
    public void testNextOpenWithSearchUntilExceeded() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        
        Map<String, Object> exceptions = new HashMap<>();
        // Closed for a week (specific dates)
        Map<String, Object> closedData = new HashMap<>();
        closedData.put("hours", Collections.emptyList());
        exceptions.put("2020-01-06 to 2020-01-10", closedData);
        data.put("exceptions", exceptions);
        
        OpeningHours openingHours = OpeningHours.create(data);
        
        // Should throw because next open (2020-01-13) is after searchUntil
        LocalDateTime searchUntil = LocalDateTime.of(2020, 1, 8, 0, 0);
        openingHours.nextOpen(LocalDateTime.of(2020, 1, 6, 10, 0), searchUntil, null);
    }
}
