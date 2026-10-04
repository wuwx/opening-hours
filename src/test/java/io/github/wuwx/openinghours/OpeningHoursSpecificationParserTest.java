package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidOpeningHoursSpecification;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;

public class OpeningHoursSpecificationParserTest {

    private static Map<String, Object> spec(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    public void testRangeOverNight() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("dayOfWeek", "Monday", "opens", "18:00", "closes", "02:00"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        // 2023-11-27 is a Monday
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 27, 17, 50)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 27, 23, 55)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 27, 23, 59, 59)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 28, 1, 50)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 28, 19, 0)));
    }

    @Test
    public void testH24Specs() {
        List<Map<String, Object>> weekdays = Collections.singletonList(spec(
                "opens", "00:00",
                "closes", "23:59",
                "dayOfWeek", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(weekdays);

        // 23:59 is assumed to mean until the end of the day
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 27, 23, 59, 34)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 11, 25, 23, 59, 34)));
        assertFalse(openingHours.isAlwaysOpen());

        List<Map<String, Object>> wholeWeek = Collections.singletonList(spec(
                "opens", "00:00",
                "closes", "23:59",
                "dayOfWeek", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")));

        assertTrue(OpeningHours.createFromStructuredData(wholeWeek).isAlwaysOpen());
    }

    @Test
    public void testClosedDay() {
        List<Map<String, Object>> specifications = Collections.singletonList(spec("dayOfWeek", "Monday"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.forDay("monday").isEmpty());
    }

    @Test
    public void testClosedAllDayRange() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("dayOfWeek", "Monday", "opens", "00:00", "closes", "00:00"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.forDay("monday").isEmpty());
    }

    @Test
    public void testStripSeconds() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("dayOfWeek", "Monday", "opens", "09:00:00", "closes", "17:00:00"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertEquals("09:00-17:00", openingHours.forDay("monday").get(0).toString());
    }

    @Test
    public void testClosesIsConvertedToMidnight() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("dayOfWeek", "Monday", "opens", "09:00", "closes", "23:59"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertEquals("09:00-24:00", openingHours.forDay("monday").get(0).toString());
    }

    @Test
    public void testSingleDayException() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("validFrom", "2023-12-25", "validThrough", "2023-12-25"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.forDate(LocalDateTime.of(2023, 12, 25, 10, 0)).isEmpty());
        assertTrue(openingHours.exceptions().containsKey("2023-12-25"));
    }

    @Test
    public void testExceptionRange() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("validFrom", "2023-12-24", "validThrough", "2023-12-26"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.forDate(LocalDateTime.of(2023, 12, 24, 10, 0)).isEmpty());
        assertTrue(openingHours.forDate(LocalDateTime.of(2023, 12, 26, 10, 0)).isEmpty());
        assertTrue(openingHours.exceptions().containsKey("2023-12-24"));
        assertTrue(openingHours.exceptions().containsKey("2023-12-25"));
        assertTrue(openingHours.exceptions().containsKey("2023-12-26"));
    }

    @Test
    public void testRecurringException() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("opens", "09:00", "closes", "12:00", "validFrom", "12-25", "validThrough", "12-25"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 25, 10, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 25, 15, 0)));
    }

    @Test
    public void testDayOfWeekAsString() {
        List<Map<String, Object>> specifications = Collections.singletonList(
                spec("dayOfWeek", "Monday", "opens", "09:00", "closes", "18:00"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        assertTrue(openingHours.isOpenOn("monday"));
        assertFalse(openingHours.isOpenOn("tuesday"));
    }

    @Test
    public void testCreateFromStructuredData() {
        List<Map<String, Object>> specifications = new ArrayList<>();

        specifications.add(spec("opens", "08:00", "closes", "12:00", "dayOfWeek", Arrays.asList(
                "https://schema.org/Monday",
                "https://schema.org/Tuesday",
                "https://schema.org/Wednesday",
                "https://schema.org/Thursday",
                "https://schema.org/Friday")));
        specifications.add(spec("opens", "14:00", "closes", "18:00", "dayOfWeek", Arrays.asList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")));
        specifications.add(spec("opens", "00:00", "closes", "00:00",
                "validFrom", "2023-12-25", "validThrough", "2023-12-25"));

        OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);

        // 2023-12-22 is a Friday, 2023-12-23 a Saturday
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 22, 9, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 22, 13, 0)));
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 22, 15, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 23, 9, 0)));
        assertFalse(openingHours.isOpenAt(LocalDateTime.of(2023, 12, 25, 10, 0)));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidDayOfWeek() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", "Someday", "opens", "09:00", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidDayType() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", 42, "opens", "09:00", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testUnsupportedPublicHolidays() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", "PublicHolidays", "opens", "09:00", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testMissingValidDates() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("opens", "09:00", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidOpens() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", "Monday", "opens", "noon", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidCloses() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", "Monday", "opens", "09:00", "closes", "noon")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testClosesWithoutOpens() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("dayOfWeek", "Monday", "closes", "18:00")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidValidFrom() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("validFrom", "2023-1-1", "validThrough", "2023-12-25")));
    }

    @Test(expected = InvalidOpeningHoursSpecification.class)
    public void testInvalidValidThrough() {
        OpeningHours.createFromStructuredData(
                Collections.singletonList(spec("validFrom", "2023-12-24", "validThrough", "25/12/2023")));
    }
}
