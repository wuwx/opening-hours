package io.github.wuwx.openinghours;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wuwx
 */
public class OpeningHoursForDay {
    public List<TimeRange> timeRanges;

    public OpeningHoursForDay() {
        this.timeRanges = new ArrayList<>();
    }

    public static OpeningHoursForDay fromStrings(List<String> openingHours) {
        OpeningHoursForDay openingHoursForDay = new OpeningHoursForDay();
        for (String openingHour : openingHours) {
            openingHoursForDay.timeRanges.add(TimeRange.fromString(openingHour));
        }
        return openingHoursForDay;
    }

    public boolean isOpenAt(DateTime dateTime) {
        for (TimeRange timeRange : this.timeRanges) {
            if (timeRange.containsTime(DateUtil.parseTime(dateTime.toString("HH:mm:ss")))) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.timeRanges.isEmpty();
    }
}
