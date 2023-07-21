package io.github.wuwx.openinghours;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author wuwx
 */
public class OpeningHours {

    private TimeZone timeZone;

    private TimeZone outputTimeZone;

    private Map<DayOfWeek, OpeningHoursForDay> openingHours;

    private Map<String, Object> exceptions;

    public OpeningHours(TimeZone timeZone, TimeZone outputTimeZone) {
        this.timeZone = timeZone;
        this.outputTimeZone = outputTimeZone;

        this.openingHours = MapUtil.createMap(HashMap.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            this.openingHours.put(dayOfWeek, null);
        }
    }

    public static OpeningHours create(Map<String, List<String>> data) {
        return new OpeningHours(DateUtil.calendar().getTimeZone(), DateUtil.calendar().getTimeZone()).fill(data);
    }

    public OpeningHours fill(Map<String, List<String>> data) {
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            this.setOpeningHoursFromStrings(entry.getKey(), entry.getValue());
        }
        MapUtil.map(data, (day, openingHoursForThisDay) -> {

            return openingHoursForThisDay;
        });
        return this;
    }

    public boolean isOpenAt(DateTime dateTime) {
        OpeningHoursForDay openingHoursForDay = MapUtil.get(this.openingHours, dateTime.dayOfWeekEnum().toJdkDayOfWeek(), OpeningHoursForDay.class);
        return openingHoursForDay.isOpenAt(dateTime);
    }

    public boolean isCloseAt(DateTime dateTime) {
        return !isOpenAt(dateTime);
    }

    public boolean isOpenOn(String day) {
        return !this.openingHours.get(DayOfWeek.valueOf(day.toUpperCase())).isEmpty();
    }

    public boolean isCloseOn(String day) {
        return !isOpenOn(day);
    }

    private void setOpeningHoursFromStrings(String day, List<String> openingHours) {
        this.openingHours.put(DayOfWeek.valueOf(day.toUpperCase()), OpeningHoursForDay.fromStrings(openingHours));
    }

    protected void setExceptionsFromStrings(Map<String, List<String>> exceptions) {
        this.exceptions = null;
    }
}
