package io.github.wuwx.openinghours;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.wuwx.openinghours.TestSchedules.mondayNineToSix;
import static org.junit.Assert.*;

// PHP 版本的 dateTimeClass 用例在 Java 中不适用：没有可注入的自定义日期类，该键会被拒绝（见 OpeningHoursFillTest#testDateTimeClassKeyIsRefused）
public class OpeningHoursCustomClassTest {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV");

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute, String zone) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of(zone));
    }

    private static String fmt(ZonedDateTime dateTime) {
        return dateTime.format(FORMAT);
    }

    @Test
    public void testNextOpenWithoutConfiguredTimezone() {
        OpeningHours openingHours = OpeningHours.create(mondayNineToSix());

        assertEquals("2022-07-25 09:00:00 UTC",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "UTC"))));
        assertEquals("2022-07-25 09:00:00 Europe/Oslo",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "Europe/Oslo"))));
    }

    @Test
    public void testConfiguredInputTimezoneKeepsCallerTimezone() {
        Map<String, Object> data = mondayNineToSix();
        data.put("timezone", "Europe/Oslo");

        OpeningHours openingHours = OpeningHours.create(data);

        // 06:30 UTC = 08:30 Oslo, still closed, opens at 09:00 Oslo = 07:00 UTC
        assertEquals("2022-07-25 07:00:00 UTC",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 6, 30, "UTC"))));
        // 07:30 UTC = 09:30 Oslo, already open, next opening is next Monday
        assertEquals("2022-08-01 07:00:00 UTC",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "UTC"))));
    }

    @Test
    public void testExplicitOutputTimezone() {
        OpeningHours openingHours = OpeningHours.create(mondayNineToSix(), ZoneId.of("Europe/Oslo"))
                .setOutputTimezone(ZoneId.of("Europe/Oslo"));

        assertEquals("2022-07-25 09:00:00 Europe/Oslo",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 6, 30, "UTC"))));
        assertEquals("2022-08-01 09:00:00 Europe/Oslo",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "UTC"))));
    }

    @Test
    public void testInputAndOutputTimezoneInData() {
        Map<String, Object> data = mondayNineToSix();
        Map<String, String> timezone = new LinkedHashMap<>();
        timezone.put("input", "Europe/Oslo");
        timezone.put("output", "UTC");
        data.put("timezone", timezone);

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("2022-07-25 07:00:00 UTC",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 6, 30, "UTC"))));
        assertEquals("2022-08-01 07:00:00 UTC",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "UTC"))));
    }

    @Test
    public void testInputAndOutputTimezoneAsParameters() {
        OpeningHours openingHours = OpeningHours.create(
                mondayNineToSix(), ZoneId.of("Europe/Oslo"), ZoneId.of("America/New_York"));

        assertEquals("2022-07-25 03:00:00 America/New_York",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 6, 30, "UTC"))));
        assertEquals("2022-08-01 03:00:00 America/New_York",
                fmt(openingHours.nextOpen(at(2022, 7, 25, 7, 30, "UTC"))));
    }

    @Test
    public void testIsOpenAcrossDaylightSavingTransition() {
        Map<String, Object> data = mondayNineToSix();
        data.put("timezone", "Europe/Oslo");

        OpeningHours openingHours = OpeningHours.create(data);

        // 2025-03-24 (UTC+1): 08:00 UTC is 09:00 Oslo
        assertTrue(openingHours.isOpenAt(at(2025, 3, 24, 8, 0, "UTC")));
        assertFalse(openingHours.isOpenAt(at(2025, 3, 24, 7, 0, "UTC")));

        // 2025-03-31 (UTC+2 after the transition): 07:00 UTC is 09:00 Oslo
        assertTrue(openingHours.isOpenAt(at(2025, 3, 31, 7, 0, "UTC")));
        assertFalse(openingHours.isOpenAt(at(2025, 3, 31, 6, 0, "UTC")));
    }

    @Test
    public void testCurrentOpenRangeWithTimezone() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("22:00-02:00"));

        OpeningHours openingHours = OpeningHours.create(data, ZoneId.of("Europe/Oslo"));

        Optional<DateTimeRange> range = openingHours.currentOpenRange(at(2016, 12, 20, 1, 0, "Europe/Oslo"));

        assertTrue(range.isPresent());
        assertTrue(range.get().overflowsNextDay());
        assertEquals("2016-12-19 22:00:00 Europe/Oslo", fmt(range.get().start()));
        assertEquals("2016-12-20 02:00:00 Europe/Oslo", fmt(range.get().end()));
        assertEquals("22:00-02:00", range.get().timeRange().toString());

        assertEquals("2016-12-19 22:00:00 Europe/Oslo",
                fmt(openingHours.currentOpenRangeStart(at(2016, 12, 20, 1, 0, "Europe/Oslo")).get()));
        assertEquals("2016-12-20 02:00:00 Europe/Oslo",
                fmt(openingHours.currentOpenRangeEnd(at(2016, 12, 20, 1, 0, "Europe/Oslo")).get()));
    }

    @Test
    public void testForDateTimeWithTimezone() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.put("monday", Arrays.asList("22:00-02:00"));
        data.put("timezone", "Europe/Oslo");

        OpeningHours openingHours = OpeningHours.create(data);

        // 00:30 UTC is 01:30 Oslo, inside Monday's night range
        assertEquals(1, openingHours.forDateTime(at(2016, 12, 20, 0, 30, "UTC")).size());
        // 01:00 UTC is 02:00 Oslo, the night range has just closed
        assertEquals(0, openingHours.forDateTime(at(2016, 12, 20, 1, 0, "UTC")).size());
        // the LocalDateTime overload keeps the plain wall-clock behaviour
        assertEquals(1, openingHours.forDateTime(LocalDateTime.of(2016, 12, 20, 1, 0)).size());
    }

    @Test
    public void testLocalDateTimeApiIsUnaffectedByTimezone() {
        Map<String, Object> data = mondayNineToSix();
        data.put("timezone", "Europe/Oslo");

        OpeningHours openingHours = OpeningHours.create(data);

        // LocalDateTime overloads keep the plain wall-clock behaviour
        assertTrue(openingHours.isOpenAt(LocalDateTime.of(2022, 7, 25, 10, 0)));
        assertEquals(LocalDateTime.of(2022, 7, 25, 18, 0),
                openingHours.nextClose(LocalDateTime.of(2022, 7, 25, 10, 0)));
    }
}
