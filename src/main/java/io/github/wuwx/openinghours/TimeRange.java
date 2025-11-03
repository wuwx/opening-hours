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
    
    public TimeRange(Time start, Time end) {
        this(start, end, null);
    }
    
    public TimeRange(Time start, Time end, Object data) {
        this.start = start;
        this.end = end;
        this.data = data;
    }
    
    public static TimeRange fromString(String timeRangeString) {
        return fromString(timeRangeString, null);
    }
    
    public static TimeRange fromString(String timeRangeString, Object data) {
        String[] parts = timeRangeString.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time range format: " + timeRangeString);
        }
        Time start = Time.fromString(parts[0].trim());
        Time end = Time.fromString(parts[1].trim());
        return new TimeRange(start, end, data);
    }
    
    public Time start() {
        return start;
    }
    
    public Time end() {
        return end;
    }
    
    public Object getData() {
        return data;
    }
    
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
