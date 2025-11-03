package com.gcrf.library.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期工具类
 * <p>
 * 提供常用的日期时间操作，使用Java 8+的LocalDate/LocalDateTime API。
 * 线程安全，所有方法都是静态方法。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 格式化当前时间
 * String dateStr = DateUtil.format(LocalDateTime.now());
 * String dateOnlyStr = DateUtil.formatDate(LocalDate.now());
 *
 * // 解析字符串为日期
 * LocalDateTime dateTime = DateUtil.parseDateTime("2025-10-23 14:30:00");
 * LocalDate date = DateUtil.parseDate("2025-10-23");
 *
 * // 日期运算
 * LocalDate tomorrow = DateUtil.plusDays(LocalDate.now(), 1);
 * LocalDateTime nextWeek = DateUtil.plusWeeks(LocalDateTime.now(), 1);
 * LocalDate lastMonth = DateUtil.minusMonths(LocalDate.now(), 1);
 *
 * // 计算日期差异
 * long daysBetween = DateUtil.daysBetween(startDate, endDate);
 * long hoursBetween = DateUtil.hoursBetween(startDateTime, endDateTime);
 *
 * // 获取时间戳
 * long timestamp = DateUtil.toTimestamp(LocalDateTime.now());
 * LocalDateTime dateTime = DateUtil.fromTimestamp(System.currentTimeMillis());
 *
 * // 日期比较
 * boolean isBefore = DateUtil.isBefore(date1, date2);
 * boolean isAfter = DateUtil.isAfter(dateTime1, dateTime2);
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Slf4j
public class DateUtil {

    /**
     * 默认日期时间格式: yyyy-MM-dd HH:mm:ss
     */
    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式: yyyy-MM-dd
     */
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 默认时间格式: HH:mm:ss
     */
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    /**
     * ISO日期时间格式: yyyy-MM-dd'T'HH:mm:ss
     */
    public static final String ISO_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * 紧凑日期时间格式: yyyyMMddHHmmss
     */
    public static final String COMPACT_DATETIME_PATTERN = "yyyyMMddHHmmss";

    /**
     * 紧凑日期格式: yyyyMMdd
     */
    public static final String COMPACT_DATE_PATTERN = "yyyyMMdd";

    /**
     * 中文日期格式: yyyy年MM月dd日
     */
    public static final String CHINESE_DATE_PATTERN = "yyyy年MM月dd日";

    /**
     * 中文日期时间格式: yyyy年MM月dd日 HH时mm分ss秒
     */
    public static final String CHINESE_DATETIME_PATTERN = "yyyy年MM月dd日 HH时mm分ss秒";

    /**
     * 默认时区
     */
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * 私有构造函数，防止实例化
     */
    private DateUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 格式化方法 ====================

    /**
     * 格式化LocalDateTime为字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_DATETIME_PATTERN);
    }

    /**
     * 格式化LocalDateTime为字符串（自定义格式）
     *
     * @param dateTime 日期时间
     * @param pattern  格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            log.error("Failed to format LocalDateTime: {}, pattern: {}", dateTime, pattern, e);
            return null;
        }
    }

    /**
     * 格式化LocalDate为字符串（默认格式：yyyy-MM-dd）
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDate date) {
        return formatDate(date, DEFAULT_DATE_PATTERN);
    }

    /**
     * 格式化LocalDate为字符串（自定义格式）
     *
     * @param date    日期
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return date.format(formatter);
        } catch (Exception e) {
            log.error("Failed to format LocalDate: {}, pattern: {}", date, pattern, e);
            return null;
        }
    }

    /**
     * 格式化LocalTime为字符串（默认格式：HH:mm:ss）
     *
     * @param time 时间
     * @return 格式化后的字符串
     */
    public static String formatTime(LocalTime time) {
        return formatTime(time, DEFAULT_TIME_PATTERN);
    }

