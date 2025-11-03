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
    
    public Time(LocalTime time) {
        this.time = time;
    }
    
    public static Time fromString(String timeString) {
        // Handle special case "24:00"
        if ("24:00".equals(timeString)) {
            return new Time(LocalTime.MAX); // 23:59:59.999999999
        }
        return new Time(LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm")));
    }
    
    public LocalTime toLocalTime() {
        return time;
    }
    
    public boolean isBefore(Time other) {
        return this.time.isBefore(other.time);
    }
    
    public boolean isAfter(Time other) {
        return this.time.isAfter(other.time);
    }
    
    public boolean isSameOrAfter(Time other) {
        return !this.time.isBefore(other.time);
    }
    
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

