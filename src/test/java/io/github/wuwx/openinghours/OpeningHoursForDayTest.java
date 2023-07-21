package io.github.wuwx.openinghours;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import junit.framework.TestCase;

public class OpeningHoursForDayTest extends TestCase {
    public void testFromStrings() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(ListUtil.of("08:00-09:00"));
        assertEquals(openingHoursForDay1.openingHours.size(), 1);

        OpeningHoursForDay openingHoursForDay2 = OpeningHoursForDay.fromStrings(ListUtil.of("08:00-09:00", "10:00-11:00"));
        assertEquals(openingHoursForDay2.openingHours.size(), 2);
    }

    public void testIsEmpty() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(ListUtil.of("08:00-09:00"));
        assertFalse(openingHoursForDay1.isEmpty());

        OpeningHoursForDay openingHoursForDay2 = OpeningHoursForDay.fromStrings(ListUtil.of());
        assertTrue(openingHoursForDay2.isEmpty());
    }

    public void testIsOpenAt() {
        OpeningHoursForDay openingHoursForDay1 = OpeningHoursForDay.fromStrings(ListUtil.of("08:00-09:00"));
        assertTrue(openingHoursForDay1.isOpenAt(DateUtil.parseTime("08:30:00")));
        assertFalse(openingHoursForDay1.isOpenAt(DateUtil.parseTime("09:30:00")));
    }
}