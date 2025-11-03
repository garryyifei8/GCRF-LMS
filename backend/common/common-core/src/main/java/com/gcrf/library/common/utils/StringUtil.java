package com.gcrf.library.common.utils;

import cn.hutool.core.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 提供常用的字符串操作，基于Apache Commons Lang3和Hutool。
 * 线程安全，所有方法都是静态方法。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 字符串判空
 * boolean empty = StringUtil.isEmpty(str);
 * boolean notEmpty = StringUtil.isNotEmpty(str);
 * boolean blank = StringUtil.isBlank(str);
 * boolean notBlank = StringUtil.isNotBlank(str);
 *
 * // 脱敏处理
 * String maskedPhone = StringUtil.maskPhone("13812345678"); // 输出: 138****5678
 * String maskedIdCard = StringUtil.maskIdCard("110101199001011234"); // 输出: 110101********1234
 * String maskedEmail = StringUtil.maskEmail("test@example.com"); // 输出: te**@example.com
 *
 * // 生成随机字符串
 * String randomStr = StringUtil.randomString(32);
 * String randomNumStr = StringUtil.randomNumeric(6);
 * String randomAlphaStr = StringUtil.randomAlpha(10);
 *
 * // 驼峰与下划线转换
 * String camelCase = StringUtil.toCamelCase("user_name"); // 输出: userName
 * String snakeCase = StringUtil.toSnakeCase("userName"); // 输出: user_name
 * String upperCamelCase = StringUtil.toUpperCamelCase("user_name"); // 输出: UserName
 *
 * // 首字母大小写
 * String capitalized = StringUtil.capitalize("hello"); // 输出: Hello
 * String uncapitalized = StringUtil.uncapitalize("Hello"); // 输出: hello
 *
 * // 字符串截取
 * String truncated = StringUtil.truncate("Hello World", 5); // 输出: Hello...
 * String abbreviated = StringUtil.abbreviate("Hello World", 8); // 输出: Hello...
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
public class StringUtil {

    /**
     * 默认的脱敏字符
     */
    private static final char MASK_CHAR = '*';

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    /**
     * 身份证号正则表达式（支持15位和18位）
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^(\\d{15}|\\d{17}[0-9Xx])$");

    /**
     * 驼峰转下划线的正则表达式
     */
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");

    /**
     * 私有构造函数，防止实例化
     */
    private StringUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 字符串判空方法 ====================

    /**
     * 判断字符串是否为空（null或空字符串）
     * <p>注意：空白字符串（如"  "）会被认为非空</p>
     *
     * @param str 待判断的字符串
     * @return true: 字符串为null或空字符串
     */
    public static boolean isEmpty(CharSequence str) {
        return StringUtils.isEmpty(str);
    }

    /**
     * 判断字符串是否不为空（非null且非空字符串）
     *
     * @param str 待判断的字符串
     * @return true: 字符串非null且非空字符串
     */
    public static boolean isNotEmpty(CharSequence str) {
        return StringUtils.isNotEmpty(str);
    }

    /**
     * 判断字符串是否为空白（null、空字符串或只包含空白字符）
     * <p>空白字符包括：空格、制表符、换行符等</p>
     *
     * @param str 待判断的字符串
     * @return true: 字符串为null、空字符串或只包含空白字符
     */
    public static boolean isBlank(CharSequence str) {
        return StringUtils.isBlank(str);
    }

