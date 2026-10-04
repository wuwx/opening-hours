package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidDate;
import io.github.wuwx.openinghours.exceptions.InvalidDateRange;
import io.github.wuwx.openinghours.exceptions.InvalidDateTimeClass;
import io.github.wuwx.openinghours.exceptions.InvalidDayName;
import io.github.wuwx.openinghours.exceptions.InvalidTimezone;
import io.github.wuwx.openinghours.exceptions.MaximumLimitExceeded;
import io.github.wuwx.openinghours.exceptions.SearchLimitReached;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The opening hours of a business: a set of weekly hours, optional exceptions, and optional filters.
 *
 * <p>An instance is built from a definition map whose keys are english day names (or ranges such as
 * {@code "monday to friday"}), plus the reserved keys {@code exceptions}, {@code data},
 * {@code filters}, {@code overflow} and {@code timezone}:</p>
 *
 * <pre>{@code
 * Map<String, Object> data = new HashMap<>();
 * data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
 * OpeningHours openingHours = OpeningHours.create(data);
 * }</pre>
 *
 * <p>Note that a {@code LocalDateTime} is interpreted as wall-clock time in the configured input
 * timezone; use the {@link ZonedDateTime} overloads when the timezone conversion matters.</p>
 */
public class OpeningHours {

    /** The default number of days searched before giving up looking for a next open or close time. */
    public static final int DEFAULT_DAY_LIMIT = 8;

    private static final Pattern DATE_STRING = Pattern.compile("^(?:(\\d+)-)?(\\d{1,2})-(\\d{1,2})$");
    private static final DateTimeFormatter MM_DD = DateTimeFormatter.ofPattern("MM-dd");
    private static final List<String> DEFAULT_EXCLUDED_KEYS =
            Arrays.asList("data", "dateTimeClass", "filters", "overflow");

    private final Map<DayOfWeek, OpeningHoursForDay> openingHours = new EnumMap<>(DayOfWeek.class);
    private final Map<String, OpeningHoursForDay> exceptions = new LinkedHashMap<>();
    private final List<Function<LocalDateTime, Object>> filters = new ArrayList<>();
    private final Object data;
    private final boolean overflow;
    private ZoneId timezone;
    private ZoneId outputTimezone;
    private Integer dayLimit;

    private OpeningHours(Map<DayOfWeek, OpeningHoursForDay> openingHours,
                         Map<String, OpeningHoursForDay> exceptions,
                         List<Function<LocalDateTime, Object>> filters,
                         Object data,
                         ZoneId timezone,
                         ZoneId outputTimezone,
                         boolean overflow,
                         Integer dayLimit) {
        this.openingHours.putAll(openingHours);
        this.exceptions.putAll(exceptions);
        this.filters.addAll(filters);
        this.data = data;
        this.timezone = timezone;
        this.outputTimezone = outputTimezone;
        this.overflow = overflow;
        this.dayLimit = dayLimit;
    }

    /**
     * Creates an {@code OpeningHours} instance from a definition map.
     *
     * @param data the days, exceptions and options
     * @return the resulting opening hours
     * @throws io.github.wuwx.openinghours.exceptions.InvalidDayName  if a key is not a valid day name
     * @throws io.github.wuwx.openinghours.exceptions.InvalidDate     if an exception key is not a valid date
     * @throws io.github.wuwx.openinghours.exceptions.InvalidDateRange if a definition would override another one
     */
    public static OpeningHours create(Map<String, Object> data) {
        return create(data, null, null);
    }

    /**
     * Creates an {@code OpeningHours} instance using the given timezone for both input and output.
     *
     * @param data     the days, exceptions and options
     * @param timezone the timezone used to interpret and return date times
     * @return the resulting opening hours
     */
    public static OpeningHours create(Map<String, Object> data, ZoneId timezone) {
        return create(data, timezone, timezone);
    }

