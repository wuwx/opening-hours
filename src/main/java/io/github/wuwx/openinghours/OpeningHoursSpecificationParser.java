package io.github.wuwx.openinghours;

import io.github.wuwx.openinghours.exceptions.InvalidOpeningHoursSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a list of https://schema.org/OpeningHoursSpecification objects into the data structure
 * accepted by {@link OpeningHours#createFromStructuredData(List)}.
 *
 * <p>Specifications carrying a {@code dayOfWeek} are converted to regular days, specifications
 * carrying {@code validFrom} and {@code validThrough} are converted to exceptions. Day names may be
 * Google-flavoured ({@code "Monday"}) or official schema.org URLs
 * ({@code "https://schema.org/Monday"}).</p>
 */
public final class OpeningHoursSpecificationParser {

    private final Map<String, Object> openingHours = new LinkedHashMap<>();

    private OpeningHoursSpecificationParser(List<?> specifications) {
        int index = 0;
        for (Object item : specifications) {
            try {
                if (!(item instanceof Map)) {
                    throw new InvalidOpeningHoursSpecification("OpeningHoursSpecification item must be an object");
                }
                parseItem(asMap(item));
            } catch (InvalidOpeningHoursSpecification e) {
                throw new InvalidOpeningHoursSpecification(
                        "Invalid openingHoursSpecification item at index " + index + ": " + e.getMessage());
            }
            index++;
        }
    }

    /**
     * Parses the given specifications.
     *
     * @param specifications the schema.org specification objects
     * @return a parser holding the resulting opening hours definition
     * @throws InvalidOpeningHoursSpecification if any specification is malformed
     */
    public static OpeningHoursSpecificationParser createFromArray(List<?> specifications) {
        return new OpeningHoursSpecificationParser(specifications);
    }

    /**
     * Returns the resulting definition, ready to be passed to {@link OpeningHours#create(Map)}.
     *
     * @return a map of day names and exceptions to hours definitions
     */
    public Map<String, Object> getOpeningHours() {
        return openingHours;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private void parseItem(Map<String, Object> item) {
        Object dayOfWeek = item.get("dayOfWeek");
        Object validFrom = item.get("validFrom");
        Object validThrough = item.get("validThrough");
        Object opens = item.get("opens");
        Object closes = item.get("closes");

        if (dayOfWeek != null) {
            List<?> days;
            if (dayOfWeek instanceof String) {
                days = Collections.singletonList(dayOfWeek);
            } else if (dayOfWeek instanceof List) {
                days = (List<?>) dayOfWeek;
            } else {
                throw new InvalidOpeningHoursSpecification("Property dayOfWeek must be a string or an array of strings");
            }
            for (Object day : days) {
                if (!(day instanceof String)) {
                    throw new InvalidOpeningHoursSpecification("Invalid https://schema.org/OpeningHoursSpecification dayOfWeek");
                }
                addDayOfWeekHours((String) day, opens, closes);
            }
            return;
        }

        if (!(validFrom instanceof String) || !(validThrough instanceof String)) {
            throw new InvalidOpeningHoursSpecification("Contains neither dayOfWeek nor validFrom and validThrough dates");
        }

        addExceptionsHours((String) validFrom, (String) validThrough, opens, closes);
    }

    private void addDayOfWeekHours(String dayOfWeek, Object opens, Object closes) {
        String day = schemaOrgDayToString(dayOfWeek);
        String hours = formatHours(opens, closes);
        if (hours == null) {
            return;
        }
        dayHours(day).add(hours);
    }

    @SuppressWarnings("unchecked")
    private List<String> dayHours(String day) {
        return (List<String>) openingHours.computeIfAbsent(day, k -> new ArrayList<String>());
    }

    @SuppressWarnings("unchecked")
    private void addExceptionsHours(String validFrom, String validThrough, Object opens, Object closes) {
        if (!validFrom.matches("^(?:\\d{4}-)?\\d{2}-\\d{2}$")) {
            throw new InvalidOpeningHoursSpecification("Invalid validFrom date");
        }
        if (!validThrough.matches("^(?:\\d{4}-)?\\d{2}-\\d{2}$")) {
            throw new InvalidOpeningHoursSpecification("Invalid validThrough date");
        }
        String exceptionKey = validFrom.equals(validThrough) ? validFrom : validFrom + " to " + validThrough;
        Map<String, List<String>> exceptions = (Map<String, List<String>>)
                openingHours.computeIfAbsent("exceptions", k -> new LinkedHashMap<String, List<String>>());
        exceptions.computeIfAbsent(exceptionKey, k -> new ArrayList<String>());
        String hours = formatHours(opens, closes);
        if (hours != null) {
            exceptions.get(exceptionKey).add(hours);
        }
    }

    private String formatHours(Object opens, Object closes) {
        if (opens == null) {
            if (closes != null) {
                throw new InvalidOpeningHoursSpecification("Property opens and closes must be both null or both string");
            }
            return null;
        }
        if (!(opens instanceof String) || !((String) opens).matches("^\\d{2}:\\d{2}(:\\d{2})?$")) {
            throw new InvalidOpeningHoursSpecification("Invalid opens hour");
        }
        if (!(closes instanceof String) || !((String) closes).matches("^\\d{2}:\\d{2}(:\\d{2})?$")) {
            throw new InvalidOpeningHoursSpecification("Invalid closes hours");
        }
        String open = ((String) opens).substring(0, 5);
        String close = ((String) closes).substring(0, 5);
        if (open.equals("00:00") && close.equals("00:00")) {
            return null;
        }
        return open + "-" + (close.equals("23:59") ? "24:00" : close);
    }

    private String schemaOrgDayToString(String schemaOrgDaySpec) {
        switch (schemaOrgDaySpec) {
            case "Monday":
            case "https://schema.org/Monday":
                return "monday";
            case "Tuesday":
            case "https://schema.org/Tuesday":
                return "tuesday";
            case "Wednesday":
            case "https://schema.org/Wednesday":
                return "wednesday";
            case "Thursday":
            case "https://schema.org/Thursday":
                return "thursday";
            case "Friday":
            case "https://schema.org/Friday":
                return "friday";
            case "Saturday":
            case "https://schema.org/Saturday":
                return "saturday";
            case "Sunday":
            case "https://schema.org/Sunday":
                return "sunday";
            case "PublicHolidays":
            case "https://schema.org/PublicHolidays":
                throw new InvalidOpeningHoursSpecification("PublicHolidays not supported");
            default:
                throw new InvalidOpeningHoursSpecification("Invalid https://schema.org Day specification");
        }
    }
}
