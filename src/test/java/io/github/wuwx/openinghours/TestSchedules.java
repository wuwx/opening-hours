package io.github.wuwx.openinghours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestSchedules {

    static final List<String> ALL_DAYS = Arrays.asList(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

    private TestSchedules() {
    }

    static Map<String, Object> typicalWeek() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("tuesday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("wednesday", Arrays.asList("09:00-12:00"));
        data.put("thursday", Arrays.asList("09:00-12:00", "13:00-18:00"));
        data.put("friday", Arrays.asList("09:00-12:00", "13:00-20:00"));
        data.put("saturday", Arrays.asList("09:00-12:00", "13:00-16:00"));
        data.put("sunday", Collections.emptyList());
        return data;
    }

    static Map<String, Object> week(String hours) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String day : ALL_DAYS) {
            data.put(day, hours == null ? Collections.emptyList() : Arrays.asList(hours));
        }
        return data;
    }

    static Map<String, Object> mondayNineToSix() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monday", Arrays.asList("09:00-18:00"));
        return data;
    }

    static Map<String, Object> hoursMap(Object hours) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hours", hours);
        return map;
    }

    static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