    /**
     * 格式化LocalTime为字符串（自定义格式）
     *
     * @param time    时间
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String formatTime(LocalTime time, String pattern) {
        if (time == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return time.format(formatter);
        } catch (Exception e) {
            log.error("Failed to format LocalTime: {}, pattern: {}", time, pattern, e);
            return null;
        }
    }

    // ==================== 解析方法 ====================

    /**
     * 解析字符串为LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTime(dateTimeStr, DEFAULT_DATETIME_PATTERN);
    }

    /**
     * 解析字符串为LocalDateTime（自定义格式）
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式模式
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse LocalDateTime: {}, pattern: {}", dateTimeStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析字符串为LocalDate（默认格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     * @return LocalDate对象
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DEFAULT_DATE_PATTERN);
    }

    /**
     * 解析字符串为LocalDate（自定义格式）
     *
     * @param dateStr 日期字符串
     * @param pattern 格式模式
     * @return LocalDate对象
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse LocalDate: {}, pattern: {}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析字符串为LocalTime（默认格式：HH:mm:ss）
     *
     * @param timeStr 时间字符串
     * @return LocalTime对象
     */
    public static LocalTime parseTime(String timeStr) {
        return parseTime(timeStr, DEFAULT_TIME_PATTERN);
    }

    /**
     * 解析字符串为LocalTime（自定义格式）
     *
     * @param timeStr 时间字符串
     * @param pattern 格式模式
     * @return LocalTime对象
     */
    public static LocalTime parseTime(String timeStr, String pattern) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalTime.parse(timeStr, formatter);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse LocalTime: {}, pattern: {}", timeStr, pattern, e);
            return null;
        }
    }

    // ==================== 日期运算方法 ====================

    /**
     * 增加天数
     *
     * @param date 日期
     * @param days 天数
     * @return 新的日期
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        return date == null ? null : date.plusDays(days);
    }

    /**
     * 增加天数
     *
     * @param dateTime 日期时间
     * @param days     天数
     * @return 新的日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.plusDays(days);
    }

    /**
     * 减少天数
     *
     * @param date 日期
     * @param days 天数
     * @return 新的日期
     */
    public static LocalDate minusDays(LocalDate date, long days) {
        return date == null ? null : date.minusDays(days);
    }

    /**
     * 减少天数
     *
     * @param dateTime 日期时间
     * @param days     天数
     * @return 新的日期时间
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.minusDays(days);
    }

    /**
     * 增加周数
     *
     * @param date  日期
     * @param weeks 周数
     * @return 新的日期
     */
    public static LocalDate plusWeeks(LocalDate date, long weeks) {
        return date == null ? null : date.plusWeeks(weeks);
    }

    /**
     * 增加周数
     *
     * @param dateTime 日期时间
     * @param weeks    周数
     * @return 新的日期时间
     */
    public static LocalDateTime plusWeeks(LocalDateTime dateTime, long weeks) {
        return dateTime == null ? null : dateTime.plusWeeks(weeks);
    }

    /**
     * 减少周数
     *
     * @param date  日期
     * @param weeks 周数
     * @return 新的日期
     */
    public static LocalDate minusWeeks(LocalDate date, long weeks) {
        return date == null ? null : date.minusWeeks(weeks);
    }

    /**
     * 减少周数
     *
     * @param dateTime 日期时间
     * @param weeks    周数
     * @return 新的日期时间
     */
    public static LocalDateTime minusWeeks(LocalDateTime dateTime, long weeks) {
        return dateTime == null ? null : dateTime.minusWeeks(weeks);
    }

    /**
     * 增加月数
     *
     * @param date   日期
     * @param months 月数
     * @return 新的日期
     */
    public static LocalDate plusMonths(LocalDate date, long months) {
        return date == null ? null : date.plusMonths(months);
    }

    /**
     * 增加月数
     *
     * @param dateTime 日期时间
     * @param months   月数
     * @return 新的日期时间
     */
    public static LocalDateTime plusMonths(LocalDateTime dateTime, long months) {
        return dateTime == null ? null : dateTime.plusMonths(months);
    }

    /**
     * 减少月数
     *
     * @param date   日期
     * @param months 月数
     * @return 新的日期
     */
    public static LocalDate minusMonths(LocalDate date, long months) {
        return date == null ? null : date.minusMonths(months);
    }

    /**
     * 减少月数
     *
     * @param dateTime 日期时间
     * @param months   月数
     * @return 新的日期时间
     */
    public static LocalDateTime minusMonths(LocalDateTime dateTime, long months) {
        return dateTime == null ? null : dateTime.minusMonths(months);
    }

    /**
     * 增加年数
     *
     * @param date  日期
     * @param years 年数
     * @return 新的日期
     */
    public static LocalDate plusYears(LocalDate date, long years) {
        return date == null ? null : date.plusYears(years);
    }

    /**
     * 增加年数
     *
     * @param dateTime 日期时间
     * @param years    年数
     * @return 新的日期时间
     */
    public static LocalDateTime plusYears(LocalDateTime dateTime, long years) {
        return dateTime == null ? null : dateTime.plusYears(years);
    }

    /**
     * 减少年数
     *
     * @param date  日期
     * @param years 年数
     * @return 新的日期
     */
    public static LocalDate minusYears(LocalDate date, long years) {
        return date == null ? null : date.minusYears(years);
    }

    /**
     * 减少年数
     *
     * @param dateTime 日期时间
     * @param years    年数
     * @return 新的日期时间
     */
    public static LocalDateTime minusYears(LocalDateTime dateTime, long years) {
        return dateTime == null ? null : dateTime.minusYears(years);
    }

    /**
     * 增加小时数
     *
     * @param dateTime 日期时间
     * @param hours    小时数
     * @return 新的日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime == null ? null : dateTime.plusHours(hours);
    }

    /**
     * 减少小时数
     *
     * @param dateTime 日期时间
     * @param hours    小时数
     * @return 新的日期时间
     */
    public static LocalDateTime minusHours(LocalDateTime dateTime, long hours) {
        return dateTime == null ? null : dateTime.minusHours(hours);
    }

    /**
     * 增加分钟数
     *
     * @param dateTime 日期时间
     * @param minutes  分钟数
     * @return 新的日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime == null ? null : dateTime.plusMinutes(minutes);
    }

    /**
     * 减少分钟数
     *
     * @param dateTime 日期时间
     * @param minutes  分钟数
     * @return 新的日期时间
     */
    public static LocalDateTime minusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime == null ? null : dateTime.minusMinutes(minutes);
    }

    /**
     * 增加秒数
     *
     * @param dateTime 日期时间
     * @param seconds  秒数
     * @return 新的日期时间
     */
    public static LocalDateTime plusSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime == null ? null : dateTime.plusSeconds(seconds);
    }

    /**
     * 减少秒数
     *
     * @param dateTime 日期时间
     * @param seconds  秒数
     * @return 新的日期时间
     */
    public static LocalDateTime minusSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime == null ? null : dateTime.minusSeconds(seconds);
    }

    // ==================== 日期差异计算方法 ====================

    /**
     * 计算两个日期之间相差的天数
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 相差天数（可能为负数）
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * 计算两个日期时间之间相差的天数
     *
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 相差天数（可能为负数）
     */
    public static long daysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDateTime, endDateTime);
    }

    /**
     * 计算两个日期时间之间相差的小时数
     *
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 相差小时数（可能为负数）
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    /**
     * 计算两个日期时间之间相差的分钟数
     *
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 相差分钟数（可能为负数）
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    /**
     * 计算两个日期时间之间相差的秒数
     *
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 相差秒数（可能为负数）
     */
    public static long secondsBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(startDateTime, endDateTime);
    }

    // ==================== 时间戳转换方法 ====================

    /**
     * LocalDateTime转换为时间戳（毫秒）
     *
     * @param dateTime 日期时间
     * @return 时间戳（毫秒）
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * LocalDate转换为时间戳（毫秒，时间部分为00:00:00）
     *
     * @param date 日期
     * @return 时间戳（毫秒）
     */
    public static long toTimestamp(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.atStartOfDay(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 时间戳（毫秒）转换为LocalDateTime
     *
     * @param timestamp 时间戳（毫秒）
     * @return LocalDateTime对象
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE_ID);
    }

    /**
     * 时间戳（毫秒）转换为LocalDate
     *
     * @param timestamp 时间戳（毫秒）
     * @return LocalDate对象
     */
    public static LocalDate fromTimestampToDate(long timestamp) {
        if (timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE_ID).toLocalDate();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前时间戳（秒）
     */
    public static long currentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    // ==================== Date与LocalDateTime互转方法 ====================

    /**
     * Date转换为LocalDateTime
     *
     * @param date Date对象
     * @return LocalDateTime对象
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * Date转换为LocalDate
     *
     * @param date Date对象
     * @return LocalDate对象
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    }

    /**
     * LocalDateTime转换为Date
     *
     * @param dateTime LocalDateTime对象
     * @return Date对象
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * LocalDate转换为Date（时间部分为00:00:00）
     *
     * @param date LocalDate对象
     * @return Date对象
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(DEFAULT_ZONE_ID).toInstant());
    }

    // ==================== 日期比较方法 ====================

    /**
     * 判断日期1是否在日期2之前
     *
     * @param date1 日期1
     * @param date2 日期2
     * @return true: 日期1在日期2之前
     */
    public static boolean isBefore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isBefore(date2);
    }

    /**
     * 判断日期时间1是否在日期时间2之前
     *
     * @param dateTime1 日期时间1
     * @param dateTime2 日期时间2
     * @return true: 日期时间1在日期时间2之前
     */
    public static boolean isBefore(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.isBefore(dateTime2);
    }

    /**
     * 判断日期1是否在日期2之后
     *
     * @param date1 日期1
     * @param date2 日期2
     * @return true: 日期1在日期2之后
     */
    public static boolean isAfter(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isAfter(date2);
    }

    /**
     * 判断日期时间1是否在日期时间2之后
     *
     * @param dateTime1 日期时间1
     * @param dateTime2 日期时间2
     * @return true: 日期时间1在日期时间2之后
     */
    public static boolean isAfter(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.isAfter(dateTime2);
    }

    /**
     * 判断两个日期是否相等
     *
     * @param date1 日期1
     * @param date2 日期2
     * @return true: 两个日期相等
     */
    public static boolean isEqual(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isEqual(date2);
    }

    /**
     * 判断两个日期时间是否相等
     *
     * @param dateTime1 日期时间1
     * @param dateTime2 日期时间2
     * @return true: 两个日期时间相等
     */
    public static boolean isEqual(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.isEqual(dateTime2);
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取当前日期
     *
     * @return 当前日期
     */
    public static LocalDate now() {
        return LocalDate.now();
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }

    /**
     * 获取今天的开始时间（00:00:00）
     *
     * @return 今天的开始时间
     */
    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * 获取今天的结束时间（23:59:59）
     *
     * @return 今天的结束时间
     */
    public static LocalDateTime endOfToday() {
        return LocalDate.now().atTime(23, 59, 59);
    }

    /**
     * 获取指定日期的开始时间（00:00:00）
     *
     * @param date 日期
     * @return 指定日期的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间（23:59:59）
     *
     * @param date 日期
     * @return 指定日期的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }

    /**
     * 获取本月第一天
     *
     * @return 本月第一天
     */
    public static LocalDate firstDayOfMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * 获取本月最后一天
     *
     * @return 本月最后一天
     */
    public static LocalDate lastDayOfMonth() {
        LocalDate now = LocalDate.now();
        return now.withDayOfMonth(now.lengthOfMonth());
    }

    /**
     * 获取指定日期所在月的第一天
     *
     * @param date 日期
     * @return 该月第一天
     */
    public static LocalDate firstDayOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(1);
    }

    /**
     * 获取指定日期所在月的最后一天
     *
     * @param date 日期
     * @return 该月最后一天
     */
    public static LocalDate lastDayOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(date.lengthOfMonth());
    }
}
