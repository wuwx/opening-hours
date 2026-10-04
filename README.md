# Opening Hours

A helper to query and format a set of opening hours for Java 8+.

With `opening-hours` you create an object that describes a business' opening hours, which you can query for `open` or `closed` on days or specific dates, or use to present the times per day.

A set of opening hours is created by passing in a regular schedule, and a list of exceptions.

```java
import io.github.wuwx.openinghours.OpeningHours;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Create opening hours
Map<String, Object> data = new HashMap<>();
data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));
data.put("tuesday", Arrays.asList("09:00-12:00", "13:00-18:00"));
data.put("wednesday", Arrays.asList("09:00-12:00"));
data.put("thursday", Arrays.asList("09:00-12:00", "13:00-18:00"));
data.put("friday", Arrays.asList("09:00-12:00", "13:00-20:00"));
data.put("saturday", Arrays.asList("09:00-12:00", "13:00-16:00"));
data.put("sunday", Collections.emptyList());

Map<String, Object> exceptions = new HashMap<>();
exceptions.put("2016-11-11", Arrays.asList("09:00-12:00"));
exceptions.put("2016-12-25", Collections.emptyList());
exceptions.put("01-01", Collections.emptyList());              // Recurring on each 1st of January
exceptions.put("12-25", Arrays.asList("09:00-12:00"));        // Recurring on each 25th of December
data.put("exceptions", exceptions);

OpeningHours openingHours = OpeningHours.create(data);

// This will allow you to display things like:
LocalDateTime now = LocalDateTime.now();
Optional<TimeRange> range = openingHours.currentOpenRange(now);

if (range.isPresent()) {
    System.out.println("It's open since " + range.get().start());
    System.out.println("It will close at " + range.get().end());
} else {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE HH:mm");
    System.out.println("It's closed since " + openingHours.previousClose(now).format(formatter));
    System.out.println("It will re-open at " + openingHours.nextOpen(now).format(formatter));
}
```

The object can be queried for a day in the week, which will return a result based on the regular schedule:

```java
// Open on Mondays:
openingHours.isOpenOn("monday"); // true

// Closed on Sundays:
openingHours.isOpenOn("sunday"); // false
```

It can also be queried for a specific date and time:

```java
// Closed because it's after hours:
openingHours.isOpenAt(LocalDateTime.of(2016, 9, 26, 19, 0)); // false

// Closed because Christmas was set as an exception
openingHours.isOpenOn("2016-12-25"); // false
```

It can also return maps/lists of opening hours for a week or a day:

```java
// OpeningHoursForDay object for the regular schedule
openingHours.forDay("monday");

// Map<String, OpeningHoursForDay> for the regular schedule, keyed by day name
openingHours.forWeek();

// Map of day with same schedule for the regular schedule, keyed by day name, days combined by working hours
openingHours.forWeekCombined();

// OpeningHoursForDay object for a specific day
openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0));

// Map<String, OpeningHoursForDay> of all exceptions, keyed by date
openingHours.exceptions();
```

On construction, you can set a flag for overflowing times across days. For example, for a nightclub opens until 3am on Friday and Saturday:

```java
Map<String, Object> data = new HashMap<>();
data.put("overflow", true);
data.put("friday", Arrays.asList("20:00-03:00"));
data.put("saturday", Arrays.asList("20:00-03:00"));

OpeningHours openingHours = OpeningHours.create(data);
```

This allows the API to look at previous day's data to check if the opening hours are open from its time range. Without `overflow`, a range like `20:00-03:00` only covers `20:00` to midnight on its own day; the `00:00-03:00` part is not considered open.

The value may be any truthy flag: `true`, `1`, `"yes"` all enable it, while `false`, `0`, `"0"` and empty values disable it.

You can add data in definitions then retrieve them:

```java
Map<String, Object> data = new HashMap<>();

// Monday with data
Map<String, Object> mondayData = new HashMap<>();
mondayData.put("data", "Typical Monday");
mondayData.put("hours", Arrays.asList("09:00-12:00", "13:00-18:00"));
data.put("monday", mondayData);

// Tuesday with mixed format
List<Object> tuesdayHours = new ArrayList<>();
tuesdayHours.add("09:00-12:00");
tuesdayHours.add("13:00-18:00");
Map<String, Object> eveningSlot = new HashMap<>();
eveningSlot.put("hours", "19:00-21:00");
eveningSlot.put("data", "Extra on Tuesday evening");
tuesdayHours.add(eveningSlot);
data.put("tuesday", tuesdayHours);

// Exception with data
Map<String, Object> exceptions = new HashMap<>();
Map<String, Object> christmasData = new HashMap<>();
christmasData.put("data", "Closed for Christmas");
exceptions.put("2016-12-25", christmasData);
data.put("exceptions", exceptions);

OpeningHours openingHours = OpeningHours.create(data);

System.out.println(openingHours.forDay("monday").data()); // Typical Monday
System.out.println(openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0)).data()); // Closed for Christmas
System.out.println(openingHours.forDay("tuesday").get(2).data()); // Extra on Tuesday evening
```

