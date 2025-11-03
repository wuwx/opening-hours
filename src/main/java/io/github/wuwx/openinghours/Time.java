package io.github.wuwx.openinghours;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Value object representing a single time
 * 
 * @author wuwx
 */
public class Time {
    private final LocalTime time;
    
    /**
     * Constructs a Time from a LocalTime
     * 
     * @param time the local time
     */
    public Time(LocalTime time) {
        this.time = time;
    }
    
    /**
     * Creates a Time from a time string in HH:mm format
     * Supports special "24:00" to represent end of day
     * 
     * @param timeString the time string (e.g., "09:00", "24:00")
     * @return a new Time instance
     */
    public static Time fromString(String timeString) {
        // Handle special case "24:00"
        if ("24:00".equals(timeString)) {
            return new Time(LocalTime.MAX); // 23:59:59.999999999
        }
        return new Time(LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm")));
    }
    
    /**
     * Converts this Time to a LocalTime
     * 
     * @return the local time
     */
    public LocalTime toLocalTime() {
        return time;
    }
    
    /**
     * Checks if this time is before another time
     * 
     * @param other the time to compare with
     * @return true if this time is before the other time
     */
    public boolean isBefore(Time other) {
        return this.time.isBefore(other.time);
    }
    
    /**
     * Checks if this time is after another time
     * 
     * @param other the time to compare with
     * @return true if this time is after the other time
     */
    public boolean isAfter(Time other) {
        return this.time.isAfter(other.time);
    }
    
    /**
     * Checks if this time is same or after another time
     * 
     * @param other the time to compare with
     * @return true if this time is same or after the other time
     */
    public boolean isSameOrAfter(Time other) {
        return !this.time.isBefore(other.time);
    }
    
    /**
     * Formats this time using the specified pattern
     * 
     * @param pattern the date time pattern (e.g., "HH:mm", "hh:mm a")
     * @return the formatted time string
     */
    public String format(String pattern) {
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    @Override
    public String toString() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time time1 = (Time) o;
        return time.equals(time1.time);
    }
    
    @Override
    public int hashCode() {
        return time.hashCode();
    }
}

