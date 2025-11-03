package io.github.wuwx.openinghours;

import java.time.LocalTime;

/**
 * Value object representing a time range with start and end
 * 
 * @author wuwx
 */
public class TimeRange {
    private final Time start;
    private final Time end;
    private final Object data;
    
    /**
     * Constructs a TimeRange with start and end times
     * 
     * @param start the start time
     * @param end the end time
     */
    public TimeRange(Time start, Time end) {
        this(start, end, null);
    }
    
    /**
     * Constructs a TimeRange with start, end times and associated data
     * 
     * @param start the start time
     * @param end the end time
     * @param data the associated data (can be any object)
     */
    public TimeRange(Time start, Time end, Object data) {
        this.start = start;
        this.end = end;
        this.data = data;
    }
    
    /**
     * Creates a TimeRange from a string in format "HH:mm-HH:mm"
     * 
     * @param timeRangeString the time range string (e.g., "09:00-17:00")
     * @return a new TimeRange instance
     */
    public static TimeRange fromString(String timeRangeString) {
        return fromString(timeRangeString, null);
    }
    
    /**
     * Creates a TimeRange from a string with associated data
     * 
     * @param timeRangeString the time range string (e.g., "09:00-17:00")
     * @param data the associated data
     * @return a new TimeRange instance
     */
    public static TimeRange fromString(String timeRangeString, Object data) {
        String[] parts = timeRangeString.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time range format: " + timeRangeString);
        }
        Time start = Time.fromString(parts[0].trim());
        Time end = Time.fromString(parts[1].trim());
        return new TimeRange(start, end, data);
    }
    
    /**
     * Gets the start time of this range
     * 
     * @return the start time
     */
    public Time start() {
        return start;
    }
    
    /**
     * Gets the end time of this range
     * 
     * @return the end time
     */
    public Time end() {
        return end;
    }
    
    /**
     * Gets the associated data
     * 
     * @return the associated data, or null if no data
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Checks if this time range contains the given time
     * Handles overnight ranges (e.g., 22:00-02:00)
     * 
     * @param time the time to check
     * @return true if this range contains the time
     */
    public boolean containsTime(LocalTime time) {
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        
        // Handle overnight ranges (e.g., 22:00-02:00)
        if (endTime.isBefore(startTime) || endTime.equals(LocalTime.MAX)) {
            // Overnight: time >= start OR time <= end
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        } else {
            // Normal range: start <= time < end
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }
    }
    
    /**
     * Checks if this time range overlaps with another time range
     * 
     * @param other the other time range
     * @return true if the ranges overlap
     */
    public boolean overlaps(TimeRange other) {
        // Check if this range overlaps with another range
        return containsTime(other.start.toLocalTime()) || 
               containsTime(other.end.toLocalTime()) ||
               other.containsTime(this.start.toLocalTime()) ||
               other.containsTime(this.end.toLocalTime());
    }
    
    @Override
    public String toString() {
        return start.toString() + "-" + end.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRange timeRange = (TimeRange) o;
        return start.equals(timeRange.start) && end.equals(timeRange.end);
    }
    
    @Override
    public int hashCode() {
        return 31 * start.hashCode() + end.hashCode();
    }
}