In the example above, data are strings but it can be any kind of value. So you can embed multiple properties.

For structure convenience, the data-hours couple can be a fully-associative map, so the example above is equivalent to using the `hours` key format:

```java
// Open by night from Wednesday 22h to Thursday 7h:
data.put("wednesday", Arrays.asList("22:00-24:00")); // use the special "24:00" to reach midnight included
data.put("thursday", Arrays.asList("00:00-07:00"));
```

You can use the separator `to` to specify multiple days at once, for the week or for exceptions:

```java
Map<String, Object> data = new HashMap<>();
data.put("monday to friday", Arrays.asList("09:00-19:00"));
data.put("saturday to sunday", Collections.emptyList());

Map<String, Object> exceptions = new HashMap<>();
// Every year
Map<String, Object> holidaysData = new HashMap<>();
holidaysData.put("hours", Collections.emptyList());
holidaysData.put("data", "Holidays");
exceptions.put("12-24 to 12-26", holidaysData);

// Only happening in 2024
Map<String, Object> worksData = new HashMap<>();
worksData.put("hours", Collections.emptyList());
worksData.put("data", "Closed for works");
exceptions.put("2024-06-25 to 2024-07-01", worksData);

data.put("exceptions", exceptions);
OpeningHours openingHours = OpeningHours.create(data);
```

The last structure tool is the filter, it allows you to pass functions that take a date as a parameter and returns the settings for the given date.

```java
import java.util.function.Function;
import java.time.LocalDateTime;

Map<String, Object> data = new HashMap<>();
data.put("monday", Arrays.asList("09:00-12:00"));

List<Function<LocalDateTime, Object>> filters = new ArrayList<>();
filters.add(date -> {
    // Example: Close on Easter Monday
    // (You would need to implement Easter calculation)
    if (isEasterMonday(date)) {
        return Collections.emptyList(); // Closed
        // Any valid hours definition can be returned here (a list of ranges, or a map with hours and data)
    }
    return null; // Else the filter does not apply to the given date
});

data.put("filters", filters);
OpeningHours openingHours = OpeningHours.create(data);
```

If a function is found in the `exceptions` property, it will be added automatically to filters so you can mix filters and exceptions. The first filter that returns a non-null value will have precedence over the next filters and the `filters` list has precedence over the filters inside the `exceptions` map.

**Warning**: We will loop on all filters for each date from which we need to retrieve opening hours and can neither predicate nor cache the result (can be a random function) so you must be careful with filters, too many filters or long process inside filters can have a significant impact on the performance.

It can also return the next open or close `LocalDateTime` from a given `LocalDateTime`.

```java
// The next open datetime is tomorrow morning, because we're closed on 25th of December.
LocalDateTime nextOpen = openingHours.nextOpen(LocalDateTime.of(2016, 12, 25, 10, 0)); // 2016-12-26 09:00:00

// The next open datetime is this afternoon, after the lunch break.
nextOpen = openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 11, 0)); // 2016-12-24 13:00:00

// The next close datetime is at noon.
LocalDateTime nextClose = openingHours.nextClose(LocalDateTime.of(2016, 12, 24, 10, 0)); // 2016-12-24 12:00:00

// The next close datetime is tomorrow at noon, because we're closed on 25th of December.
nextClose = openingHours.nextClose(LocalDateTime.of(2016, 12, 25, 15, 0)); // 2016-12-26 12:00:00
```

Read the usage section for the full API.

## Installation

Requires Java 8 or newer. Building from source requires JDK 9+ (the build compiles with `--release 8`).

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.wuwx</groupId>
    <artifactId>opening-hours</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Upgrading from 1.x

See [CHANGELOG.md](CHANGELOG.md) for the full release history.

2.0.0 fixes behaviours that were wrong in 1.0.0 and tightens the public API, so code written against 1.0.0 may need changes.

**Removed and changed signatures**

