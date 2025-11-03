package io.github.wuwx.openinghours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main class for handling opening hours
 * 
 * @author wuwx
 */
public class OpeningHours {

    private final Map<DayOfWeek, OpeningHoursForDay> openingHours;
    private final Map<String, OpeningHoursForDay> exceptions;
    private final List<Function<LocalDate, Map<String, Object>>> filters;
    private final ZoneId timezone;
    private final ZoneId outputTimezone;
    private final boolean overflow;
    
    private OpeningHours(Map<DayOfWeek, OpeningHoursForDay> openingHours,
                        Map<String, OpeningHoursForDay> exceptions,
                        List<Function<LocalDate, Map<String, Object>>> filters,
                        ZoneId timezone,
                        ZoneId outputTimezone,
                        boolean overflow) {
        this.openingHours = openingHours;
        this.exceptions = exceptions;
        this.filters = filters;
        this.timezone = timezone;
        this.outputTimezone = outputTimezone;
        this.overflow = overflow;
    }
    
    /**
     * Create opening hours from data map
     * 
     * @param data the opening hours data map
     * @return a new OpeningHours instance
     */
    public static OpeningHours create(Map<String, Object> data) {
        return create(data, null, null);
    }
    
    /**
     * Create opening hours with timezone
     * 
     * @param data the opening hours data map
     * @param timezone the timezone for input and output
     * @return a new OpeningHours instance
     */
    public static OpeningHours create(Map<String, Object> data, ZoneId timezone) {
        return create(data, timezone, timezone);
    }
    
