package io.github.wuwx.openinghours;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents opening hours for a single day
 * 
 * @author wuwx
 */
public class OpeningHoursForDay {
    private final List<TimeRange> timeRanges;
    private final Object data;
    
    public OpeningHoursForDay() {
        this(new ArrayList<>(), null);
    }
    
    public OpeningHoursForDay(List<TimeRange> timeRanges) {
        this(timeRanges, null);
    }
    
    public OpeningHoursForDay(List<TimeRange> timeRanges, Object data) {
        this.timeRanges = new ArrayList<>(timeRanges);
        this.data = data;
    }
    
    public static OpeningHoursForDay fromStrings(List<String> openingHoursStrings) {
        return fromStrings(openingHoursStrings, null);
    }
    
    public static OpeningHoursForDay fromStrings(List<String> openingHoursStrings, Object data) {
        List<TimeRange> ranges = new ArrayList<>();
        for (String hourString : openingHoursStrings) {
            ranges.add(TimeRange.fromString(hourString));
        }
        return new OpeningHoursForDay(ranges, data);
    }
    
    public boolean isOpenAt(LocalTime time) {
        for (TimeRange timeRange : timeRanges) {
            if (timeRange.containsTime(time)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isEmpty() {
        return timeRanges.isEmpty();
    }
    
    public List<TimeRange> getTimeRanges() {
        return Collections.unmodifiableList(timeRanges);
    }
    
    public TimeRange get(int index) {
        return timeRanges.get(index);
    }
    
    public int size() {
        return timeRanges.size();
    }
    
    public Object getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return timeRanges.toString();
    }
}