| 1.0.0 | 2.0.0 |
| --- | --- |
| `OpeningHours#fill(data)` | `OpeningHours.create(data, timezone, outputTimezone)` |
| `new Time(localTime)` | `Time.fromLocalTime(localTime)` |
| `Time#toLocalTime()` | `Time#toDateTime(LocalDate)`, or `Time#hours()` / `Time#minutes()` / `Time#seconds()` |
| `new TimeRange(start, end[, data])` | `TimeRange.fromString`, `fromArray`, `fromDefinition`, `fromMidnight`, `fromList` |
| `createFromStructuredData(String json)` | `createFromStructuredData(List<Map<String, Object>>)`, the JSON parsing is left to the caller |
| `mergeOverlappingRanges(Map<String, List<String>>)` | `mergeOverlappingRanges(Map<String, Object>)`, with optional `ignoreData` and `excludedKeys` |
| `forWeekCombined()` value key `hours` | value key `opening_hours` |
| `TimeRange#getData()`, `OpeningHoursForDay#getData()` | `data()`; the old name still works but is deprecated |

**Behaviour changes**

- `24:00` means midnight *included* and is preserved when formatting: `09:00-24:00` is no longer considered open at 08:00, and `nextClose` returns midnight rather than `23:59`.
- A range spilling over midnight (`22:00-02:00`) is only taken into account for the next day when `overflow: true` is set, as in the original library.
- Invalid definitions now throw instead of being silently ignored: overlapping ranges (`OverlappingTimeRanges`), a day or a date defined twice (`InvalidDateRange`), invalid day names (`InvalidDayName`), invalid dates (`InvalidDate`) and malformed times (`InvalidTimeString`, `InvalidTimeRangeString`).
- Filters given in the definition are now applied; in 1.0.0 their return value was dropped.
- `asStructuredData()` and `createFromStructuredData()` are implemented; they used to return an empty list and an all-closed instance.
- The search limit is 8 days by default (366 when the definition holds exceptions) and is configurable with `setDayLimit()`; reaching `searchUntil` raises `SearchLimitReached`.
- `OpeningHoursForDay#getTimeRanges()` still refuses modifications, but now throws `NonMutableOffsets` instead of the generic `UnsupportedOperationException`, as the original library does.
- A `dateTimeClass` key raises `InvalidDateTimeClass` instead of being rejected as an invalid day name.

## Usage

The package should only be used through the `OpeningHours` class. There are also three value object classes used throughout, `Time`, which represents a single time, `TimeRange`, which represents a period with a start and an end, and `OpeningHoursForDay`, which represents a set of `TimeRange`s which can't overlap.

### Timezones

Whenever a `ZonedDateTime` is passed, the call is evaluated in the configured input timezone, and the result is converted to the output timezone:

1. If an output timezone is configured (through `create(data, timezone, outputTimezone)`, the `timezone` => `['input' => ..., 'output' => ...]` definition or `setOutputTimezone`), it is used for the result.
2. Otherwise, if no input timezone is configured, the value is returned as is.
3. Otherwise the timezone of the object you passed in is used for the result.

```java
OpeningHours openingHours = OpeningHours.create(data); // schedule is local wall-clock time
ZonedDateTime nextOpen = openingHours.nextOpen(ZonedDateTime.now(ZoneId.of("UTC")));

// Schedule defined in Europe/Oslo, callers in UTC keep receiving UTC results
OpeningHours oslo = OpeningHours.create(data, ZoneId.of("Europe/Oslo"));
ZonedDateTime result = oslo.nextOpen(ZonedDateTime.now(ZoneId.of("UTC"))); // 07:00Z for a 09:00 opening

// Force the output timezone instead
oslo.setOutputTimezone(ZoneId.of("Europe/Oslo"));
```

All of `isOpenAt`, `isClosedAt`, `forDate`, `nextOpen`, `nextClose`, `previousOpen`, `previousClose` and `currentOpenRange*` have a `ZonedDateTime` overload. `currentOpenRange(ZonedDateTime)` returns a `DateTimeRange` whose `start()` / `end()` are `ZonedDateTime`, so a range overflowing midnight points to the previous or next day as expected:

```java
// Monday 22:00-02:00, overflow enabled
Optional<DateTimeRange> range = openingHours.currentOpenRange(
    ZonedDateTime.of(2016, 12, 20, 1, 0, 0, 0, ZoneId.of("Europe/Oslo")));

range.get().start(); // 2016-12-19 22:00+01:00[Europe/Oslo]
range.get().end();   // 2016-12-20 02:00+01:00[Europe/Oslo]
```

Note: all comparisons keep sub-minute precision, so `23:59:59` is *after* `23:59` — a range ending at `23:59` no longer contains the last second of the day, while `24:00` does.

### `io.github.wuwx.openinghours.OpeningHours`

#### `OpeningHours.create(Map<String, Object> data)` 

