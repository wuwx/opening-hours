package io.github.wuwx.openinghours;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

public class OpeningHoursStructuredDataTest {

    private static final DateTimeFormatter ZONED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV");

    private static Map<String, Object> spec(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    public void testAsStructuredDataList() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-18:00"));
        data.put("wednesday", Arrays.asList("09:00-12:00", "14:00-18:00"));
        data.put("thursday", Collections.emptyList());
        data.put("friday", Arrays.asList("09:00-20:00"));

        Map<String, Object> exceptions = new LinkedHashMap<>();
        exceptions.put("2016-09-26", Arrays.asList("09:00-12:00"));
        exceptions.put("2016-09-27", Collections.emptyList());
        data.put("exceptions", exceptions);

        OpeningHours openingHours = OpeningHours.create(data);

        List<Map<String, Object>> expected = new ArrayList<>();
        expected.add(spec("@type", "OpeningHoursSpecification", "dayOfWeek", "Monday", "opens", "09:00", "closes", "18:00"));
        expected.add(spec("@type", "OpeningHoursSpecification", "dayOfWeek", "Tuesday", "opens", "09:00", "closes", "18:00"));
        expected.add(spec("@type", "OpeningHoursSpecification", "dayOfWeek", "Wednesday", "opens", "09:00", "closes", "12:00"));
        expected.add(spec("@type", "OpeningHoursSpecification", "dayOfWeek", "Wednesday", "opens", "14:00", "closes", "18:00"));
        expected.add(spec("@type", "OpeningHoursSpecification", "dayOfWeek", "Friday", "opens", "09:00", "closes", "20:00"));
        expected.add(spec("@type", "OpeningHoursSpecification", "opens", "09:00", "closes", "12:00",
                "validFrom", "2016-09-26", "validThrough", "2016-09-26"));
        expected.add(spec("@type", "OpeningHoursSpecification", "opens", "00:00", "closes", "00:00",
                "validFrom", "2016-09-27", "validThrough", "2016-09-27"));

        assertEquals(expected, openingHours.asStructuredData());
    }

    @Test
    public void testAsStructuredDataWithCustomFormat() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", spec("hours", Arrays.asList("09:00-17:00")));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("17:00:00", openingHours.asStructuredData("HH:mm:ss").get(0).get("closes"));
    }

    @Test
    public void testAsStructuredDataKeepsMidnight() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-24:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("24:00", openingHours.asStructuredData().get(0).get("closes"));
        assertEquals("24:00:00", openingHours.asStructuredData("HH:mm:ss").get(0).get("closes"));
    }

    @Test
    public void testAsStructuredDataWithTimezone() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("17:00:00+00:00",
                openingHours.asStructuredData("HH:mm:ssxxx", ZoneId.of("UTC")).get(0).get("closes"));
        assertEquals("17:00:00+01:00",
                openingHours.asStructuredData("HH:mm:ssxxx", ZoneId.of("Europe/Paris")).get(0).get("closes"));
        assertEquals("17:00:00-05:00",
                openingHours.asStructuredData("HH:mm:ssxxx", ZoneId.of("-05:00")).get(0).get("closes"));
    }

    @Test
    public void testAsStructuredDataKeepsMidnightWithTimezone() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-24:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("24:00", openingHours.asStructuredData("HH:mm", ZoneId.of("Europe/Paris")).get(0).get("closes"));
        assertEquals("24:00:00", openingHours.asStructuredData("HH:mm:ss", ZoneId.of("Europe/Paris")).get(0).get("closes"));
    }

    @Test
    public void testAsStructuredJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("[{\"@type\":\"OpeningHoursSpecification\",\"dayOfWeek\":\"Monday\","
                + "\"opens\":\"09:00\",\"closes\":\"17:00\"}]", openingHours.asStructuredJson());
    }

    @Test
    public void testAsStructuredJsonWithFormatAndTimezone() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-17:00"));

        OpeningHours openingHours = OpeningHours.create(data);

        assertEquals("[{\"@type\":\"OpeningHoursSpecification\",\"dayOfWeek\":\"Monday\","
                        + "\"opens\":\"09:00:00\",\"closes\":\"17:00:00\"}]",
                openingHours.asStructuredJson("HH:mm:ss"));
        assertTrue(openingHours.asStructuredJson("HH:mm:ssxxx", ZoneId.of("UTC"))
                .contains("\"closes\":\"17:00:00+00:00\""));
    }

    @Test
    public void testJsonEscaping() {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("quote", "a\"b");
        spec.put("backslash", "a\\b");
        spec.put("control", "a\nb\tc");
        spec.put("unicode", "a\u0001b");
        spec.put("number", 42);

        assertEquals("[{\"quote\":\"a\\\"b\",\"backslash\":\"a\\\\b\",\"control\":\"a\\nb\\tc\","
                        + "\"unicode\":\"a\\u0001b\",\"number\":42}]",
                OpeningHours.toJson(Collections.singletonList(spec)));
    }

    @Test
    public void testFindPreviousCloseTimeWithCustomTimezone() {
        OpeningHours openingHours = OpeningHours.createAndMergeOverlappingRanges(
                TestSchedules.week("10:00-23:59"), ZoneId.of("Australia/Brisbane"), null);

        // 2025-05-23 is a Friday, the schedule opens at 10:00
        ZonedDateTime now = ZonedDateTime.of(2025, 5, 23, 9, 0, 0, 0, ZoneId.of("Australia/Brisbane"));

        assertEquals("2025-05-22 23:59:00 Australia/Brisbane",
                openingHours.previousClose(now).format(ZONED_FORMAT));
    }

    @Test
    public void testRoundTripThroughStructuredData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("sunday", Collections.emptyList());

        OpeningHours openingHours = OpeningHours.create(data);

        OpeningHours rebuilt = OpeningHours.createFromStructuredData(openingHours.asStructuredData());

        assertTrue(rebuilt.isOpenAt(LocalDateTime.of(2016, 12, 19, 10, 0)));
        assertFalse(rebuilt.isOpenAt(LocalDateTime.of(2016, 12, 19, 12, 30)));
        assertTrue(rebuilt.isOpenAt(LocalDateTime.of(2016, 12, 19, 15, 0)));
        assertFalse(rebuilt.isOpenOn("sunday"));
    }
}