    /**
     * Creates an {@code OpeningHours} instance with distinct input and output timezones.
     *
     * @param data           the days, exceptions and options
     * @param timezone       the timezone used to interpret the given date times
     * @param outputTimezone the timezone used to return date times
     * @return the resulting opening hours
     */
    @SuppressWarnings("unchecked")
    public static OpeningHours create(Map<String, Object> data, ZoneId timezone, ZoneId outputTimezone) {
        Map<String, Object> definitions = new LinkedHashMap<>(data);

        ZoneId inputTz = timezone;
        ZoneId outputTz = outputTimezone;

        Object timezoneData = definitions.remove("timezone");
        if (timezoneData instanceof String) {
            inputTz = parseTimezone((String) timezoneData);
        } else if (timezoneData instanceof Map) {
            Map<String, Object> timezones = (Map<String, Object>) timezoneData;
            if (timezones.containsKey("input")) {
                inputTz = parseTimezone(String.valueOf(timezones.get("input")));
            }
            if (timezones.containsKey("output")) {
                outputTz = parseTimezone(String.valueOf(timezones.get("output")));
            }
        }

        Object metaData = definitions.remove("data");
        boolean overflow = toBoolean(definitions.remove("overflow"));

        Object dateTimeClass = definitions.remove("dateTimeClass");
        if (dateTimeClass != null) {
            throw InvalidDateTimeClass.forString(String.valueOf(dateTimeClass));
        }

        List<Function<LocalDateTime, Object>> filters = new ArrayList<>();
        Object filtersData = definitions.remove("filters");
        if (filtersData instanceof List) {
            for (Object filter : (List<?>) filtersData) {
                if (filter instanceof Function) {
                    filters.add((Function<LocalDateTime, Object>) filter);
                }
            }
        }

        Map<String, Object> exceptionsData = new LinkedHashMap<>();
        Object exceptionsValue = definitions.remove("exceptions");
        if (exceptionsValue instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) exceptionsValue).entrySet()) {
                if (entry.getValue() instanceof Function) {
                    filters.add((Function<LocalDateTime, Object>) entry.getValue());
                    continue;
                }
                for (String date : readDatesRange(entry.getKey())) {
                    if (exceptionsData.containsKey(date)) {
                        throw InvalidDateRange.invalidDateRange(entry.getKey(), date);
                    }
                    exceptionsData.put(date, entry.getValue());
                }
            }
        }

        Map<DayOfWeek, OpeningHoursForDay> openingHours = new EnumMap<>(DayOfWeek.class);
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            for (String rawDay : readDatesRange(entry.getKey())) {
                DayOfWeek day = normalizeDayName(rawDay);
                if (openingHours.containsKey(day)) {
                    throw InvalidDateRange.invalidDateRange(entry.getKey(), rawDay);
                }
                openingHours.put(day, OpeningHoursForDay.fromStrings(entry.getValue()));
            }
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            openingHours.putIfAbsent(day, new OpeningHoursForDay());
        }

        Integer dayLimit = null;
        Map<String, OpeningHoursForDay> exceptions = new LinkedHashMap<>();
        if (!exceptionsData.isEmpty()) {
            dayLimit = 366;
            for (Map.Entry<String, Object> entry : exceptionsData.entrySet()) {
                if (!isValidDateKey(entry.getKey())) {
                    throw InvalidDate.invalidDate(entry.getKey());
                }
                exceptions.put(entry.getKey(), OpeningHoursForDay.fromStrings(entry.getValue()));
            }
        }

        return new OpeningHours(openingHours, exceptions, filters, metaData, inputTz, outputTz, overflow, dayLimit);
    }

    /**
     * Checks whether the given definition can be turned into an {@code OpeningHours} instance.
     *
     * @param data the definition to validate
     * @return true if {@link #create(Map)} would succeed, false if it would throw
     */
    public static boolean isValid(Map<String, Object> data) {
        try {
            create(data);
            return true;
        } catch (io.github.wuwx.openinghours.exceptions.Exception e) {
            return false;
        }
    }

    /**
     * Sets the timezone used to interpret date times.
     *
     * @param timezone the input timezone
     * @return this instance, for chaining
     */
    public OpeningHours setTimezone(ZoneId timezone) {
        this.timezone = timezone;
        return this;
    }

    /**
     * Sets the timezone used to return date times.
     *
     * @param outputTimezone the output timezone
     * @return this instance, for chaining
     */
    public OpeningHours setOutputTimezone(ZoneId outputTimezone) {
        this.outputTimezone = outputTimezone;
        return this;
    }

    /** @return the timezone used to interpret date times, or null when none was configured */
    public ZoneId getTimezone() {
        return timezone;
    }

    /** @return the timezone used to return date times, or null when none was configured */
    public ZoneId getOutputTimezone() {
        return outputTimezone;
    }

    /**
     * Sets how many days a next or previous search may span before giving up.
     *
     * @param dayLimit the maximum number of days to search
     * @return this instance, for chaining
     */
    public OpeningHours setDayLimit(int dayLimit) {
        this.dayLimit = dayLimit;
        return this;
    }

    /**
     * @return the number of days a next or previous search may span; 8 by default, or 366 when the
     * definition holds exceptions
     */
    public int getDayLimit() {
        return dayLimit != null ? dayLimit : DEFAULT_DAY_LIMIT;
    }

    /**
     * Returns a copy of the filters, so callers may modify it freely without altering this instance,
     * as PHP arrays behave in the original library.
     *
     * @return the filters passed with the {@code filters} key
     */
    public List<Function<LocalDateTime, Object>> getFilters() {
        return new ArrayList<>(filters);
    }

    /** @return the metadata passed with the {@code data} key, or null when there is none */
    public Object data() {
        return data;
    }

    /**
     * @return the metadata passed with the {@code data} key, or null when there is none
     * @deprecated use {@link #data()} instead; the original library deprecates {@code getData()} in
     * favour of the {@code data} property, and this method is only kept for compatibility
     */
    @Deprecated
    public Object getData() {
        return data();
    }

    /**
     * Returns the regular week, day by day.
     *
     * @return the seven days of the week, keyed by lowercase english day name
     */
    public Map<String, OpeningHoursForDay> forWeek() {
        Map<String, OpeningHoursForDay> week = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.put(day.name().toLowerCase(), openingHours.get(day));
        }
        return week;
    }

    /**
     * Returns the regular week, with the days sharing the same hours grouped together.
     *
     * <p>Each group is keyed by the first day name of the group and holds a {@code days} list plus
     * the shared {@code opening_hours}.</p>
     *
     * @return the groups of days sharing the same hours
     */
    public Map<String, Object> forWeekCombined() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.name().toLowerCase();
            String hoursKey = openingHours.get(day).toString();
            groups.computeIfAbsent(hoursKey, k -> new ArrayList<>()).add(dayName);
        }
        Map<String, Object> combined = new LinkedHashMap<>();
        for (List<String> days : groups.values()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("days", days);
            value.put("opening_hours", openingHours.get(DayOfWeek.valueOf(days.get(0).toUpperCase())));
            combined.put(days.get(0), value);
        }
        return combined;
    }

    /**
     * Returns the regular week, grouped into consecutive days sharing the same hours.
     *
     * <p>Each group is keyed by the first day name of the group and holds a {@code days} list plus
     * the shared {@code opening_hours}.</p>
     *
     * @return the consecutive groups of days sharing the same hours
     */
    public Map<String, Object> forWeekConsecutiveDays() {
        Map<String, Object> consecutive = new LinkedHashMap<>();
        List<String> currentDays = null;
        OpeningHoursForDay currentHours = null;
        for (DayOfWeek day : DayOfWeek.values()) {
            OpeningHoursForDay hours = openingHours.get(day);
            String dayName = day.name().toLowerCase();
            if (currentHours != null && currentHours.toString().equals(hours.toString())) {
                currentDays.add(dayName);
                continue;
            }
            currentDays = new ArrayList<>();
            currentDays.add(dayName);
            currentHours = hours;
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("days", currentDays);
            value.put("opening_hours", hours);
            consecutive.put(dayName, value);
        }
        return consecutive;
    }

    /**
     * Returns the regular hours of a day.
     *
     * @param day a lowercase english day name, or a day range such as {@code "monday to friday"}
     * @return the hours of that day
     * @throws io.github.wuwx.openinghours.exceptions.InvalidDayName if the name is not valid
     */
    public OpeningHoursForDay forDay(String day) {
        return openingHours.get(normalizeDayName(day));
    }

    /**
     * Returns the regular hours of a day.
     *
     * @param day the day of the week
     * @return the hours of that day
     */
    public OpeningHoursForDay forDay(DayOfWeek day) {
        return openingHours.get(day);
    }

    /**
     * Returns the hours of a specific date, taking filters, exceptions and the regular week into
     * account.
     *
     * @param date the date to look up
     * @return the hours of that date
     */
    public OpeningHoursForDay forDate(LocalDate date) {
        return forDate(date.atStartOfDay());
    }

    /**
     * Returns the hours of the date of the given date time.
     *
     * <p>Filters are evaluated first, then exceptions (full date, then recurring {@code MM-dd}), then
     * the regular week.</p>
     *
     * @param dateTime the date time to look up
     * @return the hours of that date
     */
    public OpeningHoursForDay forDate(LocalDateTime dateTime) {
        for (Function<LocalDateTime, Object> filter : filters) {
            Object result = filter.apply(dateTime);
            if (result != null) {
                return OpeningHoursForDay.fromStrings(result);
            }
        }
        LocalDate date = dateTime.toLocalDate();
        OpeningHoursForDay day = exceptions.get(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (day == null) {
            day = exceptions.get(date.format(MM_DD));
        }
        if (day == null) {
            day = openingHours.get(date.getDayOfWeek());
        }
        return day;
    }

    /**
     * Returns the hours of the date of the given date time, converted to the input timezone first.
     *
     * @param dateTime the date time to look up
     * @return the hours of that date
     */
    public OpeningHoursForDay forDate(ZonedDateTime dateTime) {
        return forDate(applyTimezone(dateTime).toLocalDateTime());
    }

    /**
     * Returns every range covering the given date time, on its own day and, for reversed ranges,
     * on the previous day.
     *
     * @param dateTime the date time to look up
     * @return the matching ranges, the previous day's after-midnight ranges first
     */
    public List<TimeRange> forDateTime(LocalDateTime dateTime) {
        List<TimeRange> result = new ArrayList<>();
        result.addAll(forDate(dateTime.minusDays(1)).forNightTime(Time.fromDateTime(dateTime)));
        result.addAll(forDate(dateTime).forTime(Time.fromDateTime(dateTime)));
        return result;
    }

    /**
     * Returns every range covering the given date time, converted to the input timezone first.
     *
     * @param dateTime the date time to look up, defaults to now when null
     * @return the matching ranges, the previous day's after-midnight ranges first
     */
    public List<TimeRange> forDateTime(ZonedDateTime dateTime) {
        return forDateTime(applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now()).toLocalDateTime());
    }

    /**
     * Returns a copy of the exceptions, so callers may modify it freely without altering this
     * instance, as PHP arrays behave in the original library.
     *
     * @return the exceptions, keyed by date ({@code yyyy-MM-dd}) or by recurring day ({@code MM-dd})
     * or date range
     */
    public Map<String, OpeningHoursForDay> exceptions() {
        return new LinkedHashMap<>(exceptions);
    }

    /**
     * Checks whether the business opens on the given day or date.
     *
     * @param day a lowercase english day name, or a date such as {@code 2016-12-25} or {@code 12-25}
     * @return true if at least one range is defined for that day
     * @throws io.github.wuwx.openinghours.exceptions.InvalidDayName if the name is not a valid day
     */
    public boolean isOpenOn(String day) {
        Matcher matcher = DATE_STRING.matcher(day);
        if (matcher.matches()) {
            int year = matcher.group(1) != null
                    ? Integer.parseInt(matcher.group(1))
                    : Year.now().getValue();
            LocalDateTime date = LocalDateTime.of(
                    year,
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    0, 0);
            return !forDate(date).isEmpty();
        }
        return !forDay(day).isEmpty();
    }

    /**
     * Checks whether the business is closed on the given day or date.
     *
     * @param day a lowercase english day name, or a date such as {@code 2016-12-25} or {@code 12-25}
     * @return true if no range is defined for that day
     */
    public boolean isClosedOn(String day) {
        return !isOpenOn(day);
    }

    /**
     * Checks whether the business is open at the given date time.
     *
     * <p>When overflow is enabled, the after-midnight part of the previous day's reversed ranges is
     * taken into account.</p>
     *
     * @param dateTime the date time to check
     * @return true if the business is open at that moment
     */
    public boolean isOpenAt(LocalDateTime dateTime) {
        if (overflow) {
            OpeningHoursForDay dayBefore = forDate(dateTime.minusDays(1));
            if (dayBefore.isOpenAtNight(Time.fromDateTime(dateTime))) {
                return true;
            }
        }
        return forDate(dateTime).isOpenAt(Time.fromDateTime(dateTime));
    }

    /**
     * Checks whether the business is closed at the given date time.
     *
     * @param dateTime the date time to check
     * @return true if the business is closed at that moment
     */
    public boolean isClosedAt(LocalDateTime dateTime) {
        return !isOpenAt(dateTime);
    }

    /**
     * Checks whether the business is open at the given date time, converted to the input timezone.
     *
     * @param dateTime the date time to check
     * @return true if the business is open at that moment
     */
    public boolean isOpenAt(ZonedDateTime dateTime) {
        return isOpenAt(applyTimezone(dateTime).toLocalDateTime());
    }

    /**
     * Checks whether the business is closed at the given date time, converted to the input timezone.
     *
     * @param dateTime the date time to check
     * @return true if the business is closed at that moment
     */
    public boolean isClosedAt(ZonedDateTime dateTime) {
        return !isOpenAt(dateTime);
    }

    /** Checks whether the business is open right now, in the input timezone. @return true when open */
    public boolean isOpen() {
        return isOpenAt(ZonedDateTime.now());
    }

    /** Checks whether the business is closed right now, in the input timezone. @return true when closed */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * Checks whether the business is open 24/7.
     *
     * <p>Every day and every exception must cover {@code 00:00-24:00}, and there must be no filter.</p>
     *
     * @return true when the business never closes
     */
    public boolean isAlwaysOpen() {
        return every(day -> "00:00-24:00".equals(day.toString()))
                && everyExceptions(day -> "00:00-24:00".equals(day.toString()))
                && filters.isEmpty();
    }

    /**
     * Checks whether the business is never open.
     *
     * <p>Every day and every exception must be empty, and there must be no filter.</p>
     *
     * @return true when the business never opens
     */
    public boolean isAlwaysClosed() {
        return every(OpeningHoursForDay::isEmpty)
                && everyExceptions(OpeningHoursForDay::isEmpty)
                && filters.isEmpty();
    }

    /**
     * Returns the regular days without any opening range.
     *
     * @return the lowercase english day names of the closed days
     */
    public List<String> regularClosingDays() {
        List<String> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (openingHours.get(day).isEmpty()) {
                days.add(day.name().toLowerCase());
            }
        }
        return days;
    }

    /**
     * Returns the regular days without any opening range, as ISO day numbers.
     *
     * @return the closed days, from {@code 1} (Monday) to {@code 7} (Sunday)
     */
    public List<Integer> regularClosingDaysISO() {
        List<Integer> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (openingHours.get(day).isEmpty()) {
                days.add(day.getValue());
            }
        }
        return days;
    }

    /**
     * Returns the dates of the exceptions that are closed all day.
     *
     * <p>Recurring exceptions ({@code MM-dd}) are not part of the result since they are not bound to
     * a year.</p>
     *
     * @return the fully qualified closing dates
     */
    public List<LocalDate> exceptionalClosingDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (Map.Entry<String, OpeningHoursForDay> entry : exceptions.entrySet()) {
            if (entry.getValue().isEmpty() && entry.getKey().length() == 10) {
                dates.add(LocalDate.parse(entry.getKey()));
            }
        }
        return dates;
    }

    /**
     * Returns the range the business is in at the given date time, if any.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the running range, or empty when the business is closed
     */
    public Optional<TimeRange> currentOpenRange(LocalDateTime dateTime) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        List<TimeRange> ranges = forDateTime(current);
        return ranges.isEmpty() ? Optional.empty() : Optional.of(ranges.get(ranges.size() - 1));
    }

    /**
     * Returns the moment the running range started.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the start of the running range, or empty when the business is closed
     */
    public Optional<LocalDateTime> currentOpenRangeStart(LocalDateTime dateTime) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        Optional<TimeRange> range = currentOpenRange(current);
        if (!range.isPresent()) {
            return Optional.empty();
        }
        Time start = range.get().start();
        LocalDate date = current.toLocalDate();
        if (range.get().overflowsNextDay() && start.hhmm() > Time.fromDateTime(current).hhmm()) {
            date = date.minusDays(1);
        }
        return Optional.of(date.atTime(start.hours(), start.minutes()));
    }

    /**
     * Returns the moment the running range ends.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the end of the running range, or empty when the business is closed
     */
    public Optional<LocalDateTime> currentOpenRangeEnd(LocalDateTime dateTime) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        Optional<TimeRange> range = currentOpenRange(current);
        if (!range.isPresent()) {
            return Optional.empty();
        }
        Time end = range.get().end();
        if (end.hours() >= 24) {
            return Optional.of(current.toLocalDate().plusDays(1).atStartOfDay());
        }
        LocalDate date = current.toLocalDate();
        if (range.get().overflowsNextDay() && end.hhmm() < Time.fromDateTime(current).hhmm()) {
            date = date.plusDays(1);
        }
        return Optional.of(date.atTime(end.hours(), end.minutes()));
    }

    /**
     * Returns the range the business is in at the given date time, with dates and timezones applied.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the running range, or empty when the business is closed
     */
    public Optional<DateTimeRange> currentOpenRange(ZonedDateTime dateTime) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        LocalDateTime local = applied.toLocalDateTime();
        Optional<TimeRange> range = currentOpenRange(local);
        if (!range.isPresent()) {
            return Optional.empty();
        }
        LocalDateTime start = currentOpenRangeStart(local).orElse(null);
        LocalDateTime end = currentOpenRangeEnd(local).orElse(null);
        ZoneId inputZone = applied.getZone();
        return Optional.of(new DateTimeRange(start.atZone(inputZone), end.atZone(inputZone), range.get()));
    }

    /**
     * Returns the moment the running range started, converted to the output timezone.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the start of the running range, or empty when the business is closed
     */
    public Optional<ZonedDateTime> currentOpenRangeStart(ZonedDateTime dateTime) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        LocalDateTime start = currentOpenRangeStart(applied.toLocalDateTime()).orElse(null);
        if (start == null) {
            return Optional.empty();
        }
        return Optional.of(toOutput(start.atZone(applied.getZone()), outputZone(dateTime)));
    }

    /**
     * Returns the moment the running range ends, converted to the output timezone.
     *
     * @param dateTime the date time to check, defaults to now when null
     * @return the end of the running range, or empty when the business is closed
     */
    public Optional<ZonedDateTime> currentOpenRangeEnd(ZonedDateTime dateTime) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        LocalDateTime end = currentOpenRangeEnd(applied.toLocalDateTime()).orElse(null);
        if (end == null) {
            return Optional.empty();
        }
        return Optional.of(toOutput(end.atZone(applied.getZone()), outputZone(dateTime)));
    }

    /**
     * Returns the next moment the business opens.
     *
     * <p>The search spans at most {@link #getDayLimit()} days; when nothing is found, a
     * {@code MaximumLimitExceeded} exception is thrown.</p>
     *
     * @param dateTime the date time to search from, defaults to now when null
     * @return the next open date time
     */
    public LocalDateTime nextOpen(LocalDateTime dateTime) {
        return nextOpen(dateTime, null, null);
    }

    /**
     * Returns the next moment the business opens, giving up at {@code searchUntil}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the next open date time
     */
    public LocalDateTime nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil) {
        return nextOpen(dateTime, searchUntil, null);
    }

    /**
     * Returns the next moment the business opens, giving up at {@code searchUntil} or returning
     * {@code cap}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no open moment is found before it
     * @return the next open date time, or {@code cap}
     */
    public LocalDateTime nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        OpeningHoursForDay day = forDate(current);
        Time nextOpen = day.nextOpen(Time.fromDateTime(current)).orElse(null);
        int tries = getDayLimit();

        while (nextOpen == null || nextOpen.hours() >= 24) {
            if (--tries < 0) {
                throw MaximumLimitExceeded.forString(
                        "No open date/time found in the next " + getDayLimit()
                                + " days, use setDayLimit() to increase the limit.");
            }
            current = current.toLocalDate().plusDays(1).atStartOfDay();
            if (isOpenAt(current) && !day.isOpenAtTheEndOfTheDay()) {
                return current;
            }
            if (cap != null && current.isAfter(cap)) {
                return cap;
            }
            if (searchUntil != null && current.isAfter(searchUntil)) {
                throw SearchLimitReached.forDate(searchUntil);
            }
            day = forDate(current);
            nextOpen = day.nextOpen(Time.fromDateTime(current)).orElse(null);
        }

        if (current.toLocalTime().equals(LocalTime.MIDNIGHT) && isOpenAt(current.minusSeconds(1))) {
            return nextOpen(current.plusSeconds(1), searchUntil, cap);
        }

        return current.toLocalDate().atTime(nextOpen.hours(), nextOpen.minutes());
    }

    /**
     * Timezone-aware variant of {@link #nextOpen(LocalDateTime)}.
     *
     * @param dateTime the date time to search from, defaults to now when null
     * @return the next open date time, in the output timezone
     */
    public ZonedDateTime nextOpen(ZonedDateTime dateTime) {
        return nextOpen(dateTime != null ? dateTime : ZonedDateTime.now(), null, null);
    }

    /**
     * Timezone-aware variant of {@link #nextOpen(LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the next open date time, in the output timezone
     */
    public ZonedDateTime nextOpen(ZonedDateTime dateTime, ZonedDateTime searchUntil) {
        return nextOpen(dateTime, searchUntil, null);
    }

    /**
     * Timezone-aware variant of {@link #nextOpen(LocalDateTime, LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no open moment is found before it
     * @return the next open date time, or {@code cap}, in the output timezone
     */
    public ZonedDateTime nextOpen(ZonedDateTime dateTime, ZonedDateTime searchUntil, ZonedDateTime cap) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        ZoneId outputZone = outputZone(dateTime);
        LocalDateTime capLocal = cap == null ? null : applyTimezone(cap).toLocalDateTime();
        LocalDateTime result;
        try {
            result = nextOpen(
                    applied.toLocalDateTime(),
                    searchUntil == null ? null : applyTimezone(searchUntil).toLocalDateTime(),
                    capLocal);
        } catch (SearchLimitReached e) {
            throw SearchLimitReached.forDate(searchUntil);
        }
        if (cap != null && result.equals(capLocal)) {
            return cap;
        }
        return toOutput(result.atZone(applied.getZone()), outputZone);
    }

    /**
     * Returns the next moment the business closes: the end of the running range, or the end of the
     * next one when the business is currently closed.
     *
     * @param dateTime the date time to search from, defaults to now when null
     * @return the next closing date time
     */
    public LocalDateTime nextClose(LocalDateTime dateTime) {
        return nextClose(dateTime, null, null);
    }

    /**
     * Returns the next closing moment, giving up at {@code searchUntil}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the next closing date time
     */
    public LocalDateTime nextClose(LocalDateTime dateTime, LocalDateTime searchUntil) {
        return nextClose(dateTime, searchUntil, null);
    }

    /**
     * Returns the next closing moment, giving up at {@code searchUntil} or returning {@code cap}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no closing moment is found before it
     * @return the next closing date time, or {@code cap}
     */
    public LocalDateTime nextClose(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        Time time = Time.fromDateTime(current);

        Optional<TimeRange> currentRange = currentOpenRange(current);
        if (currentRange.isPresent() && currentRange.get().end().hours() < 24) {
            Time end = currentRange.get().end();
            LocalDate date = current.toLocalDate();
            if (currentRange.get().overflowsNextDay() && end.hhmm() < time.hhmm()) {
                date = date.plusDays(1);
            }
            return date.atTime(end.hours(), end.minutes());
        }

        Time nextClose = null;
        if (overflow) {
            OpeningHoursForDay dayBefore = forDate(current.minusDays(1));
            if (dayBefore.isOpenAtNight(time)) {
                nextClose = dayBefore.nextClose(time).orElse(null);
            }
        }

        OpeningHoursForDay day = forDate(current);
        if (nextClose == null) {
            nextClose = day.nextClose(time).orElse(null);
            if (nextClose != null && nextClose.hours() < 24 && (
                    nextClose.hhmm() < time.hhmm()
                            || (isClosedAt(current) && Time.fromDateTime(nextOpen(current)).hhmm() > nextClose.hhmm()))) {
                current = current.toLocalDate().plusDays(1).atStartOfDay();
            }
        }

        int tries = getDayLimit();
        while (nextClose == null || nextClose.hours() >= 24) {
            if (--tries < 0) {
                throw MaximumLimitExceeded.forString(
                        "No close date/time found in the next " + getDayLimit()
                                + " days, use setDayLimit() to increase the limit.");
            }
            current = current.toLocalDate().plusDays(1).atStartOfDay();
            if (isClosedAt(current) && day.isOpenAtTheEndOfTheDay()) {
                return current;
            }
            if (cap != null && current.isAfter(cap)) {
                return cap;
            }
            if (searchUntil != null && current.isAfter(searchUntil)) {
                throw SearchLimitReached.forDate(searchUntil);
            }
            day = forDate(current);
            nextClose = day.nextClose(Time.fromDateTime(current)).orElse(null);
        }

        return current.toLocalDate().atTime(nextClose.hours(), nextClose.minutes());
    }

    /**
     * Timezone-aware variant of {@link #nextClose(LocalDateTime)}.
     *
     * @param dateTime the date time to search from, defaults to now when null
     * @return the next closing date time, in the output timezone
     */
    public ZonedDateTime nextClose(ZonedDateTime dateTime) {
        return nextClose(dateTime != null ? dateTime : ZonedDateTime.now(), null, null);
    }

    /**
     * Timezone-aware variant of {@link #nextClose(LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the next closing date time, in the output timezone
     */
    public ZonedDateTime nextClose(ZonedDateTime dateTime, ZonedDateTime searchUntil) {
        return nextClose(dateTime, searchUntil, null);
    }

    /**
     * Timezone-aware variant of {@link #nextClose(LocalDateTime, LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no closing moment is found before it
     * @return the next closing date time, or {@code cap}, in the output timezone
     */
    public ZonedDateTime nextClose(ZonedDateTime dateTime, ZonedDateTime searchUntil, ZonedDateTime cap) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        ZoneId outputZone = outputZone(dateTime);
        LocalDateTime capLocal = cap == null ? null : applyTimezone(cap).toLocalDateTime();
        LocalDateTime result;
        try {
            result = nextClose(
                    applied.toLocalDateTime(),
                    searchUntil == null ? null : applyTimezone(searchUntil).toLocalDateTime(),
                    capLocal);
        } catch (SearchLimitReached e) {
            throw SearchLimitReached.forDate(searchUntil);
        }
        if (cap != null && result.equals(capLocal)) {
            return cap;
        }
        return toOutput(result.atZone(applied.getZone()), outputZone);
    }

    /**
     * Returns the moment the business last opened, before the given date time.
     *
     * @param dateTime the date time to search backwards from, defaults to now when null
     * @return the previous open date time
     */
    public LocalDateTime previousOpen(LocalDateTime dateTime) {
        return previousOpen(dateTime, null, null);
    }

    /**
     * Returns the moment the business last opened, giving up at {@code searchUntil}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the previous open date time
     */
    public LocalDateTime previousOpen(LocalDateTime dateTime, LocalDateTime searchUntil) {
        return previousOpen(dateTime, searchUntil, null);
    }

    /**
     * Returns the moment the business last opened, giving up at {@code searchUntil} or returning
     * {@code cap}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no open moment is found before it
     * @return the previous open date time, or {@code cap}
     */
    public LocalDateTime previousOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        OpeningHoursForDay day = forDate(current);
        Time previousOpen = day.previousOpen(Time.fromDateTime(current)).orElse(null);
        int tries = getDayLimit();

        while (previousOpen == null || (previousOpen.hours() == 0 && previousOpen.minutes() == 0)) {
            if (--tries < 0) {
                throw MaximumLimitExceeded.forString(
                        "No open date/time found in the previous " + getDayLimit()
                                + " days, use setDayLimit() to increase the limit.");
            }
            LocalDateTime midnight = current.toLocalDate().atStartOfDay();
            current = midnight.minusSeconds(1);
            day = forDate(current);
            if (isOpenAt(midnight) && !day.isOpenAtTheEndOfTheDay()) {
                return midnight;
            }
            if (cap != null && current.isBefore(cap)) {
                return cap;
            }
            if (searchUntil != null && current.isBefore(searchUntil)) {
                throw SearchLimitReached.forDate(searchUntil);
            }
            previousOpen = day.previousOpen(Time.fromDateTime(current)).orElse(null);
        }

        return current.toLocalDate().atTime(previousOpen.hours(), previousOpen.minutes());
    }

    /**
     * Timezone-aware variant of {@link #previousOpen(LocalDateTime)}.
     *
     * @param dateTime the date time to search backwards from, defaults to now when null
     * @return the previous open date time, in the output timezone
     */
    public ZonedDateTime previousOpen(ZonedDateTime dateTime) {
        return previousOpen(dateTime != null ? dateTime : ZonedDateTime.now(), null, null);
    }

    /**
     * Timezone-aware variant of {@link #previousOpen(LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the previous open date time, in the output timezone
     */
    public ZonedDateTime previousOpen(ZonedDateTime dateTime, ZonedDateTime searchUntil) {
        return previousOpen(dateTime, searchUntil, null);
    }

    /**
     * Timezone-aware variant of {@link #previousOpen(LocalDateTime, LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no open moment is found before it
     * @return the previous open date time, or {@code cap}, in the output timezone
     */
    public ZonedDateTime previousOpen(ZonedDateTime dateTime, ZonedDateTime searchUntil, ZonedDateTime cap) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        ZoneId outputZone = outputZone(dateTime);
        LocalDateTime capLocal = cap == null ? null : applyTimezone(cap).toLocalDateTime();
        LocalDateTime result;
        try {
            result = previousOpen(
                    applied.toLocalDateTime(),
                    searchUntil == null ? null : applyTimezone(searchUntil).toLocalDateTime(),
                    capLocal);
        } catch (SearchLimitReached e) {
            throw SearchLimitReached.forDate(searchUntil);
        }
        if (cap != null && result.equals(capLocal)) {
            return cap;
        }
        return toOutput(result.atZone(applied.getZone()), outputZone);
    }

    /**
     * Returns the moment the business last closed, before the given date time.
     *
     * @param dateTime the date time to search backwards from, defaults to now when null
     * @return the previous closing date time
     */
    public LocalDateTime previousClose(LocalDateTime dateTime) {
        return previousClose(dateTime, null, null);
    }

    /**
     * Returns the moment the business last closed, giving up at {@code searchUntil}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the previous closing date time
     */
    public LocalDateTime previousClose(LocalDateTime dateTime, LocalDateTime searchUntil) {
        return previousClose(dateTime, searchUntil, null);
    }

    /**
     * Returns the moment the business last closed, giving up at {@code searchUntil} or returning
     * {@code cap}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no closing moment is found before it
     * @return the previous closing date time, or {@code cap}
     */
    public LocalDateTime previousClose(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap) {
        LocalDateTime current = dateTime != null ? dateTime : now();
        Time time = Time.fromDateTime(current);

        Time previousClose = null;
        if (overflow) {
            OpeningHoursForDay dayBefore = forDate(current.minusDays(1));
            if (dayBefore.isOpenAtNight(time)) {
                previousClose = dayBefore.previousClose(time).orElse(null);
            }
        }

        OpeningHoursForDay day = forDate(current);
        if (previousClose == null) {
            previousClose = day.previousClose(time).orElse(null);
        }

        int tries = getDayLimit();
        while (previousClose == null || (previousClose.hours() == 0 && previousClose.minutes() == 0)) {
            if (--tries < 0) {
                throw MaximumLimitExceeded.forString(
                        "No close date/time found in the previous " + getDayLimit()
                                + " days, use setDayLimit() to increase the limit.");
            }
            LocalDateTime midnight = current.toLocalDate().atStartOfDay();
            current = midnight.minusSeconds(1);
            day = forDate(current);
            if (isClosedAt(midnight) && day.isOpenAtTheEndOfTheDay()) {
                return midnight;
            }
            if (cap != null && current.isBefore(cap)) {
                return cap;
            }
            if (searchUntil != null && current.isBefore(searchUntil)) {
                throw SearchLimitReached.forDate(searchUntil);
            }
            previousClose = day.previousClose(Time.fromDateTime(current)).orElse(null);
        }

        return current.toLocalDate().atTime(previousClose.hours(), previousClose.minutes());
    }

    /**
     * Timezone-aware variant of {@link #previousClose(LocalDateTime)}.
     *
     * @param dateTime the date time to search backwards from, defaults to now when null
     * @return the previous closing date time, in the output timezone
     */
    public ZonedDateTime previousClose(ZonedDateTime dateTime) {
        return previousClose(dateTime != null ? dateTime : ZonedDateTime.now(), null, null);
    }

    /**
     * Timezone-aware variant of {@link #previousClose(LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @return the previous closing date time, in the output timezone
     */
    public ZonedDateTime previousClose(ZonedDateTime dateTime, ZonedDateTime searchUntil) {
        return previousClose(dateTime, searchUntil, null);
    }

    /**
     * Timezone-aware variant of {@link #previousClose(LocalDateTime, LocalDateTime, LocalDateTime)}.
     *
     * @param dateTime    the date time to search backwards from, defaults to now when null
     * @param searchUntil the moment after which a {@code SearchLimitReached} exception is thrown
     * @param cap         the value returned when no closing moment is found before it
     * @return the previous closing date time, or {@code cap}, in the output timezone
     */
    public ZonedDateTime previousClose(ZonedDateTime dateTime, ZonedDateTime searchUntil, ZonedDateTime cap) {
        ZonedDateTime applied = applyTimezone(dateTime != null ? dateTime : ZonedDateTime.now());
        ZoneId outputZone = outputZone(dateTime);
        LocalDateTime capLocal = cap == null ? null : applyTimezone(cap).toLocalDateTime();
        LocalDateTime result;
        try {
            result = previousClose(
                    applied.toLocalDateTime(),
                    searchUntil == null ? null : applyTimezone(searchUntil).toLocalDateTime(),
                    capLocal);
        } catch (SearchLimitReached e) {
            throw SearchLimitReached.forDate(searchUntil);
        }
        if (cap != null && result.equals(capLocal)) {
            return cap;
        }
        return toOutput(result.atZone(applied.getZone()), outputZone);
    }

    /**
     * Returns the number of open hours between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the hours the business is open during that period
     */
    public double diffInOpenHours(LocalDateTime start, LocalDateTime end) {
        return diffInOpenMinutes(start, end) / 60.0;
    }

    /**
     * Returns the number of open minutes between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the minutes the business is open during that period
     */
    public double diffInOpenMinutes(LocalDateTime start, LocalDateTime end) {
        return diffInOpenSeconds(start, end) / 60.0;
    }

    /**
     * Returns the number of open seconds between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the seconds the business is open during that period
     */
    public double diffInOpenSeconds(LocalDateTime start, LocalDateTime end) {
        return diffInSeconds(true, start, end);
    }

    /**
     * Returns the number of closed hours between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the hours the business is closed during that period
     */
    public double diffInClosedHours(LocalDateTime start, LocalDateTime end) {
        return diffInClosedMinutes(start, end) / 60.0;
    }

    /**
     * Returns the number of closed minutes between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the minutes the business is closed during that period
     */
    public double diffInClosedMinutes(LocalDateTime start, LocalDateTime end) {
        return diffInClosedSeconds(start, end) / 60.0;
    }

    /**
     * Returns the number of closed seconds between two date times.
     *
     * @param start the beginning of the period
     * @param end   the end of the period
     * @return the seconds the business is closed during that period
     */
    public double diffInClosedSeconds(LocalDateTime start, LocalDateTime end) {
        return diffInSeconds(false, start, end);
    }

    private double diffInSeconds(boolean open, LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            return -diffInSeconds(open, end, start);
        }
        double seconds = 0.0;
        LocalDateTime date = start;
        while (date.isBefore(end)) {
            if (open ? isClosedAt(date) : isOpenAt(date)) {
                date = open ? nextOpen(date, null, end) : nextClose(date, null, end);
                continue;
            }
            LocalDateTime nextChange = open ? nextClose(date, null, end) : nextOpen(date, null, end);
            LocalDateTime nextDate = end.isBefore(nextChange) ? end : nextChange;
            seconds += Duration.between(date, nextDate).toNanos() / 1_000_000_000.0;
            date = nextDate;
        }
        return seconds;
    }

    /**
     * Merges the overlapping ranges of every day in the given definition.
     *
     * @param data the definition holding possibly overlapping ranges
     * @return a new definition holding merged ranges
     */
    public static Map<String, Object> mergeOverlappingRanges(Map<String, Object> data) {
        return mergeOverlappingRanges(data, true, DEFAULT_EXCLUDED_KEYS);
    }

    /**
     * Merges the overlapping ranges of every day in the given definition.
     *
     * @param data       the definition holding possibly overlapping ranges
     * @param ignoreData true to merge ranges even when they carry different data, dropping that
     *                   data; false to keep ranges with different data apart
     * @return a new definition holding merged ranges
     */
    public static Map<String, Object> mergeOverlappingRanges(Map<String, Object> data, boolean ignoreData) {
        return mergeOverlappingRanges(data, ignoreData, DEFAULT_EXCLUDED_KEYS);
    }

    /**
     * Merges the overlapping ranges of every day in the given definition, leaving the given keys
     * untouched.
     *
     * @param data         the definition holding possibly overlapping ranges
     * @param ignoreData   true to merge ranges even when they carry different data
     * @param excludedKeys the keys that should be left out of the merge
     * @return a new definition holding merged ranges
     */
    public static Map<String, Object> mergeOverlappingRanges(Map<String, Object> data,
                                                             boolean ignoreData,
                                                             List<String> excludedKeys) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (excludedKeys.contains(entry.getKey())) {
                continue;
            }
            result.put(entry.getKey(), mergeRanges(entry.getValue(), ignoreData));
        }
        return result;
    }

    /**
     * Creates an {@code OpeningHours} instance from a definition whose ranges may overlap.
     *
     * @param data the definition holding possibly overlapping ranges
     * @return the resulting opening hours
     */
    public static OpeningHours createAndMergeOverlappingRanges(Map<String, Object> data) {
        return createAndMergeOverlappingRanges(data, null, null);
    }

    /**
     * Creates an {@code OpeningHours} instance from a definition whose ranges may overlap, using the
     * given timezones.
     *
     * @param data           the definition holding possibly overlapping ranges
     * @param timezone       the timezone used to interpret the given date times
     * @param outputTimezone the timezone used to return date times
     * @return the resulting opening hours
     */
    public static OpeningHours createAndMergeOverlappingRanges(Map<String, Object> data,
                                                               ZoneId timezone,
                                                               ZoneId outputTimezone) {
        return createAndMergeOverlappingRanges(data, timezone, outputTimezone, true);
    }

    /**
     * Creates an {@code OpeningHours} instance from a definition whose ranges may overlap, using the
     * given timezones.
     *
     * @param data           the definition holding possibly overlapping ranges
     * @param timezone       the timezone used to interpret the given date times
     * @param outputTimezone the timezone used to return date times
     * @param ignoreData     true to merge ranges even when they carry different data
     * @return the resulting opening hours
     */
    public static OpeningHours createAndMergeOverlappingRanges(Map<String, Object> data,
                                                               ZoneId timezone,
                                                               ZoneId outputTimezone,
                                                               boolean ignoreData) {
        return create(mergeOverlappingRanges(data, ignoreData), timezone, outputTimezone);
    }

    /**
     * Creates an {@code OpeningHours} instance from
     * https://schema.org/OpeningHoursSpecification objects.
     *
     * <p>Overflow is enabled, since such specifications may describe ranges spilling over midnight.</p>
     *
     * @param specifications the schema.org specification objects
     * @return the resulting opening hours
     * @throws io.github.wuwx.openinghours.exceptions.InvalidOpeningHoursSpecification if a
     * specification is malformed
     */
    public static OpeningHours createFromStructuredData(List<Map<String, Object>> specifications) {
        return createFromStructuredData(specifications, null, null);
    }

    /**
     * Creates an {@code OpeningHours} instance from
     * https://schema.org/OpeningHoursSpecification objects, using the given timezones.
     *
     * @param specifications the schema.org specification objects
     * @param timezone       the timezone used to interpret the given date times
     * @param outputTimezone the timezone used to return date times
     * @return the resulting opening hours
     * @throws io.github.wuwx.openinghours.exceptions.InvalidOpeningHoursSpecification if a
     * specification is malformed
     */
    public static OpeningHours createFromStructuredData(List<Map<String, Object>> specifications,
                                                        ZoneId timezone,
                                                        ZoneId outputTimezone) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overflow", true);
        data.putAll(OpeningHoursSpecificationParser.createFromArray(specifications).getOpeningHours());
        return create(data, timezone, outputTimezone);
    }

    /**
     * Returns the definition as a list of https://schema.org/OpeningHoursSpecification maps, regular
     * hours first, then exceptions.
     *
     * @return the structured data
     */
    public List<Map<String, Object>> asStructuredData() {
        return asStructuredData("HH:mm");
    }

    /**
     * Returns the definition as schema.org specification maps, formatted with the given time
     * pattern, for example {@code HH:mm:ss} or {@code hh:mm a}.
     *
     * @param format the {@link java.time.format.DateTimeFormatter} pattern for the times
     * @return the structured data
     */
    public List<Map<String, Object>> asStructuredData(String format) {
        return asStructuredData(format, null);
    }

    /**
     * Returns the definition as schema.org specification maps, formatted with the given time pattern
     * and rendered in the given timezone.
     *
     * @param format   the {@link java.time.format.DateTimeFormatter} pattern for the times
     * @param timezone the timezone the times should be expressed in
     * @return the structured data
     */
    public List<Map<String, Object>> asStructuredData(String format, ZoneId timezone) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.name().toLowerCase();
            dayName = Character.toUpperCase(dayName.charAt(0)) + dayName.substring(1);
            for (TimeRange range : openingHours.get(day).getTimeRanges()) {
                Map<String, Object> spec = new LinkedHashMap<>();
                spec.put("@type", "OpeningHoursSpecification");
                spec.put("dayOfWeek", dayName);
                spec.put("opens", formatTime(range.start(), format, timezone));
                spec.put("closes", formatTime(range.end(), format, timezone));
                result.add(spec);
            }
        }
        String zero = formatTime(Time.MIDNIGHT, format, timezone);
        for (Map.Entry<String, OpeningHoursForDay> entry : exceptions.entrySet()) {
            for (TimeRange range : entry.getValue().getTimeRanges()) {
                Map<String, Object> spec = new LinkedHashMap<>();
                spec.put("@type", "OpeningHoursSpecification");
                spec.put("opens", formatTime(range.start(), format, timezone));
                spec.put("closes", formatTime(range.end(), format, timezone));
                spec.put("validFrom", entry.getKey());
                spec.put("validThrough", entry.getKey());
                result.add(spec);
            }
            if (entry.getValue().isEmpty()) {
                Map<String, Object> spec = new LinkedHashMap<>();
                spec.put("@type", "OpeningHoursSpecification");
                spec.put("opens", zero);
                spec.put("closes", zero);
                spec.put("validFrom", entry.getKey());
                spec.put("validThrough", entry.getKey());
                result.add(spec);
            }
        }
        return result;
    }

    /**
     * Returns the definition as a JSON array of https://schema.org/OpeningHoursSpecification
     * objects.
     *
     * @return the structured data as JSON
     */
    public String asStructuredJson() {
        return asStructuredJson("HH:mm");
    }

    /**
     * Returns the definition as a JSON array of schema.org specification objects, formatted with the
     * given time pattern.
     *
     * @param format the {@link java.time.format.DateTimeFormatter} pattern for the times
     * @return the structured data as JSON
     */
    public String asStructuredJson(String format) {
        return toJson(asStructuredData(format));
    }

    /**
     * Returns the definition as a JSON array of schema.org specification objects, formatted with the
     * given time pattern and rendered in the given timezone.
     *
     * @param format   the {@link java.time.format.DateTimeFormatter} pattern for the times
     * @param timezone the timezone the times should be expressed in
     * @return the structured data as JSON
     */
    public String asStructuredJson(String format, ZoneId timezone) {
        return toJson(asStructuredData(format, timezone));
    }

    static String toJson(List<Map<String, Object>> data) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('{');
            int index = 0;
            for (Map.Entry<String, Object> entry : data.get(i).entrySet()) {
                if (index++ > 0) {
                    json.append(',');
                }
                appendJsonString(json, entry.getKey());
                json.append(':');
                appendJsonValue(json, entry.getValue());
            }
            json.append('}');
        }
        return json.append(']').toString();
    }

    private static void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else {
            appendJsonString(json, String.valueOf(value));
        }
    }

    private static void appendJsonString(StringBuilder json, String text) {
        json.append('"');
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\b':
                    json.append("\\b");
                    break;
                case '\f':
                    json.append("\\f");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
            }
        }
        json.append('"');
    }

    // 偏移量需要参考日才能算出，固定 1970-01-01 以免结果随当前日期变化
    private static String formatTime(Time time, String format, ZoneId timezone) {
        if (timezone == null) {
            return time.format(format);
        }
        LocalDateTime reference = LocalDateTime.of(1970, 1, 1, 0, 0);
        if (time.hours() == 24 && time.minutes() == 0 && format.startsWith("HH:mm")) {
            String rest = format.length() > 5
                    ? reference.atZone(timezone).format(DateTimeFormatter.ofPattern(format.substring(5)))
                    : "";
            return "24:00" + rest;
        }
        return reference.withHour(time.hours() % 24).withMinute(time.minutes())
                .atZone(timezone)
                .format(DateTimeFormatter.ofPattern(format));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(timezone != null ? timezone : ZoneId.systemDefault());
    }

    private ZonedDateTime applyTimezone(ZonedDateTime dateTime) {
        return timezone == null ? dateTime : dateTime.withZoneSameInstant(timezone);
    }

    private ZoneId outputZone(ZonedDateTime dateTime) {
        if (outputTimezone != null) {
            return outputTimezone;
        }
        if (timezone == null || dateTime == null) {
            return timezone;
        }
        return dateTime.getZone();
    }

    private ZonedDateTime toOutput(ZonedDateTime dateTime, ZoneId outputZone) {
        return outputZone == null ? dateTime : dateTime.withZoneSameInstant(outputZone);
    }

    /**
     * Filters the regular week, keeping only the days matching the callback.
     *
     * @param callback the predicate applied to every day
     * @return the matching days, keyed by lowercase english day name
     */
    public Map<String, OpeningHoursForDay> filter(Predicate<OpeningHoursForDay> callback) {
        Map<String, OpeningHoursForDay> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            OpeningHoursForDay hours = openingHours.get(day);
            if (callback.test(hours)) {
                result.put(day.name().toLowerCase(), hours);
            }
        }
        return result;
    }

    /**
     * Maps every day of the regular week to a value.
     *
     * @param callback the function applied to every day
     * @param <T>      the type returned by the callback
     * @return the mapped values, keyed by lowercase english day name
     */
    public <T> Map<String, T> map(Function<OpeningHoursForDay, T> callback) {
        Map<String, T> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day.name().toLowerCase(), callback.apply(openingHours.get(day)));
        }
        return result;
    }

    /**
     * Maps every day of the regular week to a list and flattens the result.
     *
     * @param callback the function applied to every day
     * @param <T>      the type of the flattened elements
     * @return every returned element, concatenated
     */
    public <T> List<T> flatMap(Function<OpeningHoursForDay, List<T>> callback) {
        List<T> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            result.addAll(callback.apply(openingHours.get(day)));
        }
        return result;
    }

    /**
     * Checks whether every day of the regular week matches the callback.
     *
     * @param callback the predicate applied to every day
     * @return true when all seven days match
     */
    public boolean every(Predicate<OpeningHoursForDay> callback) {
        for (OpeningHoursForDay day : openingHours.values()) {
            if (!callback.test(day)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Filters the exceptions, keeping only the ones matching the callback.
     *
     * @param callback the predicate applied to every exception
     * @return the matching exceptions, keyed by date
     */
    public Map<String, OpeningHoursForDay> filterExceptions(Predicate<OpeningHoursForDay> callback) {
        Map<String, OpeningHoursForDay> result = new LinkedHashMap<>();
        for (Map.Entry<String, OpeningHoursForDay> entry : exceptions.entrySet()) {
            if (callback.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Maps every exception to a value.
     *
     * @param callback the function applied to every exception
     * @param <T>      the type returned by the callback
     * @return the mapped values, keyed by date
     */
    public <T> Map<String, T> mapExceptions(Function<OpeningHoursForDay, T> callback) {
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, OpeningHoursForDay> entry : exceptions.entrySet()) {
            result.put(entry.getKey(), callback.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * Maps every exception to a list and flattens the result.
     *
     * @param callback the function applied to every exception
     * @param <T>      the type of the flattened elements
     * @return every returned element, concatenated
     */
    public <T> List<T> flatMapExceptions(Function<OpeningHoursForDay, List<T>> callback) {
        List<T> result = new ArrayList<>();
        for (OpeningHoursForDay day : exceptions.values()) {
            result.addAll(callback.apply(day));
        }
        return result;
    }

    /**
     * Checks whether every exception matches the callback.
     *
     * @param callback the predicate applied to every exception
     * @return true when all exceptions match, or when there are none
     */
    public boolean everyExceptions(Predicate<OpeningHoursForDay> callback) {
        for (OpeningHoursForDay day : exceptions.values()) {
            if (!callback.test(day)) {
                return false;
            }
        }
        return true;
    }

    private static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof CharSequence) {
            String text = value.toString();
            return !text.isEmpty() && !"0".equals(text);
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return true;
    }

    private static ZoneId parseTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            throw InvalidTimezone.create();
        }
    }

    private static DayOfWeek normalizeDayName(String day) {
        try {
            return DayOfWeek.valueOf(day.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw InvalidDayName.invalidDayName(day);
        }
    }

    private static List<String> readDatesRange(String key) {
        String[] toChunks = key.split(" to ", 2);
        if (toChunks.length == 2) {
            return daysBetween(toChunks[0].trim(), toChunks[1].trim());
        }

        String[] dashChunks = key.split("-");
        if (dashChunks.length == 2 && dashChunks[0].matches("[A-Za-z]+")) {
            return daysBetween(dashChunks[0].trim(), dashChunks[1].trim());
        }
        if (dashChunks.length >= 4) {
            int middle = (int) Math.ceil(dashChunks.length / 2.0);
            String start = String.join("-", Arrays.copyOfRange(dashChunks, 0, middle)).trim();
            String end = String.join("-", Arrays.copyOfRange(dashChunks, middle, dashChunks.length)).trim();
            return daysBetween(start, end);
        }
        return Collections.singletonList(key);
    }

    private static List<String> daysBetween(String start, String end) {
        int count = start.split("-").length;
        LocalDate startDate;
        LocalDate endDate;
        boolean dayNames = false;
        if (count == 2) {
            startDate = LocalDate.parse("2024-" + start);
            endDate = LocalDate.parse("2024-" + end);
        } else if (count == 3) {
            startDate = LocalDate.parse(start);
            endDate = LocalDate.parse(end);
        } else {
            dayNames = true;
            startDate = nextOccurrence(LocalDate.of(2024, 1, 1), normalizeDayName(start));
            endDate = nextOccurrence(startDate, normalizeDayName(end));
        }

        List<String> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            days.add(dayNames
                    ? date.getDayOfWeek().name().toLowerCase()
                    : date.format(count == 2 ? MM_DD : DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return days;
    }

    private static LocalDate nextOccurrence(LocalDate from, DayOfWeek target) {
        LocalDate date = from;
        for (int i = 0; i < 7 && date.getDayOfWeek() != target; i++) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static boolean isValidDateKey(String date) {
        try {
            if (date.length() == 5) {
                return date.equals(MM_DD.format(LocalDate.parse("2024-" + date)));
            }
            if (date.length() == 10) {
                return date.equals(LocalDate.parse(date).toString());
            }
        } catch (DateTimeParseException e) {
            return false;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object mergeRanges(Object value, boolean ignoreData) {
        if (value instanceof List) {
            List<Object> passthrough = new ArrayList<>();
            Map<String, List<TimeRange>> groups = new LinkedHashMap<>();
            for (Object item : (List<?>) value) {
                TimeRange range;
                if (item instanceof String) {
                    range = TimeRange.fromString((String) item);
                } else if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    if (map.get("hours") instanceof String) {
                        range = TimeRange.fromString((String) map.get("hours"), map.get("data"));
                    } else {
                        passthrough.add(item);
                        continue;
                    }
                } else if (item instanceof TimeRange) {
                    range = (TimeRange) item;
                } else {
                    passthrough.add(item);
                    continue;
                }
                String groupKey = ignoreData ? "" : String.valueOf(range.data());
                mergeInto(groups.computeIfAbsent(groupKey, k -> new ArrayList<>()), range);
            }
            List<TimeRange> mergedRanges = new ArrayList<>();
            for (List<TimeRange> group : groups.values()) {
                mergedRanges.addAll(group);
            }
            boolean hasData = false;
            for (TimeRange range : mergedRanges) {
                if (range.data() != null) {
                    hasData = true;
                    break;
                }
            }
            if (passthrough.isEmpty() && !hasData) {
                List<String> strings = new ArrayList<>();
                for (TimeRange range : mergedRanges) {
                    strings.add(range.toString());
                }
                return strings;
            }
            List<Object> merged = new ArrayList<>(passthrough);
            for (TimeRange range : mergedRanges) {
                if (range.data() == null) {
                    merged.add(range.toString());
                } else {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("hours", range.toString());
                    out.put("data", range.data());
                    merged.add(out);
                }
            }
            return merged;
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey("hours")) {
                Object merged = mergeRanges(map.get("hours"), ignoreData);
                if (map.containsKey("data")) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("hours", merged);
                    out.put("data", map.get("data"));
                    return out;
                }
                return merged;
            }
            return value;
        }
        return value;
    }

    private static void mergeInto(List<TimeRange> ranges, TimeRange value) {
        List<TimeRange> kept = new ArrayList<>();
        for (TimeRange range : ranges) {
            if (value.toString().equals(range.toString())) {
                return;
            }
            if (value.overlaps(range) || range.overlaps(value)) {
                value = TimeRange.fromList(Arrays.asList(value, range), value.data());
                continue;
            }
            kept.add(range);
        }
        kept.add(value);
        ranges.clear();
        ranges.addAll(kept);
    }
}
