package io.github.wuwx.openinghours;

import cn.hutool.core.date.DateUtil;
import junit.framework.TestCase;

public class TimeRangeTest extends TestCase {

    public void testFromString() {
        TimeRange timeRange = TimeRange.fromString("20:00-21:00");

        assertEquals(timeRange.getStart(), DateUtil.parse("20:00", "HH:mm"));
        assertEquals(timeRange.getEnd(), DateUtil.parse("21:00", "HH:mm"));

        assertEquals(timeRange.getStart(), DateUtil.parseTime("20:00:00"));
        assertEquals(timeRange.getEnd(), DateUtil.parseTime("21:00:00"));

        assertEquals(timeRange.getStart(), DateUtil.parse("1970-01-01 20:00:00"));
        assertEquals(timeRange.getEnd(), DateUtil.parse("1970-01-01 21:00:00"));
    }

}