Static factory method to fill the set of opening hours.

```java
OpeningHours openingHours = OpeningHours.create(data);
```

You can also specify timezone (using `ZoneId` or timezone string):

```java
OpeningHours openingHours = OpeningHours.create(data, ZoneId.of("America/New_York"));
```

If no timezone is specified, `OpeningHours` will just assume you always pass `LocalDateTime` objects that have already the timezone matching your schedule. A `dateTimeClass` key in the definition is refused with an `InvalidDateTimeClass` exception, since this implementation is built on `java.time` and cannot switch to a custom date class.

Alternatively you can also specify both input and output timezone:

```java
Map<String, Object> data = new HashMap<>();
data.put("monday", Arrays.asList("09:00-12:00", "13:00-18:00"));

Map<String, String> timezone = new HashMap<>();
timezone.put("input", "America/New_York");
timezone.put("output", "Europe/Oslo");
data.put("timezone", timezone);

OpeningHours openingHours = OpeningHours.create(data);
```

#### `OpeningHours.mergeOverlappingRanges(Map<String, Object> schedule)` / `mergeOverlappingRanges(Map<String, Object> schedule, boolean ignoreData)` / `mergeOverlappingRanges(Map<String, Object> schedule, boolean ignoreData, List<String> excludedKeys)`

For safety sake, creating an `OpeningHours` object with overlapping ranges throws an `OverlappingTimeRanges` exception. You can explicitly merge them first.

By default overlapping ranges are merged regardless of their `data` (`ignoreData = true`). Pass `false` to only merge ranges sharing the same `data`. `excludedKeys` defaults to `["data", "dateTimeClass", "filters", "overflow"]` and lists the definition keys that are dropped instead of being merged.

```java
Map<String, Object> ranges = new HashMap<>();
ranges.put("monday", Arrays.asList("08:00-11:00", "10:00-12:00"));

Map<String, Object> mergedRanges = OpeningHours.mergeOverlappingRanges(ranges); 
// Monday becomes ["08:00-12:00"]

OpeningHours.create(mergedRanges);
// Or use the following shortcut to create from ranges that possibly overlap:
OpeningHours.createAndMergeOverlappingRanges(ranges);
// Keeping the data groups apart:
OpeningHours.createAndMergeOverlappingRanges(ranges, null, null, false);
```

Not all days are mandatory, if a day is missing, it will be set as closed.

#### `OpeningHours.isValid(Map<String, Object> data)`

Returns `true` if the given definition can be turned into an `OpeningHours` instance, `false` otherwise (invalid day name, invalid date, overlapping ranges, etc.).

```java
boolean valid = OpeningHours.isValid(data);
```

#### `forWeek()`

Returns a `Map<String, OpeningHoursForDay>` for a regular week.

```java
Map<String, OpeningHoursForDay> week = openingHours.forWeek();
```

#### `forWeekCombined()`

Returns a map of days. The map key is the first day with the same hours, and the value is a map with `days` (the day names) and `opening_hours` (the `OpeningHoursForDay` object).

```java
Map<String, Object> combined = openingHours.forWeekCombined();
```

#### `forWeekConsecutiveDays()`

Returns a map of concatenated days, adjacent days with the same hours. The map key is the first day with the same hours, and the value is a map with `days` and `opening_hours`.

*Warning*: consecutive days are considered from Monday to Sunday without looping (Monday is not consecutive to Sunday) no matter the days order in initial data.

```java
Map<String, Object> consecutive = openingHours.forWeekConsecutiveDays();
```

#### `filter(Predicate<OpeningHoursForDay> callback)` / `every(Predicate<OpeningHoursForDay> callback)`

Returns the regular days matching the condition (keyed by day name), or whether all of them match.

```java
Map<String, OpeningHoursForDay> closed = openingHours.filter(OpeningHoursForDay::isEmpty);
boolean shortEnough = openingHours.every(day -> day.size() <= 2);
```

#### `map(Function<OpeningHoursForDay, T> callback)` / `flatMap(Function<OpeningHoursForDay, List<T>> callback)`

Maps the regular days, keeping the day name as key, or flattens the mapped lists.

```java
Map<String, Integer> rangeCount = openingHours.map(OpeningHoursForDay::size);
List<String> allRanges = openingHours.flatMap(day -> day.getTimeRanges().stream()
        .map(TimeRange::toString)
        .collect(Collectors.toList()));
```

#### `filterExceptions(...)` / `mapExceptions(...)` / `flatMapExceptions(...)` / `everyExceptions(...)`

The same aggregations applied to the exceptions map, keyed by date string.

#### `forDay(String day)` / `forDay(DayOfWeek day)`

