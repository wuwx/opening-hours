# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-10-04

This release brings the public API and the behaviour back in line with the original PHP library
([spatie/opening-hours](https://github.com/spatie/opening-hours)) and fixes several correctness bugs
that made 1.0.0 return wrong results.

**It contains breaking changes.** See *Removed*, *Changed* and *Deprecated* below, and the
*Upgrading from 1.x* section of the README for the short version.

### Added

- Timezone-aware API: `ZonedDateTime` overloads of `isOpenAt`, `isClosedAt`, `forDate`,
  `forDateTime`, `nextOpen`, `nextClose`, `previousOpen`, `previousClose`, `currentOpenRange`,
  `currentOpenRangeStart` and `currentOpenRangeEnd`, plus `setTimezone`, `setOutputTimezone`,
  `getTimezone` and `getOutputTimezone`.
- `OpeningHours`: `isValid(Map)`, `data()`, `setDayLimit(int)`, `getDayLimit()`, `getFilters()`,
  `regularClosingDays()`, `regularClosingDaysISO()`, `exceptionalClosingDates()`,
  `forDay(DayOfWeek)`, `forDateTime(...)`, `asStructuredJson()` in three flavours,
  `createAndMergeOverlappingRanges` overloads taking timezones and `ignoreData`, and the batch
  helpers `filter`, `map`, `flatMap`, `every` along with their `*Exceptions` counterparts.
- `OpeningHoursForDay`: `isOpenAtTheEndOfTheDay()`, `isOpenAtNight(Time)`, `nextOpenRange`,
  `nextCloseRange`, `previousOpenRange`, `previousCloseRange`, `forTime(Time)`, `forNightTime(Time)`,
  `map(Function)`, `iterator()`, `get(int)`, `size()`, `data()`, `equals` and `hashCode`.
- `TimeRange`: `fromArray(Map)`, `fromDefinition(Object)`, `fromMidnight(Time[, Object])`,
  `fromList(List[, Object])`, `isReversed()`, `overflowsNextDay()`, `spillsOverToNextDay()`,
  `containsTime(Time)`, `containsNightTime(Time)`, and `format()` / `format(String)` /
  `format(String, String)`.
- `Time`: `fromLocalTime(LocalTime)`, `fromDateTime(LocalDateTime)`, `hours()`, `minutes()`,
  `seconds()`, `isSame`, `isSameOrBefore`, `compareTo(Time)`, `diff(Time)`, and
  `toDateTime(LocalDate)` — the counterpart of the original `Time::toDateTime()`, where `24:00`
  resolves to midnight of the **next** day.
- New classes: `TimeDataContainer` (carrying `TIME_FORMAT` and `MIDNIGHT`, so `Time` inherits them
  exactly as in the original library), `DateTimeRange` (with `fromTimeRange(...)`), and
  `OpeningHoursSpecificationParser`.
- New exception classes, matching the original library one for one: `InvalidDate`,
  `InvalidDateRange`, `InvalidDateTimeClass`, `InvalidDayName`, `InvalidOpeningHoursSpecification`,
  `InvalidTimeRangeArray`, `InvalidTimeRangeList`, `InvalidTimeRangeString`, `InvalidTimeString`,
  `InvalidTimezone`, `NonMutableOffsets` and `SearchLimitReached`.
- Javadoc for every public type and member, and a `javadoc` artifact alongside `sources`.

### Changed

- `mergeOverlappingRanges(...)` now takes and returns `Map<String, Object>` and accepts the
  `ignoreData` and `excludedKeys` parameters, with defaults matching the original library.
- `createFromStructuredData(...)` now takes `List<Map<String, Object>>` instead of a JSON string;
  parsing JSON is left to the caller so that the library keeps no runtime dependency.
- `24:00` now means midnight *included* and survives formatting: `09:00-24:00` is no longer
  considered open at 08:00, `nextClose` returns midnight instead of `23:59`, and parsing then
  formatting no longer rewrites `09:00-24:00` into `09:00-23:59`.
- Ranges spilling over midnight (`22:00-02:00`) are only taken into account for the next day when
  `overflow: true` is set.
- Invalid definitions now throw instead of being silently skipped: overlapping ranges, a day or a
  date defined twice, invalid day names, invalid dates and malformed hours.
- Filters given in the definition are now applied; in 1.0.0 their return value was dropped.
- `asStructuredData()` and `createFromStructuredData()` are implemented; they used to return an
  empty list and an all-closed instance.
- The search limit is 8 days by default (366 when the definition holds exceptions) and is
  configurable with `setDayLimit()`; reaching `searchUntil` raises `SearchLimitReached`, while
  running out of days raises `MaximumLimitExceeded`.
- `isAlwaysOpen()` and `isAlwaysClosed()` now also accept exceptions that are open around the clock
  or closed all day.
- `OpeningHoursForDay#getTimeRanges()` still refuses modifications, but throws `NonMutableOffsets`
  instead of the generic `UnsupportedOperationException`.
- A `dateTimeClass` key raises `InvalidDateTimeClass` instead of being rejected as an invalid day
  name.
- `forWeekCombined()` entries hold the shared hours under `opening_hours` rather than `hours`.
- `Time` comparisons keep sub-minute precision, so `23:59:59` sorts after `23:59`.

### Deprecated

- `getData()` on `OpeningHours`, `OpeningHoursForDay`, `TimeRange` and `DateTimeRange`; use
  `data()` instead. The original library deprecates `getData()` in favour of the `data` property in
  the same way.

### Removed

- `OpeningHours#fill(Map)`; it never existed in the original library. Use
  `create(data, timezone, outputTimezone)`.
- The `Time(LocalTime)` constructor; use `Time.fromLocalTime(LocalTime)`.
- `Time#toLocalTime()`; use `Time#toDateTime(LocalDate)`, or `hours()`, `minutes()` and `seconds()`.
- The public `TimeRange(Time, Time)` and `TimeRange(Time, Time, Object)` constructors; use the
  `from*` factories.

### Fixed

- `Time` is no longer modelled on `LocalTime.MAX`, which made a range such as `09:00-24:00` count as
  open at every hour of the morning.
- `nextClose` and `currentOpenRangeEnd` no longer land on the wrong day for ranges ending at `24:00`.
- `nextOpen`, `nextClose`, `previousOpen` and `previousClose` follow the original library's search
  order and honour `overflow`.
- Recurring date ranges such as `02-28 to 03-01` are expanded on a leap year, so they no longer fail
  and get swallowed.
- The configured `timezone` and `outputTimezone` now actually affect the `ZonedDateTime` based
  calls; in 1.0.0 they were stored but never used.
- Overlapping ranges, duplicate day definitions and invalid configuration are reported through
  dedicated exceptions instead of producing silently wrong results.

## [1.0.0] - 2025-11-03

First release.
