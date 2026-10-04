package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidTimezone;
import io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded;
import io.github.wuwx.openinghours.exceptions.SearchLimitReached;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static io.github.wuwx.openinghours.TestSchedules.typicalWeek;
import static io.github.wuwx.openinghours.TestSchedules.week;
import static org.junit.Assert.*;

public class OpeningHoursTest {

    @Test
    public void testOpeningHoursForARegularWeek() {
        OpeningHours openingHours = OpeningHours.create(typicalWeek());

        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("saturday"));
        assertFalse(openingHours.isOpenOn("sunday"));
        assertFalse(openingHours.isClosedOn("monday"));
        assertTrue(openingHours.isClosedOn("sunday"));

        // 2016-12-23 is Friday, 2016-12-24 is Saturday, 2016-12-25 is Sunday
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 23, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 25, 10, 0)));
        assertFalse(openingHours.isClosedAt(LocalDateTime.of(2016, 12, 23, 10, 0)));
        assertTrue(openingHours.isClosedAt(LocalDateTime.of(2016, 12, 25, 10, 0)));
    }

    @Test
    public void testMultipleTimeRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 12, 30)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 15, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 18, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 19, 0)));
    }

    @Test
    public void testReportsA2400CloseOnTheNextMidnight() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-24:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 8, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 9, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 19, 23, 59)));
        assertEquals(LocalDateTime.of(2016, 12, 20, 0, 0),
                openingHours.nextClose(LocalDateTime.of(2016, 12, 19, 10, 0)));
    }

    @Test
    public void testRegularWeekDayAndWeek() {
        OpeningHours openingHours = OpeningHours.create(typicalWeek());

        // 2016-12-19 is a Monday
        assertEquals("09:00-12:00,13:00-18:00", openingHours.forDay("monday").toString());
        assertEquals("09:00-12:00,13:00-18:00", openingHours.forDay(DayOfWeek.MONDAY).toString());

        Map<String, OpeningHoursForDay> week = openingHours.forWeek();
        assertEquals(7, week.size());
        assertEquals(Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"),
                new ArrayList<>(week.keySet()));
    }

    @Test
    public void testForWeekCombined() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        data.put("wednesday", Arrays.asList("09:00-17:00"));
        data.put("thursday", Arrays.asList("09:00-12:00"));
        data.put("friday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Collections.emptyList());
        data.put("sunday", Collections.emptyList());

        Map<String, Object> combined = OpeningHours.create(data).forWeekCombined();

        assertEquals(3, combined.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> monday = (Map<String, Object>) combined.get("monday");
        assertEquals(Arrays.asList("monday", "tuesday", "wednesday"), monday.get("days"));
        assertEquals("09:00-17:00", ((OpeningHoursForDay) monday.get("opening_hours")).toString());
    }

    @Test
    public void testForWeekConsecutiveDays() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        data.put("wednesday", Arrays.asList("09:00-17:00"));
        data.put("thursday", Arrays.asList("09:00-12:00"));
        data.put("friday", Arrays.asList("09:00-12:00"));

        Map<String, Object> consecutive = OpeningHours.create(data).forWeekConsecutiveDays();

        assertEquals(3, consecutive.size());
        assertTrue(consecutive.containsKey("monday"));
        assertTrue(consecutive.containsKey("thursday"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thursday = (Map<String, Object>) consecutive.get("thursday");
        assertEquals(Arrays.asList("thursday", "friday"), thursday.get("days"));
        assertEquals("09:00-12:00", ((OpeningHoursForDay) thursday.get("opening_hours")).toString());
    }

    @Test
    public void testForDate() {
        Map<String, Object> data = typicalWeek();
        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        exceptions.put("2016-11-11", Arrays.asList("09:00-12:00"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(2, openingHours.forDate(LocalDateTime.of(2016, 12, 23, 0, 0)).size());
        assertTrue(openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0)).isEmpty());
        assertEquals(1, openingHours.forDate(LocalDateTime.of(2016, 11, 11, 0, 0)).size());
    }

    @Test
    public void testExceptions() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        exceptions.put("2016-11-11", Arrays.asList("09:00-12:00"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 25, 10, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 11, 11, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2016, 11, 11, 15, 0)));
        assertEquals(2, openingHours.exceptions().size());
    }

    @Test
    public void testRecurringExceptions() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("01-01", Collections.emptyList());
        exceptions.put("12-25", Arrays.asList("09:00-12:00"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 1, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2021, 1, 1, 10, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 15, 0)));
    }

    @Test
    public void testPrioritizesFullDatesOverRecurringDates() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-12-25", Arrays.asList("09:00-12:00"));
        exceptions.put("12-25", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        // The full date wins over the recurring one
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2016, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2017, 12, 25, 10, 0)));
    }

    @Test
    public void testExceptionDateRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> holidaysData = new LinkedHashMap<>();
        holidaysData.put("hours", Collections.emptyList());
        holidaysData.put("data", "Holidays");
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("12-24 to 12-26", holidaysData);
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 24, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 12, 26, 10, 0)));
        assertEquals("Holidays", openingHours.forDate(LocalDateTime.of(2020, 12, 25, 0, 0)).getData());
    }

    @Test
    public void testExceptionDateRangeWithFullYear() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2020-01-06 to 2020-01-08", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 6, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 8, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 9, 10, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2020, 1, 13, 10, 0)));
    }

    @Test
    public void testLeapYearRecurringDateRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday to friday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("02-28 to 03-01", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2024, 2, 29, 10, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2024, 2, 27, 10, 0)));
    }

    @Test
    public void testDayRanges() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday to friday", Arrays.asList("09:00-17:00"));
        data.put("saturday to sunday", Collections.emptyList());

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("friday"));
        assertFalse(openingHours.isOpenOn("saturday"));
        assertFalse(openingHours.isOpenOn("sunday"));
    }

    @Test
    public void testDayRangeWithDash() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday-wednesday", Arrays.asList("09:00-17:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("tuesday"));
        assertTrue(openingHours.isOpenOn("wednesday"));
        assertFalse(openingHours.isOpenOn("thursday"));
    }

    @Test
    public void testIsOpenOnDateString() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2020-09-03", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertFalse(openingHours.isOpenOn("2020-09-03"));
        assertTrue(openingHours.isOpenOn("2020-09-07"));
        assertFalse(openingHours.isOpenOn("09-03"));
        assertTrue(openingHours.isOpenOn("09-07"));
    }

    @Test
    public void testNextOpen() {
        Map<String, Object> data = typicalWeek();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2016, 12, 24, 13, 0),
                openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 11, 0)));
        assertEquals(LocalDateTime.of(2016, 12, 26, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2016, 12, 25, 10, 0)));
        assertEquals(LocalDateTime.of(2016, 12, 26, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 20, 0)));
    }

    @Test
    public void testNextClose() {
        Map<String, Object> data = typicalWeek();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0),
                openingHours.nextClose(LocalDateTime.of(2016, 12, 24, 10, 0)));
        assertEquals(LocalDateTime.of(2016, 12, 26, 12, 0),
                openingHours.nextClose(LocalDateTime.of(2016, 12, 25, 15, 0)));
    }

    @Test
    public void testPreviousOpen() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2016, 12, 24, 13, 0),
                openingHours.previousOpen(LocalDateTime.of(2016, 12, 24, 15, 0)));
        assertEquals(LocalDateTime.of(2016, 12, 24, 9, 0),
                openingHours.previousOpen(LocalDateTime.of(2016, 12, 24, 12, 30)));
    }

    @Test
    public void testPreviousClose() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0),
                openingHours.previousClose(LocalDateTime.of(2016, 12, 24, 15, 0)));
        // 2016-12-20 is Tuesday, previous close is the end of Monday's last range
        assertEquals(LocalDateTime.of(2016, 12, 19, 18, 0),
                openingHours.previousClose(LocalDateTime.of(2016, 12, 20, 10, 0)));
    }

    @Test
    public void testExceptionsAndFiltersAreReturnedAsCopies() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));

        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2020-12-25", Collections.emptyList());
        data.put("exceptions", exceptions);
        data.put("filters", Collections.singletonList((Function<LocalDateTime, Object>) date -> null));

        OpeningHours openingHours = OpeningHours.create(data);

        // the returned collections can be modified without altering the instance, as PHP arrays allow
        Map<String, OpeningHoursForDay> returnedExceptions = openingHours.exceptions();
        returnedExceptions.clear();
        assertEquals(1, openingHours.exceptions().size());

        List<?> returnedFilters = openingHours.getFilters();
        returnedFilters.clear();
        assertEquals(1, openingHours.getFilters().size());
    }

    @Test
    public void testConsecutiveOpenHours() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-24:00"));
        data.put("tuesday", Arrays.asList("00:00-24:00"));
        data.put("wednesday", Arrays.asList("00:00-03:00", "09:00-24:00"));
        data.put("friday", Arrays.asList("00:00-03:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        LocalDateTime monday = LocalDateTime.of(2019, 2, 4, 11, 0);
        assertTrue(openingHours.isOpenAt(monday));
        assertFalse(openingHours.isClosedAt(monday));
        assertEquals(LocalDateTime.of(2019, 2, 6, 3, 0), openingHours.nextClose(monday));
        assertEquals(LocalDateTime.of(2019, 2, 6, 9, 0), openingHours.nextOpen(monday));
        assertEquals(LocalDateTime.of(2019, 2, 1, 3, 0), openingHours.previousClose(monday));
        assertEquals(LocalDateTime.of(2019, 2, 4, 9, 0), openingHours.previousOpen(monday));
        assertEquals(LocalDateTime.of(2019, 2, 1, 0, 0),
                openingHours.previousOpen(LocalDateTime.of(2019, 2, 4, 8, 50)));

        LocalDateTime wednesday = LocalDateTime.of(2019, 2, 6, 9, 0);
        assertTrue(openingHours.isOpenAt(wednesday));
        assertEquals(LocalDateTime.of(2019, 2, 7, 0, 0), openingHours.nextClose(wednesday));
        assertEquals(LocalDateTime.of(2019, 2, 8, 0, 0), openingHours.nextOpen(wednesday));
        assertEquals(LocalDateTime.of(2019, 2, 6, 3, 0), openingHours.previousClose(wednesday));
        assertEquals(LocalDateTime.of(2019, 2, 4, 9, 0), openingHours.previousOpen(wednesday));

        LocalDateTime friday = LocalDateTime.of(2019, 2, 8, 9, 0);
        assertEquals(LocalDateTime.of(2019, 2, 8, 3, 0), openingHours.previousClose(friday));
    }

    @Test
    public void testNextOpenFromMixedStructures() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> firstRange = new LinkedHashMap<>();
        firstRange.put("hours", "09:00-11:00");
        firstRange.put("data", Collections.singletonList("foobar"));
        data.put("monday", TestSchedules.list(firstRange, "13:00-19:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2019, 2, 11, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 2, 11, 0, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 11, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 2, 11, 0, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 13, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 2, 11, 9, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 11, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 2, 11, 9, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 13, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 2, 11, 11, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 19, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 2, 11, 11, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 18, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 2, 11, 13, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 11, 19, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 2, 11, 13, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 18, 9, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 2, 11, 19, 0)));
        assertEquals(LocalDateTime.of(2019, 2, 18, 11, 0),
                openingHours.nextClose(LocalDateTime.of(2019, 2, 11, 19, 0)));
    }

    @Test
    public void testCurrentOpenRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        Optional<TimeRange> range = openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 24, 10, 0));
        assertTrue(range.isPresent());
        assertEquals("09:00", range.get().start().toString());
        assertEquals("12:00", range.get().end().toString());
        assertFalse(openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 24, 12, 30)).isPresent());

        assertEquals(LocalDateTime.of(2016, 12, 24, 9, 0),
                openingHours.currentOpenRangeStart(LocalDateTime.of(2016, 12, 24, 10, 0)).get());
        assertEquals(LocalDateTime.of(2016, 12, 24, 12, 0),
                openingHours.currentOpenRangeEnd(LocalDateTime.of(2016, 12, 24, 10, 0)).get());
        assertFalse(openingHours.currentOpenRangeStart(LocalDateTime.of(2016, 12, 24, 12, 30)).isPresent());
        assertFalse(openingHours.currentOpenRangeEnd(LocalDateTime.of(2016, 12, 24, 12, 30)).isPresent());
    }

    @Test
    public void testCurrentOpenRangeEndAtMidnight() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-24:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(LocalDateTime.of(2016, 12, 20, 0, 0),
                openingHours.currentOpenRangeEnd(LocalDateTime.of(2016, 12, 19, 10, 0)).get());
    }

    @Test
    public void testDiffInOpenHours() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(6.0, openingHours.diffInOpenHours(
                LocalDateTime.of(2016, 12, 24, 9, 0),
                LocalDateTime.of(2016, 12, 24, 16, 0)), 0.001);
        assertEquals(90.0, openingHours.diffInOpenMinutes(
                LocalDateTime.of(2016, 12, 24, 9, 0),
                LocalDateTime.of(2016, 12, 24, 10, 30)), 0.001);
        assertEquals(60.0, openingHours.diffInOpenSeconds(
                LocalDateTime.of(2016, 12, 24, 9, 0),
                LocalDateTime.of(2016, 12, 24, 9, 1)), 0.001);
    }

    @Test
    public void testDiffInClosed() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(1.0, openingHours.diffInClosedHours(
                LocalDateTime.of(2016, 12, 24, 11, 0),
                LocalDateTime.of(2016, 12, 24, 14, 0)), 0.001);
        assertEquals(60.0, openingHours.diffInClosedMinutes(
                LocalDateTime.of(2016, 12, 24, 11, 0),
                LocalDateTime.of(2016, 12, 24, 14, 0)), 0.001);
        assertEquals(60.0, openingHours.diffInClosedSeconds(
                LocalDateTime.of(2016, 12, 24, 12, 0),
                LocalDateTime.of(2016, 12, 24, 12, 1)), 0.001);
    }

    @Test
    public void testDiffReversedRange() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("saturday", Arrays.asList("09:00-12:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(-3.0, openingHours.diffInOpenHours(
                LocalDateTime.of(2016, 12, 24, 12, 0),
                LocalDateTime.of(2016, 12, 24, 9, 0)), 0.001);
    }

    @Test
    public void testDiffAcrossDays() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        // Monday 2016-12-19 10:00 to Tuesday 2016-12-20 10:00:
        // open Monday 10:00-12:00 (2h) + Tuesday 09:00-10:00 (1h)
        assertEquals(3.0, openingHours.diffInOpenHours(
                LocalDateTime.of(2016, 12, 19, 10, 0),
                LocalDateTime.of(2016, 12, 20, 10, 0)), 0.001);
    }

    @Test
    public void testIsOpenNow() {
        assertTrue(OpeningHours.create(week("00:00-24:00")).isOpen());
    }

    @Test
    public void testIsClosedNow() {
        assertTrue(OpeningHours.create(week(null)).isClosed());
    }

    @Test
    public void testIsAlwaysOpenClosed() {
        OpeningHours closedHours = OpeningHours.create(week(null));
        assertTrue(closedHours.isAlwaysClosed());
        assertFalse(closedHours.isAlwaysOpen());

        OpeningHours alwaysOpen = OpeningHours.create(week("00:00-24:00"));
        assertTrue(alwaysOpen.isAlwaysOpen());
        assertFalse(alwaysOpen.isAlwaysClosed());
    }

    @Test
    public void testIsAlwaysOpenWithAlwaysOpenExceptions() {
        Map<String, Object> data = week("00:00-24:00");
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Arrays.asList("00:00-24:00"));
        data.put("exceptions", exceptions);

        assertTrue(OpeningHours.create(data).isAlwaysOpen());
    }

    @Test
    public void testRetrievesRegularClosingDaysAsStrings() {
        assertEquals(Collections.singletonList("sunday"),
                OpeningHours.create(typicalWeek()).regularClosingDays());
    }

    @Test
    public void testRetrievesRegularClosingDaysAsIsoNumbers() {
        assertEquals(Collections.singletonList(7),
                OpeningHours.create(typicalWeek()).regularClosingDaysISO());
    }

    @Test
    public void testRetrievesExceptionalClosingDates() {
        Map<String, Object> data = typicalWeek();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        exceptions.put("2016-11-11", Arrays.asList("09:00-12:00"));
        data.put("exceptions", exceptions);

        assertEquals(Collections.singletonList(LocalDate.of(2016, 12, 25)),
                OpeningHours.create(data).exceptionalClosingDates());
    }

    @Test
    public void testValidatesTheOpeningHours() {
        Map<String, Object> valid = new HashMap<>();
        valid.put("monday", Arrays.asList("09:00-18:00"));
        assertTrue(OpeningHours.isValid(valid));

        Map<String, Object> invalidDate = new HashMap<>();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("01-99", Collections.emptyList());
        invalidDate.put("exceptions", exceptions);
        assertFalse(OpeningHours.isValid(invalidDate));

        Map<String, Object> overlapping = new HashMap<>();
        overlapping.put("monday", Arrays.asList("09:00-12:00", "11:00-14:00"));
        assertFalse(OpeningHours.isValid(overlapping));
    }

    @Test
    public void testStopsAtCapLimitWithNextOpen() {
        OpeningHours openingHours = OpeningHours.create(week(null));

        LocalDateTime cap = LocalDateTime.of(2020, 1, 5, 0, 0);
        assertEquals(cap, openingHours.nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0), null, cap));
    }

    @Test
    public void testStopsAtCapLimitWithNextClose() {
        OpeningHours openingHours = OpeningHours.create(week(null));

        LocalDateTime cap = LocalDateTime.of(2020, 1, 5, 0, 0);
        assertEquals(cap, openingHours.nextClose(LocalDateTime.of(2020, 1, 1, 10, 0), null, cap));
    }

    @Test
    public void testStopsAtCapLimitWithPreviousOpen() {
        OpeningHours openingHours = OpeningHours.create(week(null));

        LocalDateTime cap = LocalDateTime.of(2019, 12, 28, 0, 0);
        assertEquals(cap, openingHours.previousOpen(LocalDateTime.of(2020, 1, 1, 10, 0), null, cap));
    }

    @Test
    public void testStopsAtCapLimitWithPreviousClose() {
        OpeningHours openingHours = OpeningHours.create(week(null));

        LocalDateTime cap = LocalDateTime.of(2019, 12, 28, 0, 0);
        assertEquals(cap, openingHours.previousClose(LocalDateTime.of(2020, 1, 1, 10, 0), null, cap));
    }

    @Test(expected = MaximumLimitExceeded.class)
    public void testThrowsOnLimitExceededWithNextOpen() {
        OpeningHours.create(week(null)).nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test(expected = MaximumLimitExceeded.class)
    public void testThrowsOnLimitExceededWithNextClose() {
        OpeningHours.create(week(null)).nextClose(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test(expected = MaximumLimitExceeded.class)
    public void testThrowsOnLimitExceededWithPreviousOpen() {
        OpeningHours.create(week(null)).previousOpen(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test(expected = MaximumLimitExceeded.class)
    public void testThrowsOnLimitExceededWithPreviousClose() {
        OpeningHours.create(week(null)).previousClose(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test
    public void testHandlesVeryFarExceptionByChangingLimit() {
        Map<String, Object> data = week(null);
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2021-01-04", Arrays.asList("10:00-12:00"));
        data.put("exceptions", exceptions);

        // The exception is 369 days away, which is beyond the default 366 days limit
        OpeningHours openingHours = OpeningHours.create(data).setDayLimit(800);

        assertEquals(LocalDateTime.of(2021, 1, 4, 10, 0),
                openingHours.nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0)));
    }

    @Test(expected = MaximumLimitExceeded.class)
    public void testThrowsOnFarExceptionWithoutChangingLimit() {
        Map<String, Object> data = week(null);
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2021-01-04", Arrays.asList("10:00-12:00"));
        data.put("exceptions", exceptions);

        OpeningHours.create(data).nextOpen(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test(expected = SearchLimitReached.class)
    public void testThrowsOnSearchLimitExceededWithNextOpen() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("2020-01-06 to 2020-01-10", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours.create(data).nextOpen(
                LocalDateTime.of(2020, 1, 6, 10, 0),
                LocalDateTime.of(2020, 1, 8, 0, 0),
                null);
    }

    @Test(expected = SearchLimitReached.class)
    public void testThrowsOnSearchLimitExceededWithNextClose() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        // 2016-12-25 is a Sunday
        OpeningHours.create(data).nextClose(
                LocalDateTime.of(2016, 12, 25, 10, 0),
                LocalDateTime.of(2016, 12, 25, 23, 0),
                null);
    }

    @Test(expected = SearchLimitReached.class)
    public void testThrowsOnSearchLimitExceededWithPreviousOpen() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        OpeningHours.create(data).previousOpen(
                LocalDateTime.of(2016, 12, 25, 10, 0),
                LocalDateTime.of(2016, 12, 25, 0, 0),
                null);
    }

    @Test(expected = SearchLimitReached.class)
    public void testThrowsOnSearchLimitExceededWithPreviousClose() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        OpeningHours.create(data).previousClose(
                LocalDateTime.of(2016, 12, 26, 10, 0),
                LocalDateTime.of(2016, 12, 26, 0, 0),
                null);
    }

    @Test
    public void testSetsTimezoneOnConstruct() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));

        assertNotNull(OpeningHours.create(data, java.time.ZoneId.of("Europe/Oslo")));
        assertNotNull(OpeningHours.create(data, java.time.ZoneId.of("Europe/Oslo"),
                java.time.ZoneId.of("America/New_York")));
    }

    @Test
    public void testSetsTimezoneFromData() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        Map<String, String> timezone = new HashMap<>();
        timezone.put("input", "America/New_York");
        timezone.put("output", "Europe/Oslo");
        data.put("timezone", timezone);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(java.time.ZoneId.of("America/New_York"), openingHours.getTimezone());
        assertEquals(java.time.ZoneId.of("Europe/Oslo"), openingHours.getOutputTimezone());
    }

    @Test(expected = InvalidTimezone.class)
    public void testThrowsOnInvalidTimezone() {
        Map<String, Object> data = new HashMap<>();
        data.put("timezone", "Mars/Olympus_Mons");
        OpeningHours.create(data);
    }

    @Test
    public void testHandlesTimezoneForDateString() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        data.put("timezone", "Europe/Oslo");

        OpeningHours openingHours = OpeningHours.create(data);

        // 2020-09-03 is a Thursday, 2020-09-07 a Monday
        assertFalse(openingHours.isOpenOn("2020-09-03"));
        assertTrue(openingHours.isOpenOn("2020-09-07"));
    }
}