Returns an `OpeningHoursForDay` object for a regular day. A day is a lowercase string of the english day name, or a `DayOfWeek`.

```java
OpeningHoursForDay monday = openingHours.forDay("monday");
OpeningHoursForDay tuesday = openingHours.forDay(DayOfWeek.TUESDAY);
```

#### `forDate(LocalDateTime dateTime)`

Returns an `OpeningHoursForDay` object for a specific date. It looks for an exception on that day, and otherwise it returns the opening hours based on the regular schedule.

```java
OpeningHoursForDay christmas = openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0));
```

#### `exceptions()`

Returns a `Map<String, OpeningHoursForDay>` of all exceptions, keyed by a date string in `yyyy-MM-dd` format.

```java
Map<String, OpeningHoursForDay> exceptions = openingHours.exceptions();
```

#### `regularClosingDays()` / `regularClosingDaysISO()`

Returns the day names (or ISO numbers, `1` for Monday to `7` for Sunday) of the regular schedule that are closed.

```java
List<String> closingDays = openingHours.regularClosingDays(); // ["sunday"]
List<Integer> closingDaysIso = openingHours.regularClosingDaysISO(); // [7]
```

#### `exceptionalClosingDates()`

Returns the full dates (`yyyy-MM-dd`) of all exceptions that are closed all day. Recurring exceptions (`MM-dd`) are not included since they are not bound to a year.

```java
List<LocalDate> closingDates = openingHours.exceptionalClosingDates();
```

#### `data()`

Returns the metadata passed with the `data` key in the definition, if any. The same accessor is available on `OpeningHoursForDay`, `TimeRange` and `DateTimeRange`.

```java
Object data = openingHours.data();
Object mondayData = openingHours.forDay("monday").data();
```

`getData()` is still available on those classes, but deprecated, matching the original library where it is deprecated in favour of the `data` property.

#### `isOpenOn(String day)`

Checks if the business is open (contains at least 1 range of open hours) on a day in the regular schedule.

```java
openingHours.isOpenOn("saturday");
```

If the given string is a date, it will check if it's open (contains at least 1 range of open hours) considering both regular day schedule and possible exceptions.

```java
openingHours.isOpenOn("2020-09-03");
openingHours.isOpenOn("09-03"); // If year is omitted, current year is used instead
```

#### `isClosedOn(String day)`

Checks if the business is closed on a day in the regular schedule.

```java
openingHours.isClosedOn("sunday");
```

#### `isOpenAt(LocalDateTime dateTime)`

Checks if the business is open on a specific day, at a specific time.

```java
openingHours.isOpenAt(LocalDateTime.of(2016, 9, 26, 20, 0));
```

#### `isClosedAt(LocalDateTime dateTime)`

Checks if the business is closed on a specific day, at a specific time.

```java
openingHours.isClosedAt(LocalDateTime.of(2016, 9, 26, 20, 0));
```

#### `isOpen()`

Checks if the business is open right now.

```java
openingHours.isOpen();
```

#### `isClosed()`

Checks if the business is closed right now.

```java
openingHours.isClosed();
```

#### `isAlwaysOpen()`

Checks if the business is open 24/7, has no filters, and exceptions (if any) are open 24/7 as well.

```java
if (openingHours.isAlwaysOpen()) {
    System.out.println("This business is open all day long every day.");
}
```

#### `isAlwaysClosed()`

Checks if the business is never open, has no filters, and exceptions (if any) are closed as well.

`OpeningHours` accept empty map or list with every week day empty.

If it's not a valid state in your domain, you should use this method to throw an exception or show an error.

```java
if (openingHours.isAlwaysClosed()) {
    throw new RuntimeException("Opening hours missing");
}
```

#### `nextOpen(LocalDateTime dateTime)` / `nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil)` / `nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap)`

Returns next open `LocalDateTime` from the given `LocalDateTime` (or from now if parameter is null or omitted).

The search is limited to `getDayLimit()` days (8 by default, or 366 as soon as the schedule has exceptions). Set `searchUntil` to a date to throw a `SearchLimitReached` exception if no open time can be found before this moment. Set `cap` to a date so if no open time can be found before this moment, `cap` is returned. When neither is reachable before the day limit, a `MaximumLimitExceeded` exception is thrown.

```java
LocalDateTime nextOpen = openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 11, 0));
```

#### `setTimezone(ZoneId timezone)` / `setOutputTimezone(ZoneId outputTimezone)`

Sets the input and/or output timezone after construction.

```java
openingHours.setTimezone(ZoneId.of("Europe/Oslo"));
openingHours.setOutputTimezone(ZoneId.of("UTC"));
```