    /**
     * Create opening hours with separate input and output timezones
     * 
     * @param data the opening hours data map
     * @param timezone the input timezone
     * @param outputTimezone the output timezone
     * @return a new OpeningHours instance
     */
    public static OpeningHours create(Map<String, Object> data, ZoneId timezone, ZoneId outputTimezone) {
        Map<DayOfWeek, OpeningHoursForDay> openingHours = new EnumMap<>(DayOfWeek.class);
        Map<String, OpeningHoursForDay> exceptions = new HashMap<>();
        List<Function<LocalDate, Map<String, Object>>> filters = new ArrayList<>();
        boolean overflow = false;
        
        // Initialize all days as closed
        for (DayOfWeek day : DayOfWeek.values()) {
            openingHours.put(day, new OpeningHoursForDay());
        }
        
        // Parse data
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if ("exceptions".equals(key)) {
                // Handle exceptions
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> exceptionsMap = (Map<String, Object>) value;
                    for (Map.Entry<String, Object> excEntry : exceptionsMap.entrySet()) {
                        String dateKey = excEntry.getKey();
                        Object excValue = excEntry.getValue();
                        
                        // Handle date ranges in exceptions (e.g., "12-24 to 12-26")
                        if (dateKey.contains(" to ")) {
                            String[] parts = dateKey.split(" to ");
                            if (parts.length == 2) {
                                parseDateRangeException(parts[0].trim(), parts[1].trim(), excValue, exceptions);
                            }
                        } else {
                            exceptions.put(dateKey, parseOpeningHoursValue(excValue));
                        }
                    }
                }
            } else if ("filters".equals(key)) {
                // Handle filters
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Function<LocalDate, Map<String, Object>>> filterList = 
                        (List<Function<LocalDate, Map<String, Object>>>) value;
                    filters.addAll(filterList);
                }
            } else if ("overflow".equals(key)) {
                // Handle overflow flag
                if (value instanceof Boolean) {
                    overflow = (Boolean) value;
                }
            } else if ("timezone".equals(key)) {
                // Handle timezone (already handled in calling method)
                continue;
            } else {
                // Handle day names (monday, tuesday, etc.) or day ranges (monday to friday)
                if (key.contains(" to ")) {
                    // Handle day range
                    String[] parts = key.split(" to ");
                    if (parts.length == 2) {
                        parseDayRange(parts[0].trim(), parts[1].trim(), value, openingHours);
                    }
                } else {
                    // Handle single day
                    try {
                        DayOfWeek dayOfWeek = DayOfWeek.valueOf(key.toUpperCase());
                        openingHours.put(dayOfWeek, parseOpeningHoursValue(value));
                    } catch (IllegalArgumentException e) {
                        // Invalid day name, skip
                    }
                }
            }
        }
        
        return new OpeningHours(openingHours, exceptions, filters, timezone, outputTimezone, overflow);
    }
    
    private static void parseDayRange(String startDay, String endDay, Object value, 
                                     Map<DayOfWeek, OpeningHoursForDay> openingHours) {
        try {
            DayOfWeek start = DayOfWeek.valueOf(startDay.toUpperCase());
            DayOfWeek end = DayOfWeek.valueOf(endDay.toUpperCase());
            OpeningHoursForDay hours = parseOpeningHoursValue(value);
            
            DayOfWeek current = start;
            while (true) {
                openingHours.put(current, hours);
                if (current == end) break;
                current = current.plus(1);
            }
        } catch (IllegalArgumentException e) {
            // Invalid day names, skip
        }
    }
    
    private static void parseDateRangeException(String startDate, String endDate, Object value,
                                                Map<String, OpeningHoursForDay> exceptions) {
        try {
            java.time.LocalDate start;
            java.time.LocalDate end;
            boolean isRecurring = false;
            
            // Check if it's a recurring range (MM-dd format)
            if (startDate.length() == 5 && startDate.charAt(2) == '-') {
                isRecurring = true;
                // Parse as MM-dd, use current year or a reference year
                start = java.time.LocalDate.parse("2000-" + startDate);
                end = java.time.LocalDate.parse("2000-" + endDate);
            } else {
                // Parse as full date
                start = java.time.LocalDate.parse(startDate);
                end = java.time.LocalDate.parse(endDate);
            }
            
            OpeningHoursForDay hours = parseOpeningHoursValue(value);
            
            // Add all dates in range
            java.time.LocalDate current = start;
            while (!current.isAfter(end)) {
                String key;
                if (isRecurring) {
                    key = String.format("%02d-%02d", current.getMonthValue(), current.getDayOfMonth());
                } else {
                    key = current.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                }
                exceptions.put(key, hours);
                current = current.plusDays(1);
            }
        } catch (Exception e) {
            // Invalid date format, skip
        }
    }
    
    @SuppressWarnings("unchecked")
    private static OpeningHoursForDay parseOpeningHoursValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return new OpeningHoursForDay();
            }
            
            // Check if all elements are strings (simple format without mixed Map items)
            boolean allStrings = true;
            for (Object item : list) {
                if (!(item instanceof String)) {
                    allStrings = false;
                    break;
                }
            }
            
            if (allStrings) {
                // Simple time range format
                List<String> timeRanges = new ArrayList<>();
                for (Object item : list) {
                    timeRanges.add((String) item);
                }
                return OpeningHoursForDay.fromStrings(timeRanges);
            }
            
            // Handle mixed formats (strings and maps)
            List<TimeRange> ranges = new ArrayList<>();
            Object dayData = null;
            
            for (Object item : list) {
                if (item instanceof String) {
                    ranges.add(TimeRange.fromString((String) item));
                } else if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    if (map.containsKey("hours")) {
                        Object hours = map.get("hours");
                        if (hours instanceof String) {
                            Object itemData = map.get("data");
                            ranges.add(TimeRange.fromString((String) hours, itemData));
                        } else if (hours instanceof List) {
                            for (Object h : (List<?>) hours) {
                                if (h instanceof String) {
                                    ranges.add(TimeRange.fromString((String) h));
                                }
                            }
                        }
                    }
                    if (map.containsKey("data") && dayData == null) {
                        dayData = map.get("data");
                    }
                }
            }
            
            return new OpeningHoursForDay(ranges, dayData);
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Object hours = map.get("hours");
            Object data = map.get("data");
            
            if (hours == null) {
                return new OpeningHoursForDay(new ArrayList<>(), data);
            }
            
            if (hours instanceof List) {
                List<String> timeRanges = new ArrayList<>();
                for (Object item : (List<?>) hours) {
                    if (item instanceof String) {
                        timeRanges.add((String) item);
                    }
                }
                return OpeningHoursForDay.fromStrings(timeRanges, data);
            } else if (hours instanceof String) {
                return OpeningHoursForDay.fromStrings(Arrays.asList((String) hours), data);
            }
            
            return new OpeningHoursForDay(new ArrayList<>(), data);
        }
        
        return new OpeningHoursForDay();
    }
    
    /**
     * Fill opening hours with data (non-static version of create)
     * Note: This returns a new instance rather than modifying the existing one (immutability)
     * 
     * @param data the opening hours data map
     * @return a new OpeningHours instance
     */
    public OpeningHours fill(Map<String, Object> data) {
        return create(data, this.timezone, this.outputTimezone);
    }
    
    /**
     * Check if open on a specific day name or date string
     * Supports day names (e.g., "monday") and date strings (e.g., "2020-09-03", "09-03")
     * 
     * @param day the day name or date string
     * @return true if open on that day/date
     */
    public boolean isOpenOn(String day) {
        // Try to parse as day name first
        try {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
            OpeningHoursForDay hours = openingHours.get(dayOfWeek);
            return hours != null && !hours.isEmpty();
        } catch (IllegalArgumentException e) {
            // Try parsing as date
            try {
                java.time.LocalDate date;
                if (day.length() == 5 && day.charAt(2) == '-') {
                    // MM-dd format, use current year
                    date = java.time.LocalDate.parse(java.time.Year.now() + "-" + day);
                } else {
                    // Full date format
                    date = java.time.LocalDate.parse(day);
                }
                
                OpeningHoursForDay hours = forDate(date.atStartOfDay());
                return hours != null && !hours.isEmpty();
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    /**
     * Check if closed on a specific day name
     * 
     * @param day the day name or date string
     * @return true if closed on that day/date
     */
    public boolean isClosedOn(String day) {
        return !isOpenOn(day);
    }
    
    /**
     * Check if open at specific date/time
     * 
     * @param dateTime the date and time to check
     * @return true if open at that date/time
     */
    public boolean isOpenAt(LocalDateTime dateTime) {
        OpeningHoursForDay day = forDate(dateTime);
        return day.isOpenAt(dateTime.toLocalTime());
    }
    
    /**
     * Check if closed at specific date/time
     * 
     * @param dateTime the date and time to check
     * @return true if closed at that date/time
     */
    public boolean isClosedAt(LocalDateTime dateTime) {
        return !isOpenAt(dateTime);
    }
    
    /**
     * Check if open right now
     * 
     * @return true if currently open
     */
    public boolean isOpen() {
        return isOpenAt(LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault()));
    }
    
    /**
     * Check if closed right now
     * 
     * @return true if currently closed
     */
    public boolean isClosed() {
        return !isOpen();
    }
    
    /**
     * Check if always open (24/7)
     * 
     * @return true if open 24/7 with no exceptions or filters
     */
    public boolean isAlwaysOpen() {
        // Must have no exceptions and no filters
        if (!exceptions.isEmpty() || !filters.isEmpty()) {
            return false;
        }
        
        // Check if all days have 24-hour coverage
        for (DayOfWeek day : DayOfWeek.values()) {
            OpeningHoursForDay hours = openingHours.get(day);
            if (hours == null || hours.isEmpty()) {
                return false;
            }
            
            // Check if covers full 24 hours (00:00-24:00 or equivalent)
            boolean coversFullDay = false;
            for (TimeRange range : hours.getTimeRanges()) {
                // Check if it's a 00:00-24:00 range or similar full-day coverage
                if (range.start().toLocalTime().equals(java.time.LocalTime.MIN) && 
                    (range.end().toLocalTime().equals(java.time.LocalTime.MAX) ||
                     range.end().toString().equals("24:00"))) {
                    coversFullDay = true;
                    break;
                }
            }
            
            if (!coversFullDay) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if always closed
     * 
     * @return true if never open with no exceptions or filters
     */
    public boolean isAlwaysClosed() {
        // Must have no exceptions and no filters
        if (!exceptions.isEmpty() || !filters.isEmpty()) {
            return false;
        }
        
        // Check if all days are empty
        for (DayOfWeek day : DayOfWeek.values()) {
            OpeningHoursForDay hours = openingHours.get(day);
            if (hours != null && !hours.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get opening hours for a specific day name
     * 
     * @param day the day name (e.g., "monday", "tuesday")
     * @return the opening hours for that day
     */
    public OpeningHoursForDay forDay(String day) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
        return openingHours.get(dayOfWeek);
    }
    
    /**
     * Get opening hours for a specific date
     * Considers exceptions and filters
     * 
     * @param dateTime the date to check
     * @return the opening hours for that date
     */
    public OpeningHoursForDay forDate(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        
        // Check filters first
        for (Function<LocalDate, Map<String, Object>> filter : filters) {
            Map<String, Object> result = filter.apply(date);
            if (result != null) {
                // TODO: Parse result into OpeningHoursForDay
            }
        }
        
        // Check exceptions
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (exceptions.containsKey(dateKey)) {
            return exceptions.get(dateKey);
        }
        
        // Check recurring exceptions (MM-dd format)
        String recurringKey = date.format(DateTimeFormatter.ofPattern("MM-dd"));
        if (exceptions.containsKey(recurringKey)) {
            return exceptions.get(recurringKey);
        }
        
        // Return regular schedule
        return openingHours.get(date.getDayOfWeek());
    }
    
    /**
     * Get opening hours for the whole week
     * 
     * @return a map of day names to opening hours, containing all 7 days
     */
    public Map<String, OpeningHoursForDay> forWeek() {
        Map<String, OpeningHoursForDay> week = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.put(day.name().toLowerCase(), openingHours.get(day));
        }
        return week;
    }
    
    /**
     * Get combined opening hours (days with same hours grouped)
     * 
     * @return a map with first day as key, containing days list and hours
     */
    public Map<String, Object> forWeekCombined() {
        Map<String, Object> combined = new LinkedHashMap<>();
        Map<String, List<String>> groups = new HashMap<>();
        
        // Group days by their opening hours
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.name().toLowerCase();
            OpeningHoursForDay hours = openingHours.get(day);
            String hoursKey = hours.toString(); // Use string representation as key
            
            groups.computeIfAbsent(hoursKey, k -> new ArrayList<>()).add(dayName);
        }
        
        // Create result with first day as key
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            List<String> days = entry.getValue();
            if (!days.isEmpty()) {
                String firstDay = days.get(0);
                Map<String, Object> value = new HashMap<>();
                value.put("days", days);
                value.put("hours", openingHours.get(DayOfWeek.valueOf(firstDay.toUpperCase())));
                combined.put(firstDay, value);
            }
        }
        
        return combined;
    }
    
    /**
     * Get consecutive days with same opening hours
     * 
     * @return a map with first day as key, containing consecutive days list and hours
     */
    public Map<String, Object> forWeekConsecutiveDays() {
        Map<String, Object> consecutive = new LinkedHashMap<>();
        
        List<DayOfWeek> days = Arrays.asList(DayOfWeek.values());
        int i = 0;
        
        while (i < days.size()) {
            DayOfWeek startDay = days.get(i);
            OpeningHoursForDay startHours = openingHours.get(startDay);
            List<String> group = new ArrayList<>();
            group.add(startDay.name().toLowerCase());
            
            // Find consecutive days with same hours
            int j = i + 1;
            while (j < days.size()) {
                DayOfWeek nextDay = days.get(j);
                OpeningHoursForDay nextHours = openingHours.get(nextDay);
                
                if (startHours.toString().equals(nextHours.toString())) {
                    group.add(nextDay.name().toLowerCase());
                    j++;
                } else {
                    break;
                }
            }
            
            Map<String, Object> value = new HashMap<>();
            value.put("days", group);
            value.put("hours", startHours);
            consecutive.put(startDay.name().toLowerCase(), value);
            
            i = j;
        }
        
        return consecutive;
    }
    
    /**
     * Get all exceptions
     * 
     * @return a map of date strings to opening hours for exceptions
     */
    public Map<String, OpeningHoursForDay> exceptions() {
        return new HashMap<>(exceptions);
    }
    
    /**
     * Get next open date/time from the given date/time
     * 
     * @param dateTime the starting date/time (null for now)
     * @return the next open date/time
     */
    public LocalDateTime nextOpen(LocalDateTime dateTime) {
        return nextOpen(dateTime, null, null);
    }
    
    /**
     * Get next open date/time with search limits
     * 
     * @param dateTime the starting date/time (null for now)
     * @param searchUntil throw exception if not found before this time
     * @param cap return this time if not found before it
     * @return the next open date/time
     */
    public LocalDateTime nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
        }
        
        LocalDateTime current = dateTime;
        int daysSearched = 0;
        final int MAX_DAYS = 366;
        
        while (daysSearched < MAX_DAYS) {
            OpeningHoursForDay day = forDate(current);
            LocalTime currentTime = current.toLocalTime();
            
            // Find next opening time that starts AFTER current time
            for (TimeRange range : day.getTimeRanges()) {
                LocalTime startTime = range.start().toLocalTime();
                
                // Only consider ranges that start after current time
                // This ensures we get the NEXT opening, not the current one
                if (startTime.isAfter(currentTime)) {
                    LocalDateTime result = current.toLocalDate().atTime(startTime);
                    if (searchUntil != null && result.isAfter(searchUntil)) {
                        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                            "No open time found before " + searchUntil);
                    }
                    if (cap != null && result.isAfter(cap)) {
                        return cap;
                    }
                    return result;
                }
            }
            
            // Move to next day at midnight
            current = current.toLocalDate().plusDays(1).atStartOfDay();
            daysSearched++;
        }
        
        if (searchUntil != null) {
            throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                "No open time found before " + searchUntil);
        }
        if (cap != null) {
            return cap;
        }
        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
            "No open time found within " + MAX_DAYS + " days");
    }
    
    /**
     * Get next close date/time from the given date/time
     * 
     * @param dateTime the starting date/time (null for now)
     * @return the next close date/time
     */
    public LocalDateTime nextClose(LocalDateTime dateTime) {
        return nextClose(dateTime, null, null);
    }
    
    /**
     * Get next close date/time with search limits
     * 
     * @param dateTime the starting date/time (null for now)
     * @param searchUntil throw exception if not found before this time
     * @param cap return this time if not found before it
     * @return the next close date/time
     */
    public LocalDateTime nextClose(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
        }
        
        LocalDateTime current = dateTime;
        int daysSearched = 0;
        final int MAX_DAYS = 366;
        
        while (daysSearched < MAX_DAYS) {
            OpeningHoursForDay day = forDate(current);
            LocalTime currentTime = current.toLocalTime();
            
            // If currently in an open range, return the end of that range
            for (TimeRange range : day.getTimeRanges()) {
                if (range.containsTime(currentTime)) {
                    LocalTime endTime = range.end().toLocalTime();
                    LocalDateTime result = current.toLocalDate().atTime(endTime);
                    
                    // Handle overnight ranges
                    if (endTime.isBefore(range.start().toLocalTime()) || endTime.equals(LocalTime.MAX)) {
                        result = result.plusDays(1);
                    }
                    
                    if (searchUntil != null && result.isAfter(searchUntil)) {
                        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                            "No close time found before " + searchUntil);
                    }
                    if (cap != null && result.isAfter(cap)) {
                        return cap;
                    }
                    return result;
                }
            }
            
            // Currently closed - find the end of next open range on this or future days
            for (TimeRange range : day.getTimeRanges()) {
                LocalTime startTime = range.start().toLocalTime();
                LocalTime endTime = range.end().toLocalTime();
                
                // If this range starts after or at current time (for future days, startTime can be <= currentTime)
                if (startTime.isAfter(currentTime) || current.toLocalDate().isAfter(dateTime.toLocalDate())) {
                    LocalDateTime result = current.toLocalDate().atTime(endTime);
                    
                    // Handle overnight ranges
                    if (endTime.isBefore(startTime) || endTime.equals(LocalTime.MAX)) {
                        result = result.plusDays(1);
                    }
                    
                    if (searchUntil != null && result.isAfter(searchUntil)) {
                        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                            "No close time found before " + searchUntil);
                    }
                    if (cap != null && result.isAfter(cap)) {
                        return cap;
                    }
                    return result;
                }
            }
            
            // Move to next day at midnight
            current = current.toLocalDate().plusDays(1).atStartOfDay();
            daysSearched++;
        }
        
        if (searchUntil != null) {
            throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                "No close time found before " + searchUntil);
        }
        if (cap != null) {
            return cap;
        }
        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
            "No close time found within " + MAX_DAYS + " days");
    }
    
    /**
     * Get previous open date/time from the given date/time
     * 
     * @param dateTime the starting date/time (null for now)
     * @return the previous open date/time
     */
    public LocalDateTime previousOpen(LocalDateTime dateTime) {
        return previousOpen(dateTime, null, null);
    }
    
    /**
     * Get previous open date/time with search limits
     * 
     * @param dateTime the starting date/time (null for now)
     * @param searchUntil throw exception if not found after this time
     * @param cap return this time if not found after it
     * @return the previous open date/time
     */
    public LocalDateTime previousOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
        }
        
        LocalDateTime current = dateTime;
        int daysSearched = 0;
        final int MAX_DAYS = 366;
        
        while (daysSearched < MAX_DAYS) {
            OpeningHoursForDay day = forDate(current);
            LocalTime currentTime = current.toLocalTime();
            
            // Look for opening time on this day (in reverse)
            List<TimeRange> ranges = day.getTimeRanges();
            for (int i = ranges.size() - 1; i >= 0; i--) {
                TimeRange range = ranges.get(i);
                LocalTime startTime = range.start().toLocalTime();
                
                // Find previous opening that starts before current time
                if (currentTime.isAfter(startTime)) {
                    LocalDateTime result = current.toLocalDate().atTime(startTime);
                    
                    if (searchUntil != null && result.isBefore(searchUntil)) {
                        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                            "No open time found after " + searchUntil);
                    }
                    if (cap != null && result.isBefore(cap)) {
                        return cap;
                    }
                    return result;
                }
            }
            
            // Move to previous day at end of day
            current = current.toLocalDate().minusDays(1).atTime(23, 59, 59);
            daysSearched++;
        }
        
        if (searchUntil != null) {
            throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                "No open time found after " + searchUntil);
        }
        if (cap != null) {
            return cap;
        }
        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
            "No open time found within " + MAX_DAYS + " days");
    }
    
    /**
     * Get previous close date/time from the given date/time
     * 
     * @param dateTime the starting date/time (null for now)
     * @return the previous close date/time
     */
    public LocalDateTime previousClose(LocalDateTime dateTime) {
        return previousClose(dateTime, null, null);
    }
    
    /**
     * Get previous close date/time with search limits
     * 
     * @param dateTime the starting date/time (null for now)
     * @param searchUntil throw exception if not found after this time
     * @param cap return this time if not found after it
     * @return the previous close date/time
     */
    public LocalDateTime previousClose(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
        }
        
        LocalDateTime current = dateTime;
        int daysSearched = 0;
        final int MAX_DAYS = 366;
        
        while (daysSearched < MAX_DAYS) {
            OpeningHoursForDay day = forDate(current);
            LocalTime currentTime = current.toLocalTime();
            
            // Look for close times on this day (in reverse)
            List<TimeRange> ranges = day.getTimeRanges();
            for (int i = ranges.size() - 1; i >= 0; i--) {
                TimeRange range = ranges.get(i);
                LocalTime endTime = range.end().toLocalTime();
                
                // Find previous close that ends before current time
                if (currentTime.isAfter(endTime)) {
                    LocalDateTime result = current.toLocalDate().atTime(endTime);
                    
                    // Handle overnight ranges
                    if (endTime.isBefore(range.start().toLocalTime()) || endTime.equals(LocalTime.MAX)) {
                        result = result.plusDays(1);
                    }
                    
                    if (searchUntil != null && result.isBefore(searchUntil)) {
                        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                            "No close time found after " + searchUntil);
                    }
                    if (cap != null && result.isBefore(cap)) {
                        return cap;
                    }
                    return result;
                }
            }
            
            // Move to previous day
            current = current.toLocalDate().minusDays(1).atTime(23, 59, 59);
            daysSearched++;
        }
        
        if (searchUntil != null) {
            throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
                "No close time found after " + searchUntil);
        }
        if (cap != null) {
            return cap;
        }
        throw new io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded(
            "No close time found within " + MAX_DAYS + " days");
    }
    
    /**
     * Get current open range at the given date/time
     * 
     * @param dateTime the date/time to check (null for now)
     * @return Optional containing the current open TimeRange, or empty if closed
     */
    public Optional<TimeRange> currentOpenRange(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
        }
        
        OpeningHoursForDay day = forDate(dateTime);
        LocalTime time = dateTime.toLocalTime();
        
        for (TimeRange range : day.getTimeRanges()) {
            if (range.containsTime(time)) {
                return Optional.of(range);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get current open range start time
     * 
     * @param dateTime the date/time to check (null for now)
     * @return Optional containing the start date/time, or empty if closed
     */
    public Optional<LocalDateTime> currentOpenRangeStart(LocalDateTime dateTime) {
        Optional<TimeRange> range = currentOpenRange(dateTime);
        if (range.isPresent()) {
            LocalDateTime start = dateTime.toLocalDate().atTime(range.get().start().toLocalTime());
            return Optional.of(start);
        }
        return Optional.empty();
    }
    
    /**
     * Get current open range end time
     * 
     * @param dateTime the date/time to check (null for now)
     * @return Optional containing the end date/time, or empty if closed
     */
    public Optional<LocalDateTime> currentOpenRangeEnd(LocalDateTime dateTime) {
        Optional<TimeRange> range = currentOpenRange(dateTime);
        if (range.isPresent()) {
            LocalTime endTime = range.get().end().toLocalTime();
            LocalDateTime end = dateTime.toLocalDate().atTime(endTime);
            
            // Handle overnight ranges
            LocalTime startTime = range.get().start().toLocalTime();
            if (endTime.isBefore(startTime) || endTime.equals(java.time.LocalTime.MAX)) {
                end = end.plusDays(1);
            }
            
            return Optional.of(end);
        }
        return Optional.empty();
    }
    
    /**
     * Calculate difference in open hours between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of open hours as a floating point number
     */
    public double diffInOpenHours(LocalDateTime start, LocalDateTime end) {
        return diffInOpenMinutes(start, end) / 60.0;
    }
    
    /**
     * Calculate difference in open minutes between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of open minutes as a floating point number
     */
    public double diffInOpenMinutes(LocalDateTime start, LocalDateTime end) {
        return diffInOpenSeconds(start, end) / 60.0;
    }
    
    /**
     * Calculate difference in open seconds between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of open seconds as a floating point number
     */
    public double diffInOpenSeconds(LocalDateTime start, LocalDateTime end) {
        return diffInSeconds(true, start, end);
    }
    
    /**
     * Calculate difference in closed hours between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of closed hours as a floating point number
     */
    public double diffInClosedHours(LocalDateTime start, LocalDateTime end) {
        return diffInClosedMinutes(start, end) / 60.0;
    }
    
    /**
     * Calculate difference in closed minutes between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of closed minutes as a floating point number
     */
    public double diffInClosedMinutes(LocalDateTime start, LocalDateTime end) {
        return diffInClosedSeconds(start, end) / 60.0;
    }
    
    /**
     * Calculate difference in closed seconds between two date/times
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return the number of closed seconds as a floating point number
     */
    public double diffInClosedSeconds(LocalDateTime start, LocalDateTime end) {
        return diffInSeconds(false, start, end);
    }
    
    private double diffInSeconds(boolean countOpen, LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            return -diffInSeconds(countOpen, end, start);
        }
        
        double totalSeconds = 0.0;
        LocalDateTime current = start;
        
        while (current.isBefore(end)) {
            LocalDateTime dayEnd = current.toLocalDate().plusDays(1).atStartOfDay();
            LocalDateTime segmentEnd = end.isBefore(dayEnd) ? end : dayEnd;
            
            OpeningHoursForDay day = forDate(current);
            LocalTime currentTime = current.toLocalTime();
            LocalTime segmentEndTime = segmentEnd.toLocalTime();
            
            if (segmentEnd.toLocalDate().equals(current.toLocalDate())) {
                // Same day
                totalSeconds += calculateSecondsInDay(day, currentTime, segmentEndTime, countOpen);
            } else {
                // Goes to next day
                totalSeconds += calculateSecondsInDay(day, currentTime, LocalTime.MAX, countOpen);
            }
            
            current = segmentEnd;
        }
        
        return totalSeconds;
    }
    
    private double calculateSecondsInDay(OpeningHoursForDay day, LocalTime start, LocalTime end, boolean countOpen) {
        double seconds = 0.0;
        LocalTime current = start;
        
        while (current.isBefore(end)) {
            boolean isOpen = day.isOpenAt(current);
            
            if (isOpen == countOpen) {
                // Find end of current state (open or closed)
                LocalTime stateEnd = end;
                
                for (TimeRange range : day.getTimeRanges()) {
                    LocalTime rangeStart = range.start().toLocalTime();
                    LocalTime rangeEnd = range.end().toLocalTime();
                    
                    if (countOpen && range.containsTime(current)) {
                        // Currently in open range, find end
                        stateEnd = rangeEnd.isBefore(end) ? rangeEnd : end;
                        break;
                    } else if (!countOpen && current.isBefore(rangeStart) && rangeStart.isBefore(end)) {
                        // Currently in closed period before this range
                        stateEnd = rangeStart;
                        break;
                    }
                }
                
                seconds += java.time.Duration.between(current, stateEnd).getSeconds();
                current = stateEnd;
            } else {
                // Skip to next state change
                LocalTime nextChange = end;
                
                for (TimeRange range : day.getTimeRanges()) {
                    LocalTime rangeStart = range.start().toLocalTime();
                    LocalTime rangeEnd = range.end().toLocalTime();
                    
                    if (countOpen) {
                        // Looking for open time - find next range start
                        if (rangeStart.isAfter(current) && rangeStart.isBefore(nextChange)) {
                            nextChange = rangeStart;
                        }
                    } else {
                        // Looking for closed time - find next range end
                        if (range.containsTime(current) && rangeEnd.isBefore(nextChange)) {
                            nextChange = rangeEnd;
                        }
                    }
                }
                
                current = nextChange;
            }
            
            // Prevent infinite loop
            if (current.equals(start)) {
                current = current.plusMinutes(1);
            }
        }
        
        return seconds;
    }
    
    /**
     * Merge overlapping time ranges in the schedule
     * 
     * @param schedule the schedule map with time range strings
     * @return a new schedule map with merged ranges
     */
    public static Map<String, List<String>> mergeOverlappingRanges(Map<String, List<String>> schedule) {
        Map<String, List<String>> result = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : schedule.entrySet()) {
            String day = entry.getKey();
            List<String> ranges = entry.getValue();
            
            if (ranges.isEmpty()) {
                result.put(day, new ArrayList<>());
                continue;
            }
            
            // Parse all ranges
            List<TimeRange> timeRanges = new ArrayList<>();
            for (String rangeStr : ranges) {
                timeRanges.add(TimeRange.fromString(rangeStr));
            }
            
            // Sort by start time
            timeRanges.sort((a, b) -> a.start().toLocalTime().compareTo(b.start().toLocalTime()));
            
            // Merge overlapping ranges
            List<TimeRange> merged = new ArrayList<>();
            TimeRange current = timeRanges.get(0);
            
            for (int i = 1; i < timeRanges.size(); i++) {
                TimeRange next = timeRanges.get(i);
                
                // Check if ranges overlap or are adjacent
                if (current.overlaps(next) || 
                    current.end().toLocalTime().equals(next.start().toLocalTime())) {
                    // Merge ranges
                    LocalTime newEnd = current.end().toLocalTime().isAfter(next.end().toLocalTime()) ?
                        current.end().toLocalTime() : next.end().toLocalTime();
                    current = new TimeRange(current.start(), Time.fromString(newEnd.toString()));
                } else {
                    merged.add(current);
                    current = next;
                }
            }
            merged.add(current);
            
            // Convert back to strings
            List<String> mergedStrings = new ArrayList<>();
            for (TimeRange range : merged) {
                mergedStrings.add(range.toString());
            }
            
            result.put(day, mergedStrings);
        }
        
        return result;
    }
    
    /**
     * Create from schedule and merge overlapping ranges
     * 
     * @param schedule the schedule map with potentially overlapping ranges
     * @return a new OpeningHours instance with merged ranges
     */
    public static OpeningHours createAndMergeOverlappingRanges(Map<String, List<String>> schedule) {
        Map<String, List<String>> merged = mergeOverlappingRanges(schedule);
        Map<String, Object> data = new HashMap<>();
        data.putAll(merged);
        return create(data);
    }
    
    /**
     * Create from structured data (schema.org format)
     * 
     * @param jsonData the JSON string in schema.org OpeningHoursSpecification format
     * @return a new OpeningHours instance
     */
    public static OpeningHours createFromStructuredData(String jsonData) {
        return createFromStructuredData(jsonData, null, null);
    }
    
    /**
     * Create from structured data with timezone
     * 
     * @param jsonData the JSON string in schema.org format
     * @param timezone the input timezone
     * @param outputTimezone the output timezone
     * @return a new OpeningHours instance
     */
    public static OpeningHours createFromStructuredData(String jsonData, ZoneId timezone, ZoneId outputTimezone) {
        // TODO: Implement JSON parsing (requires JSON library like Gson or Jackson)
        return create(new HashMap<>(), timezone, outputTimezone);
    }
    
    /**
     * Convert to structured data (schema.org format)
     * 
     * @return a list of OpeningHoursSpecification maps
     */
    public List<Map<String, Object>> asStructuredData() {
        return asStructuredData("HH:mm", null);
    }
    
    /**
     * Convert to structured data with custom time format
     * 
     * @param format the time format pattern (e.g., "HH:mm", "HH:mm:ss")
     * @return a list of OpeningHoursSpecification maps
     */
    public List<Map<String, Object>> asStructuredData(String format) {
        return asStructuredData(format, null);
    }
    
    /**
     * Convert to structured data with custom format and timezone
     * 
     * @param format the time format pattern
     * @param timezone the timezone to use
     * @return a list of OpeningHoursSpecification maps
     */
    public List<Map<String, Object>> asStructuredData(String format, ZoneId timezone) {
        // TODO: Implement conversion to schema.org format
        return new ArrayList<>();
    }
}
