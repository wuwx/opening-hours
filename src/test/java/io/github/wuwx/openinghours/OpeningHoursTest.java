package io.github.wuwx.openinghours;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class OpeningHoursTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public OpeningHoursTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(OpeningHoursTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testOpeningHours() {
        Map<String, List<String>> data = MapUtil.createMap(HashMap.class);

        data.put("monday", ListUtil.of("09:00-12:00"));
        data.put("tuesday", ListUtil.of("09:00-12:00"));
        data.put("wednesday", ListUtil.of("09:00-12:00"));
        data.put("thursday", ListUtil.of("09:00-12:00"));
        data.put("friday", ListUtil.of("09:00-12:00"));
        data.put("saturday", ListUtil.of());
        data.put("sunday", ListUtil.of());

        OpeningHours openingHours = OpeningHours.create(data);

        assertTrue(openingHours.isOpenOn("monday"));
        assertTrue(openingHours.isOpenOn("tuesday"));
        assertTrue(openingHours.isOpenOn("wednesday"));
        assertTrue(openingHours.isOpenOn("thursday"));
        assertTrue(openingHours.isOpenOn("friday"));
        assertFalse(openingHours.isOpenOn("saturday"));
        assertFalse(openingHours.isOpenOn("sunday"));

        assertFalse(openingHours.isCloseOn("monday"));
        assertFalse(openingHours.isCloseOn("tuesday"));
        assertFalse(openingHours.isCloseOn("wednesday"));
        assertFalse(openingHours.isCloseOn("thursday"));
        assertFalse(openingHours.isCloseOn("friday"));
        assertTrue(openingHours.isCloseOn("saturday"));
        assertTrue(openingHours.isCloseOn("sunday"));

        assertTrue(openingHours.isOpenAt(DateUtil.parse("2023-07-21 10:00:00")));
        assertFalse(openingHours.isOpenAt(DateUtil.parse("2023-07-22 10:00:00")));
        assertFalse(openingHours.isCloseAt(DateUtil.parse("2023-07-21 10:00:00")));
        assertTrue(openingHours.isCloseAt(DateUtil.parse("2023-07-22 10:00:00")));
    }

    public void testSetOpeningHoursFromStrings() {

    }
}
