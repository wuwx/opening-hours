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

This allows the API to look at previous day's data to check if the opening hours are open from its time range.

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

System.out.println(openingHours.forDay("monday").getData()); // Typical Monday
System.out.println(openingHours.forDate(LocalDateTime.of(2016, 12, 25, 0, 0)).getData()); // Closed for Christmas
System.out.println(openingHours.forDay("tuesday").get(2).getData()); // Extra on Tuesday evening
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
import java.time.LocalDate;

Map<String, Object> data = new HashMap<>();
data.put("monday", Arrays.asList("09:00-12:00"));

List<Function<LocalDate, Map<String, Object>>> filters = new ArrayList<>();
filters.add(date -> {
    // Example: Close on Easter Monday
    // (You would need to implement Easter calculation)
    if (isEasterMonday(date)) {
        return Collections.emptyMap(); // Closed on Easter Monday
        // Any valid exception map can be returned here (range of hours, with or without data)
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

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.wuwx</groupId>
    <artifactId>opening-hours</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

The package should only be used through the `OpeningHours` class. There are also three value object classes used throughout, `Time`, which represents a single time, `TimeRange`, which represents a period with a start and an end, and `OpeningHoursForDay`, which represents a set of `TimeRange`s which can't overlap.

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

If no timezone is specified, `OpeningHours` will just assume you always pass `LocalDateTime` objects that have already the timezone matching your schedule.

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

#### `OpeningHours.mergeOverlappingRanges(Map<String, List<String>> schedule)`

For safety sake, creating `OpeningHours` object with overlapping ranges will throw an exception unless you pass explicitly `overflow = true` in the opening hours map definition. You can also explicitly merge them.

```java
Map<String, List<String>> ranges = new HashMap<>();
ranges.put("monday", Arrays.asList("08:00-11:00", "10:00-12:00"));

Map<String, List<String>> mergedRanges = OpeningHours.mergeOverlappingRanges(ranges); 
// Monday becomes ["08:00-12:00"]

OpeningHours.create(mergedRanges);
// Or use the following shortcut to create from ranges that possibly overlap:
OpeningHours.createAndMergeOverlappingRanges(ranges);
```

Not all days are mandatory, if a day is missing, it will be set as closed.

#### `OpeningHours.fill(Map<String, Object> data)`

The same as `create`, but non-static.

```java
OpeningHours openingHours = new OpeningHours().fill(data);
```

#### `forWeek()`

Returns a `Map<String, OpeningHoursForDay>` for a regular week.

```java
Map<String, OpeningHoursForDay> week = openingHours.forWeek();
```

#### `forWeekCombined()`

Returns a map of days. Map key is first day with same hours, map values are days that have the same working hours and `OpeningHoursForDay` object.

```java
Map<String, Object> combined = openingHours.forWeekCombined();
```

#### `forWeekConsecutiveDays()`

Returns a map of concatenated days, adjacent days with the same hours. Map key is first day with same hours, map values are days that have the same working hours and `OpeningHoursForDay` object.

*Warning*: consecutive days are considered from Monday to Sunday without looping (Monday is not consecutive to Sunday) no matter the days order in initial data.

```java
Map<String, Object> consecutive = openingHours.forWeekConsecutiveDays();
```

#### `forDay(String day)`

Returns an `OpeningHoursForDay` object for a regular day. A day is lowercase string of the english day name.

```java
OpeningHoursForDay monday = openingHours.forDay("monday");
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

Checks if the business is open 24/7, has no exceptions and no filters.

```java
if (openingHours.isAlwaysOpen()) {
    System.out.println("This business is open all day long every day.");
}
```

#### `isAlwaysClosed()`

Checks if the business is never open, has no exceptions and no filters.

`OpeningHours` accept empty map or list with every week day empty.

If it's not a valid state in your domain, you should use this method to throw an exception or show an error.

```java
if (openingHours.isAlwaysClosed()) {
    throw new RuntimeException("Opening hours missing");
}
```

#### `nextOpen(LocalDateTime dateTime)` / `nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil)` / `nextOpen(LocalDateTime dateTime, LocalDateTime searchUntil, LocalDateTime cap)`

Returns next open `LocalDateTime` from the given `LocalDateTime` (or from now if parameter is null or omitted).

Set `searchUntil` to a date to throw an exception if no open time can be found before this moment.

Set `cap` to a date so if no open time can be found before this moment, `cap` is returned.

```java
LocalDateTime nextOpen = openingHours.nextOpen(LocalDateTime.of(2016, 12, 24, 11, 0));
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

#### `createFromStructuredData(String jsonData)` / with timezone parameters

Static factory method to fill the set with a https://schema.org/OpeningHoursSpecification JSON string.

`dayOfWeek` supports array of day names (Google-flavored) or array of day URLs (official schema.org specification).

```java
String json = "[" +
    "{" +
    "  \"@type\": \"OpeningHoursSpecification\"," +
    "  \"opens\": \"08:00\"," +
    "  \"closes\": \"12:00\"," +
    "  \"dayOfWeek\": [" +
    "    \"https://schema.org/Monday\"," +
    "    \"https://schema.org/Tuesday\"," +
    "    \"https://schema.org/Wednesday\"," +
    "    \"https://schema.org/Thursday\"," +
    "    \"https://schema.org/Friday\"" +
    "  ]" +
    "}," +
    "{" +
    "  \"@type\": \"OpeningHoursSpecification\"," +
    "  \"opens\": \"14:00\"," +
    "  \"closes\": \"18:00\"," +
    "  \"dayOfWeek\": [" +
    "    \"Monday\"," +
    "    \"Tuesday\"," +
    "    \"Wednesday\"," +
    "    \"Thursday\"," +
    "    \"Friday\"" +
    "  ]" +
    "}," +
    "{" +
    "  \"@type\": \"OpeningHoursSpecification\"," +
    "  \"opens\": \"00:00\"," +
    "  \"closes\": \"00:00\"," +
    "  \"validFrom\": \"2023-12-25\"," +
    "  \"validThrough\": \"2023-12-25\"" +
    "}" +
    "]";

OpeningHours openingHours = OpeningHours.createFromStructuredData(json);
```

#### `asStructuredData()` / `asStructuredData(String format)` / `asStructuredData(String format, ZoneId timezone)`

Returns a list of [OpeningHoursSpecification](https://schema.org/openingHoursSpecification) maps.

```java
List<Map<String, Object>> structuredData = openingHours.asStructuredData();
// Customize time format, could be "HH:mm:ss", "hh:mm a", "H:mm", etc.
structuredData = openingHours.asStructuredData("HH:mm:ss");
// Add a timezone
structuredData = openingHours.asStructuredData("HH:mmXXX", ZoneId.of("-05:00"));
```

### `io.github.wuwx.openinghours.OpeningHoursForDay`

This class is meant as read-only. It supports list-like access so you can process the list of `TimeRange`s.

### `io.github.wuwx.openinghours.TimeRange`

Value object describing a period with a start and an end time. Can be converted to a string in a `HH:mm-HH:mm` format.

### `io.github.wuwx.openinghours.Time`

Value object describing a single time. Can be converted to a string in a `HH:mm` format.

## Testing

```bash
mvn test
```

## License

The MIT License (MIT). Please see [License File](LICENSE.md) for more information.