    /**
     * 判断字符串是否不为空白
     *
     * @param str 待判断的字符串
     * @return true: 字符串非null、非空字符串且不只包含空白字符
     */
    public static boolean isNotBlank(CharSequence str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * 判断所有字符串是否都为空
     *
     * @param strs 待判断的字符串数组
     * @return true: 所有字符串都为空
     */
    public static boolean isAllEmpty(CharSequence... strs) {
        return StringUtils.isAllEmpty(strs);
    }

    /**
     * 判断所有字符串是否都不为空
     *
     * @param strs 待判断的字符串数组
     * @return true: 所有字符串都不为空
     */
    public static boolean isNoneEmpty(CharSequence... strs) {
        return StringUtils.isNoneEmpty(strs);
    }

    /**
     * 判断所有字符串是否都为空白
     *
     * @param strs 待判断的字符串数组
     * @return true: 所有字符串都为空白
     */
    public static boolean isAllBlank(CharSequence... strs) {
        return StringUtils.isAllBlank(strs);
    }

    /**
     * 判断所有字符串是否都不为空白
     *
     * @param strs 待判断的字符串数组
     * @return true: 所有字符串都不为空白
     */
    public static boolean isNoneBlank(CharSequence... strs) {
        return StringUtils.isNoneBlank(strs);
    }

    // ==================== 脱敏方法 ====================

    /**
     * 手机号脱敏（保留前3位和后4位）
     * <p>例如：13812345678 -> 138****5678</p>
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (isBlank(phone)) {
            return phone;
        }
        // 简单验证手机号格式
        if (phone.length() != 11) {
            return phone;
        }
        return mask(phone, 3, 4);
    }

    /**
     * 身份证号脱敏（保留前6位和后4位）
     * <p>例如：110101199001011234 -> 110101********1234</p>
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (isBlank(idCard)) {
            return idCard;
        }
        // 支持15位和18位身份证
        if (idCard.length() != 15 && idCard.length() != 18) {
            return idCard;
        }
        return mask(idCard, 6, 4);
    }

    /**
     * 邮箱脱敏（用户名保留前2位，@符号后完整显示）
     * <p>例如：test@example.com -> te**@example.com</p>
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (isBlank(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return MASK_CHAR + username + domain;
        }
        String maskedUsername = username.substring(0, 2) + StringUtils.repeat(MASK_CHAR, username.length() - 2);
        return maskedUsername + domain;
    }

    /**
     * 银行卡号脱敏（保留前4位和后4位）
     * <p>例如：6222600000001234567 -> 6222 **** **** 4567</p>
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (isBlank(bankCard)) {
            return bankCard;
        }
        if (bankCard.length() < 8) {
            return bankCard;
        }
        String prefix = bankCard.substring(0, 4);
        String suffix = bankCard.substring(bankCard.length() - 4);
        int middleLength = bankCard.length() - 8;
        return prefix + " " + StringUtils.repeat(MASK_CHAR, Math.min(middleLength, 4)) + " " +
               StringUtils.repeat(MASK_CHAR, Math.min(middleLength - 4, 4)) + " " + suffix;
    }

    /**
     * 姓名脱敏（保留姓氏，名字用*代替）
     * <p>例如：张三 -> 张*，李明明 -> 李**</p>
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (isBlank(name)) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + StringUtils.repeat(MASK_CHAR, name.length() - 1);
    }

    /**
     * 通用脱敏方法（保留前N位和后M位）
     *
     * @param str        待脱敏的字符串
     * @param prefixLen  保留前N位
     * @param suffixLen  保留后M位
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixLen, int suffixLen) {
        if (isBlank(str)) {
            return str;
        }
        int length = str.length();
        if (length <= prefixLen + suffixLen) {
            return str;
        }
        String prefix = str.substring(0, prefixLen);
        String suffix = str.substring(length - suffixLen);
        String middle = StringUtils.repeat(MASK_CHAR, length - prefixLen - suffixLen);
        return prefix + middle + suffix;
    }

    // ==================== 随机字符串生成方法 ====================

    /**
     * 生成随机字符串（包含字母和数字）
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return RandomUtil.randomString(length);
    }

    /**
     * 生成随机数字字符串
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        return RandomUtil.randomNumbers(length);
    }

    /**
     * 生成随机字母字符串
     *
     * @param length 字符串长度
     * @return 随机字母字符串
     */
    public static String randomAlpha(int length) {
        return RandomUtil.randomString(RandomUtil.BASE_CHAR, length);
    }

    /**
     * 生成随机字母字符串（大写）
     *
     * @param length 字符串长度
     * @return 随机大写字母字符串
     */
    public static String randomAlphaUpper(int length) {
        return randomAlpha(length).toUpperCase();
    }

    /**
     * 生成随机字母字符串（小写）
     *
     * @param length 字符串长度
     * @return 随机小写字母字符串
     */
    public static String randomAlphaLower(int length) {
        return randomAlpha(length).toLowerCase();
    }

    // ==================== 驼峰与下划线转换方法 ====================

    /**
     * 下划线转驼峰（首字母小写）
     * <p>例如：user_name -> userName</p>
     *
     * @param str 下划线字符串
     * @return 驼峰字符串
     */
    public static String toCamelCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        return cn.hutool.core.util.StrUtil.toCamelCase(str);
    }

