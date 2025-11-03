package com.gcrf.library.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtil类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class DateUtilTest {

    @Test
    void testFormatDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 23, 14, 30, 0);
        String formatted = DateUtil.format(dateTime);

        assertEquals("2025-10-23 14:30:00", formatted);
    }

    @Test
    void testFormatDateTimeWithCustomPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 23, 14, 30, 0);
        String formatted = DateUtil.format(dateTime, "yyyy/MM/dd HH:mm");

        assertEquals("2025/10/23 14:30", formatted);
    }

    @Test
    void testFormatDate() {
        LocalDate date = LocalDate.of(2025, 10, 23);
        String formatted = DateUtil.formatDate(date);

        assertEquals("2025-10-23", formatted);
    }

    @Test
    void testFormatDateWithCustomPattern() {
        LocalDate date = LocalDate.of(2025, 10, 23);
        String formatted = DateUtil.formatDate(date, DateUtil.CHINESE_DATE_PATTERN);

        assertEquals("2025年10月23日", formatted);
    }

    @Test
    void testFormatTime() {
        LocalTime time = LocalTime.of(14, 30, 0);
        String formatted = DateUtil.formatTime(time);

        assertEquals("14:30:00", formatted);
    }

    @Test
    void testParseDateTime() {
        String dateTimeStr = "2025-10-23 14:30:00";
        LocalDateTime parsed = DateUtil.parseDateTime(dateTimeStr);

        assertNotNull(parsed);
        assertEquals(2025, parsed.getYear());
        assertEquals(10, parsed.getMonthValue());
        assertEquals(23, parsed.getDayOfMonth());
        assertEquals(14, parsed.getHour());
        assertEquals(30, parsed.getMinute());
    }

    @Test
    void testParseDateTimeWithCustomPattern() {
        String dateTimeStr = "2025/10/23 14:30";
        LocalDateTime parsed = DateUtil.parseDateTime(dateTimeStr, "yyyy/MM/dd HH:mm");

        assertNotNull(parsed);
        assertEquals(2025, parsed.getYear());
        assertEquals(10, parsed.getMonthValue());
        assertEquals(23, parsed.getDayOfMonth());
    }

    @Test
    void testParseDate() {
        String dateStr = "2025-10-23";
        LocalDate parsed = DateUtil.parseDate(dateStr);

        assertNotNull(parsed);
        assertEquals(2025, parsed.getYear());
        assertEquals(10, parsed.getMonthValue());
        assertEquals(23, parsed.getDayOfMonth());
    }

    @Test
    void testParseTime() {
        String timeStr = "14:30:00";
        LocalTime parsed = DateUtil.parseTime(timeStr);

        assertNotNull(parsed);
        assertEquals(14, parsed.getHour());
        assertEquals(30, parsed.getMinute());
        assertEquals(0, parsed.getSecond());
    }

    @Test
    void testPlusAndMinusDays() {
        LocalDate date = LocalDate.of(2025, 10, 23);

        LocalDate tomorrow = DateUtil.plusDays(date, 1);
        assertEquals(LocalDate.of(2025, 10, 24), tomorrow);

        LocalDate yesterday = DateUtil.minusDays(date, 1);
        assertEquals(LocalDate.of(2025, 10, 22), yesterday);
    }

    @Test
    void testPlusAndMinusMonths() {
        LocalDate date = LocalDate.of(2025, 10, 23);

        LocalDate nextMonth = DateUtil.plusMonths(date, 1);
        assertEquals(LocalDate.of(2025, 11, 23), nextMonth);

        LocalDate lastMonth = DateUtil.minusMonths(date, 1);
        assertEquals(LocalDate.of(2025, 9, 23), lastMonth);
    }

    @Test
    void testPlusAndMinusYears() {
        LocalDate date = LocalDate.of(2025, 10, 23);

        LocalDate nextYear = DateUtil.plusYears(date, 1);
        assertEquals(LocalDate.of(2026, 10, 23), nextYear);

        LocalDate lastYear = DateUtil.minusYears(date, 1);
        assertEquals(LocalDate.of(2024, 10, 23), lastYear);
    }

    @Test
    void testDaysBetween() {
        LocalDate startDate = LocalDate.of(2025, 10, 23);
        LocalDate endDate = LocalDate.of(2025, 10, 28);

        long days = DateUtil.daysBetween(startDate, endDate);
        assertEquals(5, days);

        long daysReverse = DateUtil.daysBetween(endDate, startDate);
        assertEquals(-5, daysReverse);
    }

    @Test
    void testHoursBetween() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 15, 0);

        long hours = DateUtil.hoursBetween(start, end);
        assertEquals(5, hours);
    }

    @Test
    void testMinutesBetween() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 30);

        long minutes = DateUtil.minutesBetween(start, end);
        assertEquals(30, minutes);
    }

    @Test
    void testToTimestamp() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 23, 14, 30, 0);
        long timestamp = DateUtil.toTimestamp(dateTime);

        assertTrue(timestamp > 0);
    }

    @Test
    void testFromTimestamp() {
        long timestamp = System.currentTimeMillis();
        LocalDateTime dateTime = DateUtil.fromTimestamp(timestamp);

        assertNotNull(dateTime);
        assertTrue(dateTime.getYear() >= 2025);
    }

    @Test
    void testCurrentTimestamp() {
        long timestamp = DateUtil.currentTimestamp();
        assertTrue(timestamp > 0);
    }

    @Test
    void testDateAndLocalDateTimeConversion() {
        LocalDateTime now = LocalDateTime.now();
        Date date = DateUtil.toDate(now);
        LocalDateTime converted = DateUtil.toLocalDateTime(date);

        assertNotNull(date);
        assertNotNull(converted);
        assertEquals(now.getYear(), converted.getYear());
        assertEquals(now.getMonth(), converted.getMonth());
        assertEquals(now.getDayOfMonth(), converted.getDayOfMonth());
    }

    @Test
    void testIsBefore() {
        LocalDate date1 = LocalDate.of(2025, 10, 23);
        LocalDate date2 = LocalDate.of(2025, 10, 24);

        assertTrue(DateUtil.isBefore(date1, date2));
        assertFalse(DateUtil.isBefore(date2, date1));
    }

    @Test
    void testIsAfter() {
        LocalDate date1 = LocalDate.of(2025, 10, 24);
        LocalDate date2 = LocalDate.of(2025, 10, 23);

        assertTrue(DateUtil.isAfter(date1, date2));
        assertFalse(DateUtil.isAfter(date2, date1));
    }

    @Test
    void testIsEqual() {
        LocalDate date1 = LocalDate.of(2025, 10, 23);
        LocalDate date2 = LocalDate.of(2025, 10, 23);

        assertTrue(DateUtil.isEqual(date1, date2));
    }

    @Test
    void testNowMethods() {
        LocalDate date = DateUtil.now();
        assertNotNull(date);

        LocalDateTime dateTime = DateUtil.nowDateTime();
        assertNotNull(dateTime);

        LocalTime time = DateUtil.nowTime();
        assertNotNull(time);
    }

    @Test
    void testStartAndEndOfDay() {
        LocalDate date = LocalDate.of(2025, 10, 23);

        LocalDateTime start = DateUtil.startOfDay(date);
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
        assertEquals(0, start.getSecond());

        LocalDateTime end = DateUtil.endOfDay(date);
        assertEquals(23, end.getHour());
        assertEquals(59, end.getMinute());
        assertEquals(59, end.getSecond());
    }

    @Test
    void testFirstAndLastDayOfMonth() {
        LocalDate date = LocalDate.of(2025, 10, 23);

        LocalDate firstDay = DateUtil.firstDayOfMonth(date);
        assertEquals(1, firstDay.getDayOfMonth());

        LocalDate lastDay = DateUtil.lastDayOfMonth(date);
        assertEquals(31, lastDay.getDayOfMonth());
    }

    @Test
    void testNullSafety() {
        assertNull(DateUtil.format(null));
        assertNull(DateUtil.formatDate(null));
        assertNull(DateUtil.parseDateTime(null));
        assertNull(DateUtil.parseDate(null));
        assertNull(DateUtil.plusDays((LocalDate) null, 1));
        assertEquals(0, DateUtil.daysBetween(null, LocalDate.now()));
        assertEquals(0, DateUtil.toTimestamp((LocalDateTime) null));
        assertNull(DateUtil.fromTimestamp(0));
    }

    @Test
    void testInvalidDateFormat() {
        String invalidDate = "not-a-date";
        LocalDate parsed = DateUtil.parseDate(invalidDate);
        assertNull(parsed);
    }
}
