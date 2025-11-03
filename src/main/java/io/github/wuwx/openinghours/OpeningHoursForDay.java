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
    
    /**
     * Constructs an empty OpeningHoursForDay (closed all day)
     */
    public OpeningHoursForDay() {
        this(new ArrayList<>(), null);
    }
    
    /**
     * Constructs an OpeningHoursForDay with time ranges
     * 
     * @param timeRanges the list of time ranges
     */
    public OpeningHoursForDay(List<TimeRange> timeRanges) {
        this(timeRanges, null);
    }
    
    /**
     * Constructs an OpeningHoursForDay with time ranges and associated data
     * 
     * @param timeRanges the list of time ranges
     * @param data the associated data
     */
    public OpeningHoursForDay(List<TimeRange> timeRanges, Object data) {
        this.timeRanges = new ArrayList<>(timeRanges);
        this.data = data;
    }
    
    /**
     * Creates an OpeningHoursForDay from time range strings
     * 
     * @param openingHoursStrings list of time range strings (e.g., ["09:00-12:00", "13:00-18:00"])
     * @return a new OpeningHoursForDay instance
     */
    public static OpeningHoursForDay fromStrings(List<String> openingHoursStrings) {
        return fromStrings(openingHoursStrings, null);
    }
    
    /**
     * Creates an OpeningHoursForDay from time range strings with associated data
     * 
     * @param openingHoursStrings list of time range strings
     * @param data the associated data
     * @return a new OpeningHoursForDay instance
     */
    public static OpeningHoursForDay fromStrings(List<String> openingHoursStrings, Object data) {
        List<TimeRange> ranges = new ArrayList<>();
        for (String hourString : openingHoursStrings) {
            ranges.add(TimeRange.fromString(hourString));
        }
        return new OpeningHoursForDay(ranges, data);
    }
    
    /**
     * Checks if open at the given time
     * 
     * @param time the time to check
     * @return true if open at this time
     */
    public boolean isOpenAt(LocalTime time) {
        for (TimeRange timeRange : timeRanges) {
            if (timeRange.containsTime(time)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if this day has no opening hours (closed all day)
     * 
     * @return true if closed all day
     */
    public boolean isEmpty() {
        return timeRanges.isEmpty();
    }
    
    /**
     * Gets an unmodifiable list of time ranges
     * 
     * @return the list of time ranges
     */
    public List<TimeRange> getTimeRanges() {
        return Collections.unmodifiableList(timeRanges);
    }
    
    /**
     * Gets the time range at the specified index
     * 
     * @param index the index
     * @return the time range at the index
     */
    public TimeRange get(int index) {
        return timeRanges.get(index);
    }
    
    /**
     * Gets the number of time ranges
     * 
     * @return the number of time ranges
     */
    public int size() {
        return timeRanges.size();
    }
    
    /**
     * Gets the associated data
     * 
     * @return the associated data, or null if no data
     */
    public Object getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return timeRanges.toString();
    }
}
