package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidDate;
import io.github.wuwx.openinghours.exceptions.InvalidDateRange;
import io.github.wuwx.openinghours.exceptions.InvalidDateTimeClass;
import io.github.wuwx.openinghours.exceptions.InvalidDayName;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.wuwx.openinghours.TestSchedules.hoursMap;
import static io.github.wuwx.openinghours.TestSchedules.list;
import static io.github.wuwx.openinghours.TestSchedules.typicalWeek;
import static org.junit.Assert.*;

// PHP keeps the "Fill" name for historical reasons; there is no fill() method upstream either

public class OpeningHoursFillTest {

    @Test
    public void testFillsOpeningHours() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-18:00"));
        data.put("wednesday", Arrays.asList("09:00-12:00", "14:00-18:00"));
        data.put("thursday", Collections.emptyList());
        data.put("friday", Arrays.asList("09:00-20:00"));
        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-09-26", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("09:00-18:00", openingHours.forDay("monday").get(0).toString());
        assertEquals("09:00-18:00", openingHours.forDay("tuesday").get(0).toString());
        assertEquals("09:00-12:00", openingHours.forDay("wednesday").get(0).toString());
        assertEquals("14:00-18:00", openingHours.forDay("wednesday").get(1).toString());
        assertTrue(openingHours.forDay("thursday").isEmpty());
        assertEquals("09:00-20:00", openingHours.forDay("friday").get(0).toString());
        assertTrue(openingHours.forDate(LocalDateTime.of(2016, 9, 26, 11, 0)).isEmpty());
    }

    @Test
    public void testMergeRangesWithExcludedKeys() {
        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList("08:00-11:00", "10:00-12:00"));
        ranges.put("custom", Arrays.asList("08:00-10:00", "09:00-11:00"));

        Map<String, Object> merged = OpeningHours.mergeOverlappingRanges(ranges);
        assertEquals(Collections.singletonList("08:00-11:00"), merged.get("custom"));

        Map<String, Object> withExclusions = OpeningHours.mergeOverlappingRanges(
                ranges, true, Arrays.asList("data", "dateTimeClass", "filters", "overflow", "custom"));
        assertEquals(Collections.singletonList("08:00-12:00"), withExclusions.get("monday"));
        assertFalse(withExclusions.containsKey("custom"));
    }

    @Test
    public void testCreateAndMergeOverlappingRangesWithIgnoreData() {
        Map<String, Object> first = hoursMap("05:00-08:00");
        first.put("data", Collections.singletonMap("testdata", false));
        Map<String, Object> second = hoursMap("08:00-12:00");
        second.put("data", Collections.singletonMap("testdata", true));
        Map<String, Object> third = hoursMap("12:00-24:00");
        third.put("data", Collections.singletonMap("testdata", true));

        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList(first, second, third));

        OpeningHours kept = OpeningHours.createAndMergeOverlappingRanges(ranges, null, null, false);
        assertEquals(Arrays.asList("05:00-08:00", "08:00-24:00"), dump(kept.forDay("monday")));

        OpeningHours merged = OpeningHours.createAndMergeOverlappingRanges(ranges, null, null, true);
        assertEquals(Collections.singletonList("05:00-24:00"), dump(merged.forDay("monday")));
    }

    @Test
    public void testCanMapWeekWithACallback() {
        OpeningHours openingHours = OpeningHours.create(mapWeekSchedule());

        Map<String, Integer> mapped = openingHours.map(
                day -> day.isEmpty() ? null : day.get(0).start().hours());

        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("monday", 9);
        expected.put("tuesday", 10);
        expected.put("wednesday", 9);
        expected.put("thursday", null);
        expected.put("friday", 14);
        expected.put("saturday", null);
        expected.put("sunday", null);

        assertEquals(expected, mapped);
    }

    @Test
    public void testCanMapExceptionsWithACallback() {
        Map<String, Object> data = mapWeekSchedule();
        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-09-26", Collections.emptyList());
        exceptions.put("10-10", Arrays.asList("14:00-20:00"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        Map<String, Integer> mapped = openingHours.mapExceptions(
                day -> day.isEmpty() ? null : day.get(0).start().hours());

        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("2016-09-26", null);
        expected.put("10-10", 14);

        assertEquals(expected, mapped);
    }

    private static Map<String, Object> mapWeekSchedule() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        data.put("tuesday", Arrays.asList("10:00-18:00"));
        data.put("wednesday", Arrays.asList("09:00-12:00", "14:00-18:00"));
        data.put("thursday", Collections.emptyList());
        data.put("friday", Arrays.asList("14:00-20:00"));
        return data;
    }

    @Test
    public void testCanHandleEmptyInput() {
        OpeningHours openingHours = OpeningHours.create(new HashMap<>());

        for (DayOfWeek day : DayOfWeek.values()) {
            assertTrue(openingHours.forDay(day).isEmpty());
        }
        assertTrue(openingHours.isAlwaysClosed());
    }

    @Test
    public void testHandlesDayNamesInACaseInsensitiveManner() {
        Map<String, Object> uppercase = new LinkedHashMap<>();
        uppercase.put("Monday", Arrays.asList("09:00-18:00"));
        assertEquals("09:00-18:00", OpeningHours.create(uppercase).forDay("monday").get(0).toString());

        Map<String, Object> lowercase = new LinkedHashMap<>();
        lowercase.put("monday", Arrays.asList("09:00-18:00"));
        assertEquals("09:00-18:00", OpeningHours.create(lowercase).forDay("Monday").get(0).toString());
    }

    @Test(expected = InvalidDayName.class)
    public void testThrowsWhenUsingAnInvalidDayName() {
        Map<String, Object> data = new HashMap<>();
        data.put("mmmmonday", Arrays.asList("09:00-18:00"));
        OpeningHours.create(data);
    }

    @Test(expected = InvalidDate.class)
    public void testThrowsWhenUsingAnInvalidExceptionDate() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("25/12/2016", Collections.emptyList());
        data.put("exceptions", exceptions);
        OpeningHours.create(data);
    }

    @Test(expected = InvalidDate.class)
    public void testThrowsWhenUsingAnImpossibleExceptionDate() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> exceptions = new HashMap<>();
        exceptions.put("01-99", Collections.emptyList());
        data.put("exceptions", exceptions);
        OpeningHours.create(data);
    }

    @Test(expected = InvalidDateRange.class)
    public void testThrowsWhenDayDefinitionsOverlap() {
        Map<String, Object> data = new HashMap<>();
        data.put("monday to wednesday", Arrays.asList("09:00-17:00"));
        data.put("tuesday", Arrays.asList("09:00-17:00"));
        OpeningHours.create(data);
    }

    @Test
    public void testStoresMetaData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));

        Map<String, Object> tuesday = hoursMap(Arrays.asList("09:00-12:00", "13:00-18:00"));
        tuesday.put("data", "foobar");
        data.put("tuesday", tuesday);

        Map<String, Object> wednesday = hoursMap(Arrays.asList("09:00-12:00"));
        wednesday.put("data", Collections.singletonList("foobar"));
        data.put("wednesday", wednesday);

        Map<String, Object> thursdayRange = hoursMap("09:00-12:00");
        thursdayRange.put("data", Collections.singletonList("foobar"));
        data.put("thursday", list(thursdayRange, "13:00-18:00"));

        Map<String, Object> exceptions = new LinkedHashMap<>();
        Map<String, Object> newYearsDay = hoursMap(Arrays.asList("13:00-18:00"));
        newYearsDay.put("data", "Newyearsday opening times");
        exceptions.put("2011-01-01", newYearsDay);
        Map<String, Object> newYearsNextDay = hoursMap(Arrays.asList("13:00-18:00"));
        newYearsNextDay.put("data", "Newyearsday next day");
        exceptions.put("2011-01-02", newYearsNextDay);
        exceptions.put("12-25", Collections.singletonMap("data", "Christmas"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("Newyearsday opening times", openingHours.exceptions().get("2011-01-01").getData());
        assertEquals("Newyearsday opening times",
                openingHours.forDate(LocalDateTime.of(2011, 1, 1, 0, 0)).getData());
        assertEquals("Newyearsday next day", openingHours.exceptions().get("2011-01-02").getData());
        assertEquals("Christmas", openingHours.exceptions().get("12-25").getData());
        assertEquals("Christmas", openingHours.forDate(LocalDateTime.of(2011, 12, 25, 0, 0)).getData());

        assertNull(openingHours.forDay("monday").getData());
        assertEquals("foobar", openingHours.forDay("tuesday").getData());
        assertEquals(2, openingHours.forDay("tuesday").size());
        assertEquals(Collections.singletonList("foobar"), openingHours.forDay("wednesday").getData());
        assertEquals(1, openingHours.forDay("wednesday").size());
        assertEquals(Collections.singletonList("foobar"), openingHours.forDay("thursday").get(0).getData());
        assertNull(openingHours.forDay("thursday").get(1).getData());

        // PHP writes ["09:00-12:00", "morning"]; the Java equivalent is the {hours, data} map
        Map<String, Object> morning = hoursMap("09:00-12:00");
        morning.put("data", "morning");
        Map<String, Object> afternoon = hoursMap("13:00-18:00");
        afternoon.put("data", "afternoon");
        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", list(morning, afternoon));

        OpeningHours withDataOnRanges = OpeningHours.create(ranges);

        assertEquals("morning", withDataOnRanges.forDay("monday").get(0).getData());
        assertEquals("afternoon", withDataOnRanges.forDay("monday").get(1).getData());

        Map<String, Object> evening = hoursMap("19:00-21:00");
        evening.put("data", "Extra on Tuesday evening");
        Map<String, Object> mixed = new LinkedHashMap<>();
        mixed.put("tuesday", list("09:00-12:00", "13:00-18:00", evening));

        OpeningHours mixedRanges = OpeningHours.create(mixed);

        assertEquals("09:00-12:00,13:00-18:00,19:00-21:00", mixedRanges.forDay("tuesday").toString());
        assertNull(mixedRanges.forDay("tuesday").get(1).getData());
        assertEquals("Extra on Tuesday evening", mixedRanges.forDay("tuesday").get(2).getData());
    }

    @Test
    public void testHandlesFilters() {
        List<String> typicalDay = Arrays.asList("08:00-12:00", "14:00-18:00");

        Map<String, Object> data = new LinkedHashMap<>();
        for (String day : Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday")) {
            data.put(day, typicalDay);
        }

        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("first monday of the month", (Function<LocalDateTime, Object>) date ->
                date.getDayOfWeek() == DayOfWeek.MONDAY && date.getDayOfMonth() <= 7
                        ? Arrays.asList("08:00-11:00", "15:00-18:00")
                        : null);
        data.put("exceptions", exceptions);

        List<Function<LocalDateTime, Object>> filters = new ArrayList<>();
        filters.add(date -> date.toLocalDate().equals(LocalDate.of(2018, 4, 2))
                ? Collections.emptyList()
                : null);
        filters.add(date -> {
            if (date.getMonthValue() == date.getDayOfMonth()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("hours", typicalDay);
                result.put("data", "Month equals day");
                return result;
            }
            return null;
        });
        data.put("filters", filters);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(3, openingHours.getFilters().size());
        // 2018-12-03 is the first Monday of the month
        assertEquals("08:00-11:00,15:00-18:00",
                openingHours.forDate(LocalDateTime.of(2018, 12, 3, 0, 0)).toString());
        assertEquals("08:00-12:00,14:00-18:00",
                openingHours.forDate(LocalDateTime.of(2018, 12, 10, 0, 0)).toString());
        // Easter Monday, filtered to closed
        assertEquals("", openingHours.forDate(LocalDateTime.of(2018, 4, 2, 0, 0)).toString());
        assertEquals(LocalDateTime.of(2018, 4, 3, 8, 0),
                openingHours.nextOpen(LocalDateTime.of(2018, 3, 31, 0, 0)));
        assertEquals(LocalDateTime.of(2018, 12, 3, 11, 0),
                openingHours.nextClose(LocalDateTime.of(2018, 12, 3, 0, 0)));
        assertEquals("Month equals day",
                openingHours.forDate(LocalDateTime.of(2018, 12, 12, 0, 0)).getData());
    }

    @Test
    public void testMergesRangesOnExplicitlyCreateFromOverlappingRanges() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("08:00-12:00", "08:00-12:00", "11:30-13:30", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("08:00-12:00", "11:30-13:30", "15:00-18:00",
                "16:00-17:00", "19:00-20:00", "20:00-21:00"));

        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(data);

        assertEquals(Collections.singletonList("08:00-18:00"), dump(openingHours.forDay("monday")));
        assertEquals(Arrays.asList("08:00-13:30", "15:00-18:00", "19:00-21:00"),
                dump(openingHours.forDay("tuesday")));
    }

    @Test
    public void testMergesRangesAndDropsDateTimeClass() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dateTimeClass", "java.time.LocalDateTime");
        data.put("monday", Arrays.asList("08:00-12:00", "08:00-12:00", "11:30-13:30", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("08:00-12:00", "11:30-13:30", "15:00-18:00",
                "16:00-17:00", "19:00-20:00", "20:00-21:00"));

        Map<String, Object> merged = OpeningHours.mergeOverlappingRanges(data);

        // dateTimeClass is one of the default excluded keys, so it is dropped, as in the original library
        assertFalse(merged.containsKey("dateTimeClass"));
        assertEquals(Collections.singletonList("08:00-18:00"), merged.get("monday"));
        assertEquals(Arrays.asList("08:00-13:30", "15:00-18:00", "19:00-21:00"), merged.get("tuesday"));

        // the merged definition can then be used, since no dateTimeClass is left in it
        OpeningHours openingHours = OpeningHours.create(merged);

        // 2018-12-03 is a Monday
        assertEquals(LocalDateTime.of(2018, 12, 3, 8, 0),
                openingHours.nextOpen(LocalDateTime.of(2018, 12, 3, 0, 0)));
    }

    @Test(expected = InvalidDateTimeClass.class)
    public void testDateTimeClassKeyIsRefused() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dateTimeClass", "DateTimeImmutable");
        data.put("monday", Arrays.asList("09:00-12:00"));

        OpeningHours.create(data);
    }

    @Test
    public void testMergesRangesIncludingExplicit2400() {
        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList("08:00-12:00", "12:00-24:00"));

        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(ranges);

        assertEquals(Collections.singletonList("08:00-24:00"), dump(openingHours.forDay("monday")));
    }

    @Test
    public void testMergesOverlappingRangesUntil2400() {
        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList("08:00-12:00", "11:00-24:00"));

        Map<String, Object> merged = OpeningHours.mergeOverlappingRanges(ranges);

        @SuppressWarnings("unchecked")
        List<String> monday = (List<String>) merged.get("monday");
        assertEquals(Collections.singletonList("08:00-24:00"), monday);
    }

    @Test
    public void testMergesRangesAndKeepsData() {
        Map<String, Object> first = hoursMap("08:00-12:00");
        first.put("data", Collections.singletonMap("testdata", true));
        Map<String, Object> second = hoursMap("12:00-24:00");
        second.put("data", Collections.singletonMap("testdata", true));
        Map<String, Object> third = hoursMap("05:00-08:00");
        third.put("data", Collections.singletonMap("testdata", false));

        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList(first, second, third));

        OpeningHours openingHours = OpeningHours.create(
                OpeningHours.mergeOverlappingRanges(ranges, false));

        assertEquals(Arrays.asList("05:00-08:00", "08:00-24:00"), dump(openingHours.forDay("monday")));
        assertEquals(Collections.singletonMap("testdata", true), openingHours.forDay("monday").get(1).getData());
        assertEquals(Collections.singletonMap("testdata", false), openingHours.forDay("monday").get(0).getData());
    }

    @Test
    public void testMergesRangesIgnoringDataByDefault() {
        Map<String, Object> first = hoursMap("08:00-11:00");
        first.put("data", "a");
        Map<String, Object> second = hoursMap("10:00-12:00");
        second.put("data", "b");

        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList(first, second));

        Map<String, Object> merged = OpeningHours.mergeOverlappingRanges(ranges);
        List<?> monday = (List<?>) merged.get("monday");
        assertEquals(1, monday.size());
        assertEquals("08:00-12:00", ((Map<?, ?>) monday.get(0)).get("hours"));
        assertEquals("b", ((Map<?, ?>) monday.get(0)).get("data"));

        Map<String, Object> kept = OpeningHours.mergeOverlappingRanges(ranges, false);
        assertEquals(2, ((List<?>) kept.get("monday")).size());
    }

    @Test
    public void testReordersRanges() {
        Map<String, Object> ranges = new LinkedHashMap<>();
        ranges.put("monday", Arrays.asList("13:00-24:00", "08:00-12:00"));

        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(ranges);

        // 2019-07-06 is a Saturday, the next opening is Monday 08:00
        assertEquals(LocalDateTime.of(2019, 7, 8, 8, 0),
                openingHours.nextOpen(LocalDateTime.of(2019, 7, 6, 7, 25)));
    }

    @Test
    public void testFilterAndEvery() {
        OpeningHours openingHours = OpeningHours.create(typicalWeek());

        assertEquals(Collections.singletonList("sunday"),
                new ArrayList<>(openingHours.filter(OpeningHoursForDay::isEmpty).keySet()));
        assertTrue(openingHours.every(day -> day.size() <= 2));
        assertFalse(openingHours.every(day -> !day.isEmpty()));
    }

    @Test
    public void testFlatMapWeek() {
        OpeningHours openingHours = OpeningHours.create(typicalWeek());

        List<String> ranges = openingHours.flatMap(day -> day.getTimeRanges().stream()
                .map(TimeRange::toString)
                .collect(Collectors.toList()));

        assertEquals(11, ranges.size());
        assertTrue(ranges.contains("09:00-12:00"));
    }

    @Test
    public void testExceptionAggregations() {
        Map<String, Object> data = typicalWeek();
        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-12-25", Collections.emptyList());
        exceptions.put("2016-11-11", Arrays.asList("09:00-12:00"));
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals(Collections.singletonList("2016-12-25"),
                new ArrayList<>(openingHours.filterExceptions(OpeningHoursForDay::isEmpty).keySet()));
        assertFalse(openingHours.everyExceptions(day -> !day.isEmpty()));
        assertTrue(openingHours.flatMapExceptions(day -> day.getTimeRanges().stream()
                        .map(TimeRange::toString)
                        .collect(Collectors.toList()))
                .contains("09:00-12:00"));
    }

    @Test
    public void testSupportsEmptyArraysWithMerge() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Collections.emptyList());

        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(data);

        assertTrue(openingHours.forDay("monday").isEmpty());
    }

    private static List<String> dump(OpeningHoursForDay day) {
        return day.getTimeRanges().stream().map(TimeRange::toString).collect(Collectors.toList());
    }
}