#### `setDayLimit(int dayLimit)` / `getDayLimit()`

Set the number of days to try before abandoning the search of the next close/open time.

```java
openingHours.setDayLimit(30);
```

#### `nextClose(LocalDateTime dateTime)` / `nextClose(LocalDateTime dateTime, LocalDateTime searchUntil)` / `nextClose(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap)`

Returns next close `LocalDateTime` from the given `LocalDateTime` (or from now if parameter is null or omitted).

Set `searchUntil` to a date to throw an exception if no closed time can be found before this moment.

Set `cap` to a date so if no closed time can be found before this moment, `cap` is returned.

If the schedule is always open or always closed, there is no state change to be found and therefore `nextOpen` (but also `previousOpen`, `nextClose` and `previousClose`) will throw a `MaximumLimitExceeded` exception. You can catch it and react accordingly or you can use `isAlwaysOpen()` / `isAlwaysClosed()` methods to anticipate such case.

```java
LocalDateTime nextClose = openingHours.nextClose(LocalDateTime.of(2016, 12, 24, 11, 0));
```

#### `previousOpen(LocalDateTime dateTime)` / with `searchUntil` and `cap` parameters

Returns previous open `LocalDateTime` from the given `LocalDateTime` (or from now if parameter is null or omitted).

```java
LocalDateTime previousOpen = openingHours.previousOpen(LocalDateTime.of(2016, 12, 24, 11, 0));
```

#### `previousClose(LocalDateTime dateTime)` / with `searchUntil` and `cap` parameters

Returns previous close `LocalDateTime` from the given `LocalDateTime` (or from now if parameter is null or omitted).

```java
LocalDateTime previousClose = openingHours.previousClose(LocalDateTime.of(2016, 12, 24, 11, 0));
```

#### `diffInOpenHours(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of open time (number of hours as a floating number) between 2 dates/times.

```java
double hours = openingHours.diffInOpenHours(
    LocalDateTime.of(2016, 12, 24, 11, 0),
    LocalDateTime.of(2016, 12, 24, 16, 34, 25)
);
```

#### `diffInOpenMinutes(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of open time (number of minutes as a floating number) between 2 dates/times.

#### `diffInOpenSeconds(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of open time (number of seconds as a floating number) between 2 dates/times.

#### `diffInClosedHours(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of closed time (number of hours as a floating number) between 2 dates/times.

```java
double hours = openingHours.diffInClosedHours(
    LocalDateTime.of(2016, 12, 24, 11, 0),
    LocalDateTime.of(2016, 12, 24, 16, 34, 25)
);
```

#### `diffInClosedMinutes(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of closed time (number of minutes as a floating number) between 2 dates/times.

#### `diffInClosedSeconds(LocalDateTime startDate, LocalDateTime endDate)`

Return the amount of closed time (number of seconds as a floating number) between 2 dates/times.

#### `currentOpenRange(LocalDateTime dateTime)`

Returns an `Optional<TimeRange>` of the current open range if the business is open, empty if the business is closed.

```java
Optional<TimeRange> range = openingHours.currentOpenRange(LocalDateTime.of(2016, 12, 24, 11, 0));

if (range.isPresent()) {
    System.out.println("It's open since " + range.get().start());
    System.out.println("It will close at " + range.get().end());
} else {
    System.out.println("It's closed");
}
```

`start()` and `end()` methods return `Time` instances. `Time` instances created from a date can be formatted with date information. This is useful for ranges overflowing midnight.

#### `currentOpenRangeStart(LocalDateTime dateTime)`

Returns an `Optional<LocalDateTime>` of the date and time since when the business is open if the business is open, empty if the business is closed.

Note: date can be the previous day if you use night ranges.

```java
Optional<LocalDateTime> start = openingHours.currentOpenRangeStart(LocalDateTime.of(2016, 12, 24, 11, 0));

if (start.isPresent()) {
    System.out.println("It's open since " + start.get().format(DateTimeFormatter.ofPattern("HH:mm")));
} else {
    System.out.println("It's closed");
}
```

#### `currentOpenRangeEnd(LocalDateTime dateTime)`

Returns an `Optional<LocalDateTime>` of the date and time until when the business will be open if the business is open, empty if the business is closed.

Note: date can be the next day if you use night ranges.

```java
Optional<LocalDateTime> end = openingHours.currentOpenRangeEnd(LocalDateTime.of(2016, 12, 24, 11, 0));

if (end.isPresent()) {
    System.out.println("It will close at " + end.get().format(DateTimeFormatter.ofPattern("HH:mm")));
} else {
    System.out.println("It's closed");
}
```