    /**
     * 下划线转驼峰（首字母大写）
     * <p>例如：user_name -> UserName</p>
     *
     * @param str 下划线字符串
     * @return 驼峰字符串（首字母大写）
     */
    public static String toUpperCamelCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        String camelCase = toCamelCase(str);
        return capitalize(camelCase);
    }

    /**
     * 驼峰转下划线（小写）
     * <p>例如：userName -> user_name</p>
     *
     * @param str 驼峰字符串
     * @return 下划线字符串
     */
    public static String toSnakeCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        return cn.hutool.core.util.StrUtil.toUnderlineCase(str);
    }

    /**
     * 驼峰转下划线（大写）
     * <p>例如：userName -> USER_NAME</p>
     *
     * @param str 驼峰字符串
     * @return 下划线字符串（大写）
     */
    public static String toSnakeCaseUpper(String str) {
        if (isBlank(str)) {
            return str;
        }
        return toSnakeCase(str).toUpperCase();
    }

    /**
     * 转换为kebab-case（短横线连接）
     * <p>例如：userName -> user-name</p>
     *
     * @param str 字符串
     * @return kebab-case字符串
     */
    public static String toKebabCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        return toSnakeCase(str).replace('_', '-');
    }

    // ==================== 首字母大小写方法 ====================

    /**
     * 首字母大写
     * <p>例如：hello -> Hello</p>
     *
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        return StringUtils.capitalize(str);
    }

    /**
     * 首字母小写
     * <p>例如：Hello -> hello</p>
     *
     * @param str 字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        return StringUtils.uncapitalize(str);
    }

    // ==================== 字符串截取方法 ====================

    /**
     * 截取字符串，超出部分用...代替
     * <p>例如：truncate("Hello World", 5) -> Hello...</p>
     *
     * @param str       字符串
     * @param maxLength 最大长度（不包括省略号）
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (isBlank(str)) {
            return str;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 缩略字符串，超出部分用...代替
     * <p>与truncate类似，但maxLength包括省略号的长度</p>
     * <p>例如：abbreviate("Hello World", 8) -> Hello...</p>
     *
     * @param str       字符串
     * @param maxLength 最大长度（包括省略号）
     * @return 缩略后的字符串
     */
    public static String abbreviate(String str, int maxLength) {
        return StringUtils.abbreviate(str, maxLength);
    }

    // ==================== 字符串比较方法 ====================

    /**
     * 比较两个字符串是否相等（null安全）
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true: 相等
     */
    public static boolean equals(CharSequence str1, CharSequence str2) {
        return StringUtils.equals(str1, str2);
    }

    /**
     * 比较两个字符串是否相等（忽略大小写，null安全）
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true: 相等
     */
    public static boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
        return StringUtils.equalsIgnoreCase(str1, str2);
    }

    // ==================== 字符串包含方法 ====================

    /**
     * 判断字符串是否包含指定字符序列
     *
     * @param str        字符串
     * @param searchStr  搜索字符序列
     * @return true: 包含
     */
    public static boolean contains(CharSequence str, CharSequence searchStr) {
        return StringUtils.contains(str, searchStr);
    }

    /**
     * 判断字符串是否包含指定字符序列（忽略大小写）
     *
     * @param str        字符串
     * @param searchStr  搜索字符序列
     * @return true: 包含
     */
    public static boolean containsIgnoreCase(CharSequence str, CharSequence searchStr) {
        return StringUtils.containsIgnoreCase(str, searchStr);
    }

    /**
     * 判断字符串是否包含任意一个指定字符序列
     *
     * @param str         字符串
     * @param searchStrs  搜索字符序列数组
     * @return true: 包含任意一个
     */
    public static boolean containsAny(CharSequence str, CharSequence... searchStrs) {
        return StringUtils.containsAny(str, searchStrs);
    }

    // ==================== 字符串去除空白方法 ====================

    /**
     * 去除字符串两端的空白字符
     *
     * @param str 字符串
     * @return 去除空白后的字符串
     */
    public static String trim(String str) {
        return StringUtils.trim(str);
    }

    /**
     * 去除字符串两端的空白字符，如果结果为空字符串则返回null
     *
     * @param str 字符串
     * @return 去除空白后的字符串或null
     */
    public static String trimToNull(String str) {
        return StringUtils.trimToNull(str);
    }

    /**
     * 去除字符串两端的空白字符，如果为null则返回空字符串
     *
     * @param str 字符串
     * @return 去除空白后的字符串或空字符串
     */
    public static String trimToEmpty(String str) {
        return StringUtils.trimToEmpty(str);
    }

    // ==================== 字符串替换方法 ====================

    /**
     * 替换字符串中的指定内容
     *
     * @param text         原字符串
     * @param searchString 搜索字符串
     * @param replacement  替换字符串
     * @return 替换后的字符串
     */
    public static String replace(String text, String searchString, String replacement) {
        return StringUtils.replace(text, searchString, replacement);
    }

    /**
     * 替换字符串中的指定内容（忽略大小写）
     *
     * @param text         原字符串
     * @param searchString 搜索字符串
     * @param replacement  替换字符串
     * @return 替换后的字符串
     */
    public static String replaceIgnoreCase(String text, String searchString, String replacement) {
        return StringUtils.replaceIgnoreCase(text, searchString, replacement);
    }

    // ==================== 字符串验证方法 ====================

    /**
     * 判断字符串是否为有效的手机号
     *
     * @param phone 手机号
     * @return true: 有效的手机号
     */
    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 判断字符串是否为有效的邮箱
     *
     * @param email 邮箱
     * @return true: 有效的邮箱
     */
    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 判断字符串是否为有效的身份证号
     *
     * @param idCard 身份证号
     * @return true: 有效的身份证号
     */
    public static boolean isValidIdCard(String idCard) {
        return isNotBlank(idCard) && ID_CARD_PATTERN.matcher(idCard).matches();
    }

    /**
     * 判断字符串是否为纯数字
     *
     * @param str 字符串
     * @return true: 纯数字
     */
    public static boolean isNumeric(CharSequence str) {
        return StringUtils.isNumeric(str);
    }

    /**
     * 判断字符串是否为纯字母
     *
     * @param str 字符串
     * @return true: 纯字母
     */
    public static boolean isAlpha(CharSequence str) {
        return StringUtils.isAlpha(str);
    }

    /**
     * 判断字符串是否为字母和数字的组合
     *
     * @param str 字符串
     * @return true: 字母和数字的组合
     */
    public static boolean isAlphanumeric(CharSequence str) {
        return StringUtils.isAlphanumeric(str);
    }

    // ==================== 其他实用方法 ====================

    /**
     * 获取字符串的默认值（如果为null或空白，返回默认值）
     *
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return StringUtils.defaultIfBlank(str, defaultValue);
    }

    /**
     * 获取字符串的默认值（如果为null或空，返回默认值）
     *
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return StringUtils.defaultIfEmpty(str, defaultValue);
    }

    /**
     * 重复字符串N次
     *
     * @param str    字符串
     * @param repeat 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int repeat) {
        return StringUtils.repeat(str, repeat);
    }

    /**
     * 反转字符串
     *
     * @param str 字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        return StringUtils.reverse(str);
    }

    /**
     * 连接字符串数组
     *
     * @param elements  字符串数组
     * @param separator 分隔符
     * @return 连接后的字符串
     */
    public static String join(Object[] elements, String separator) {
        return StringUtils.join(elements, separator);
    }

    /**
     * 连接字符串集合
     *
     * @param elements  字符串集合
     * @param separator 分隔符
     * @return 连接后的字符串
     */
    public static String join(Iterable<?> elements, String separator) {
        return StringUtils.join(elements, separator);
    }

    /**
     * 将字符串按分隔符分割为数组
     *
     * @param str       字符串
     * @param separator 分隔符
     * @return 字符串数组
     */
    public static String[] split(String str, String separator) {
        return StringUtils.split(str, separator);
    }
}
