package io.github.wuwx.openinghours;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wuwx
 */
public class OpeningHoursForDay {
    public List<TimeRange> openingHours;

    public OpeningHoursForDay() {
        this.openingHours = new ArrayList<>();
    }

    public static OpeningHoursForDay fromStrings(List<String> openingHours) {
        OpeningHoursForDay openingHoursForDay = new OpeningHoursForDay();
        for (String openingHour : openingHours) {
            openingHoursForDay.openingHours.add(TimeRange.fromString(openingHour));
        }
        return openingHoursForDay;
    }

    public boolean isOpenAt(DateTime time) {
        for (TimeRange timeRange : this.openingHours) {
            if (DateUtil.parseTime(time.toString("HH:mm:ss")).after(timeRange.getStart()) && DateUtil.parseTime(time.toString("HH:mm:ss")).before(timeRange.getEnd())) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.openingHours.isEmpty();
    }
}
