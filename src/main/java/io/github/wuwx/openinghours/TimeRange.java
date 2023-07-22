package io.github.wuwx.openinghours;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * @author wuwx
 */
public class TimeRange {

    private DateTime start;

    private DateTime end;

    public TimeRange(DateTime start, DateTime end) {
        this.start = start;
        this.end = end;
    }

    public DateTime getStart() {
        return start;
    }

    public DateTime getEnd() {
        return end;
    }

    public static TimeRange fromString(String openingHour) {
        List<String> times = StrUtil.split(openingHour, "-");
        return new TimeRange(DateUtil.parse(times.get(0), "HH:mm"), DateUtil.parse(times.get(1), "HH:mm"));
    }

    public boolean containsTime(DateTime dateTime) {
        return dateTime.after(this.start) && dateTime.before(this.end);
    }
}