#### `createFromStructuredData(List<Map<String, Object>> specifications)` / with timezone parameters

Static factory method to fill the set with a list of https://schema.org/OpeningHoursSpecification objects. There is no JSON dependency: parse your JSON into a list of maps first.

`dayOfWeek` supports a single day name or a list of day names (Google-flavored), or a list of day URLs (official schema.org specification). `overflow` is enabled by default since the schema allows ranges spilling over midnight.

```java
List<Map<String, Object>> specifications = new ArrayList<>();

Map<String, Object> weekday = new HashMap<>();
weekday.put("@type", "OpeningHoursSpecification");
weekday.put("opens", "08:00");
weekday.put("closes", "12:00");
weekday.put("dayOfWeek", Arrays.asList(
    "https://schema.org/Monday",
    "https://schema.org/Tuesday",
    "https://schema.org/Wednesday",
    "https://schema.org/Thursday",
    "https://schema.org/Friday"
));
specifications.add(weekday);

Map<String, Object> holiday = new HashMap<>();
holiday.put("@type", "OpeningHoursSpecification");
holiday.put("opens", "00:00");
holiday.put("closes", "00:00");
holiday.put("validFrom", "2023-12-25");
holiday.put("validThrough", "2023-12-25");
specifications.add(holiday);

OpeningHours openingHours = OpeningHours.createFromStructuredData(specifications);
```

An `InvalidOpeningHoursSpecification` exception is thrown for unsupported days (`PublicHolidays`), invalid hours or invalid ranges.

#### `asStructuredData()` / `asStructuredData(String format)` / `asStructuredData(String format, ZoneId timezone)`

Returns a list of [OpeningHoursSpecification](https://schema.org/openingHoursSpecification) maps, regular hours first, then exceptions.

```java
List<Map<String, Object>> structuredData = openingHours.asStructuredData();
// Customize time format, could be "HH:mm:ss", "hh:mm a", "H:mm", etc.
structuredData = openingHours.asStructuredData("HH:mm:ss");
// Add a timezone offset, computed on a fixed reference date (1970-01-01)
structuredData = openingHours.asStructuredData("HH:mm:ssxxx", ZoneId.of("Europe/Paris")); // "17:00:00+01:00"
```

#### `asStructuredJson()` / `asStructuredJson(String format)` / `asStructuredJson(String format, ZoneId timezone)`

Same as `asStructuredData(...)`, serialized to a JSON string so it can be embedded directly (no JSON library needed):

```java
String json = openingHours.asStructuredJson();
// [{"@type":"OpeningHoursSpecification","dayOfWeek":"Monday","opens":"09:00","closes":"17:00"}]

String withOffset = openingHours.asStructuredJson("HH:mm:ssxxx", ZoneId.of("Europe/Paris"));
```

### `io.github.wuwx.openinghours.OpeningHoursForDay`

This class is meant as read-only. It is `Iterable<TimeRange>`, and also supports list-like access so you can process the list of `TimeRange`s (`get(int)`, `size()`, `getTimeRanges()`).

Ranges are sorted by start time, and creating a day with overlapping ranges throws an `OverlappingTimeRanges` exception.

It also exposes time based lookups: `isOpenAt(Time)`, `isOpenAtTheEndOfTheDay()`, `isOpenAtNight(Time)`, `nextOpen(Time)`, `nextOpenRange(Time)`, `nextClose(Time)`, `nextCloseRange(Time)`, `previousOpen(Time)`, `previousOpenRange(Time)`, `previousClose(Time)`, `previousCloseRange(Time)`, `forTime(Time)` and `forNightTime(Time)`, plus `map(Function<TimeRange, T>)` to transform the ranges:

```java
List<Integer> hours = openingHours.forDay("monday").map(range -> range.start().hours());
```

### `io.github.wuwx.openinghours.TimeRange`

Value object describing a period with a start and an end time. Can be converted to a string in a `HH:mm-HH:mm` format, or formatted explicitly with `format(String timeFormat)` / `format(String timeFormat, String rangeFormat)`:

```java
TimeRange range = TimeRange.fromString("09:00-18:00");
range.format("H:mm");                          // "9:00-18:00"
range.format("HH:mm", "from %s to %s");        // "from 09:00 to 18:00"

TimeRange.fromMidnight(Time.fromString("02:00")); // "00:00-02:00"
```

A range is "reversed" (`overflowsNextDay()`) when its end is before its start, e.g. `22:00-02:00`. The special end time `24:00` is supported and means midnight, included.

Ranges can be built from several shapes: `fromString("09:00-18:00")`, `fromArray(Map)` for a `{hours, data}` definition, `fromDefinition(Object)` for a string, map or existing range, `fromMidnight(Time)` for `00:00-end`, and `fromList(List<TimeRange>)` spanning from the earliest start to the latest end.

### `io.github.wuwx.openinghours.DateTimeRange`

A `TimeRange` projected on the calendar. `currentOpenRange(ZonedDateTime)` returns one, with `start()` / `end()` resolved to concrete `ZonedDateTime`s so a range overflowing midnight points to the right day.

`DateTimeRange.fromTimeRange(ZonedDateTime, TimeRange)` performs that projection on demand, which answers "when does this range actually run around that moment?":

```java
TimeRange evening = TimeRange.fromString("22:00-02:00");

DateTimeRange range = DateTimeRange.fromTimeRange(
        ZonedDateTime.of(2020, 1, 6, 23, 0, 0, 0, ZoneId.of("UTC")),
        evening);
// start: 2020-01-06T22:00Z, end: 2020-01-07T02:00Z
```

The given date time is the reference point: a start later than it lands on the previous day, an end earlier than it lands on the next day.

Unlike the original library, where `DateTimeRange` extends `TimeRange` and stores the dates inside its `Time`s, this class composes a `TimeRange` and exposes the resolved moments.

### `io.github.wuwx.openinghours.Time`

Value object describing a single time. Can be converted to a string in a `HH:mm` format, with `24:00` as a valid value and `Time.TIME_FORMAT` / `Time.MIDNIGHT` as the shared constants.

`toDateTime(LocalDate)` applies the time to a concrete date and returns a `LocalDateTime`; the special value `24:00` yields midnight of the **next** day, so it stays distinct from `00:00`.

Comparisons keep sub-minute precision (`seconds()`), so `23:59:59` is after `23:59`, and `diff(Time)` returns a `Duration`:

```java
Duration duration = Time.fromString("09:00").diff(Time.fromString("10:30")); // PT1H30M
```

### `io.github.wuwx.openinghours.OpeningHoursSpecificationParser`

Parses a list of https://schema.org/OpeningHoursSpecification maps into an opening hours definition, which can then be passed to `OpeningHours.create`:

```java
Map<String, Object> definition =
        OpeningHoursSpecificationParser.createFromArray(specifications).getOpeningHours();

OpeningHours openingHours = OpeningHours.create(definition);
```

`OpeningHours.createFromStructuredData(...)` is the shortcut for this, enabling `overflow` as the schema allows ranges spilling over midnight.

### Exceptions

All exceptions extend `io.github.wuwx.openinghours.exceptions.Exception`:

| Exception | Thrown when |
| --- | --- |
| `InvalidDayName` | a day key is not a valid english day name |
| `InvalidDate` | an exception key is not a valid `yyyy-MM-dd` or `MM-dd` date |
| `InvalidDateRange` | a definition would override an already defined day or date |
| `InvalidTimeString` / `InvalidTimeRangeString` / `InvalidTimeRangeArray` / `InvalidTimeRangeList` | an hours definition is malformed |
| `OverlappingTimeRanges` | two ranges of the same day overlap |
| `NonMutableOffsets` | a read-only collection, such as `OpeningHoursForDay#getTimeRanges()`, is modified |
| `InvalidDateTimeClass` | the definition carries a `dateTimeClass` key, which cannot be honoured |
| `InvalidTimezone` | a timezone string cannot be parsed |
| `InvalidOpeningHoursSpecification` | a schema.org specification is invalid |
| `SearchLimitReached` | `searchUntil` was reached during a next/previous search |
| `MaximumLimitExceeded` | the day limit was reached during a next/previous search |

## Testing

```bash
mvn test
```

The test classes are organised by concern:

| Test class | Covers |
| --- | --- |
| `OpeningHoursTest` | week/day/date lookups, next/previous, diff, limits, timezone setup |
| `OpeningHoursFillTest` | building from definitions, data, filters, aggregations, merging ranges |
| `OpeningHoursOverflowTest` | overflow (night ranges leaking into the next day) |
| `OpeningHoursStructuredDataTest` | `asStructuredData()` and round-tripping |
| `OpeningHoursSpecificationParserTest` | `createFromStructuredData()` |
| `OpeningHoursCustomClassTest` | timezones |
| `OpeningHoursForDayTest` | per-day ranges and lookups |
| `TimeTest` / `PreciseTimeTest` / `TimeRangeTest` | value objects, including sub-minute precision |
| `TestSchedules` | shared test helpers |

## License

The MIT License (MIT). Please see [License File](LICENSE.md) for more information.

