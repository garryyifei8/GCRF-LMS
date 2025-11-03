package com.gcrf.library.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringUtil类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class StringUtilTest {

    @Test
    void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertFalse(StringUtil.isEmpty(" "));
        assertFalse(StringUtil.isEmpty("test"));
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(StringUtil.isNotEmpty(null));
        assertFalse(StringUtil.isNotEmpty(""));
        assertTrue(StringUtil.isNotEmpty(" "));
        assertTrue(StringUtil.isNotEmpty("test"));
    }

    @Test
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank(" "));
        assertTrue(StringUtil.isBlank("\t\n"));
        assertFalse(StringUtil.isBlank("test"));
    }

    @Test
    void testIsNotBlank() {
        assertFalse(StringUtil.isNotBlank(null));
        assertFalse(StringUtil.isNotBlank(""));
        assertFalse(StringUtil.isNotBlank(" "));
        assertTrue(StringUtil.isNotBlank("test"));
    }

    @Test
    void testMaskPhone() {
        String phone = "13812345678";
        String masked = StringUtil.maskPhone(phone);
        assertEquals("138****5678", masked);

        // 非11位手机号不脱敏
        assertEquals("1234", StringUtil.maskPhone("1234"));
        assertNull(StringUtil.maskPhone(null));
    }

    @Test
    void testMaskIdCard() {
        String idCard18 = "110101199001011234";
        String masked18 = StringUtil.maskIdCard(idCard18);
        assertEquals("110101********1234", masked18);

        String idCard15 = "110101900101123";
        String masked15 = StringUtil.maskIdCard(idCard15);
        assertEquals("110101*****1123", masked15);

        // 非15或18位不脱敏
        assertEquals("12345", StringUtil.maskIdCard("12345"));
    }

    @Test
    void testMaskEmail() {
        String email = "test@example.com";
        String masked = StringUtil.maskEmail(email);
        assertEquals("te**@example.com", masked);

        String shortEmail = "a@example.com";
        String maskedShort = StringUtil.maskEmail(shortEmail);
        assertEquals("*a@example.com", maskedShort);

        assertNull(StringUtil.maskEmail(null));
    }

    @Test
    void testMaskName() {
        String name2 = "张三";
        String masked2 = StringUtil.maskName(name2);
        assertEquals("张*", masked2);

        String name3 = "李明明";
        String masked3 = StringUtil.maskName(name3);
        assertEquals("李**", masked3);

        String name1 = "王";
        assertEquals("王", StringUtil.maskName(name1));
    }

    @Test
    void testMask() {
        String str = "1234567890";
        String masked = StringUtil.mask(str, 3, 3);
        assertEquals("123****890", masked);

        // 字符串太短不脱敏
        String shortStr = "123";
        assertEquals("123", StringUtil.mask(shortStr, 2, 2));
    }

    @Test
    void testRandomString() {
        String random = StringUtil.randomString(10);
        assertNotNull(random);
        assertEquals(10, random.length());
    }

    @Test
    void testRandomNumeric() {
        String numeric = StringUtil.randomNumeric(6);
        assertNotNull(numeric);
        assertEquals(6, numeric.length());
        assertTrue(StringUtil.isNumeric(numeric));
    }

    @Test
    void testRandomAlpha() {
        String alpha = StringUtil.randomAlpha(8);
        assertNotNull(alpha);
        assertEquals(8, alpha.length());
        assertTrue(StringUtil.isAlpha(alpha));
    }

    @Test
    void testToCamelCase() {
        String snakeCase = "user_name";
        String camelCase = StringUtil.toCamelCase(snakeCase);
        assertEquals("userName", camelCase);

        String withMultiple = "user_first_name";
        assertEquals("userFirstName", StringUtil.toCamelCase(withMultiple));
    }

    @Test
    void testToUpperCamelCase() {
        String snakeCase = "user_name";
        String upperCamelCase = StringUtil.toUpperCamelCase(snakeCase);
        assertEquals("UserName", upperCamelCase);
    }

    @Test
    void testToSnakeCase() {
        String camelCase = "userName";
        String snakeCase = StringUtil.toSnakeCase(camelCase);
        assertEquals("user_name", snakeCase);

        String upperCamelCase = "UserName";
        assertEquals("user_name", StringUtil.toSnakeCase(upperCamelCase));
    }

    @Test
    void testToSnakeCaseUpper() {
        String camelCase = "userName";
        String snakeCaseUpper = StringUtil.toSnakeCaseUpper(camelCase);
        assertEquals("USER_NAME", snakeCaseUpper);
    }

    @Test
    void testToKebabCase() {
        String camelCase = "userName";
        String kebabCase = StringUtil.toKebabCase(camelCase);
        assertEquals("user-name", kebabCase);
    }

    @Test
    void testCapitalize() {
        assertEquals("Hello", StringUtil.capitalize("hello"));
        assertEquals("Hello", StringUtil.capitalize("Hello"));
        assertEquals("H", StringUtil.capitalize("h"));
        assertNull(StringUtil.capitalize(null));
    }

    @Test
    void testUncapitalize() {
        assertEquals("hello", StringUtil.uncapitalize("Hello"));
        assertEquals("hello", StringUtil.uncapitalize("hello"));
        assertEquals("h", StringUtil.uncapitalize("H"));
        assertNull(StringUtil.uncapitalize(null));
    }

    @Test
    void testTruncate() {
        String text = "Hello World";
        String truncated = StringUtil.truncate(text, 5);
        assertEquals("Hello...", truncated);

        String shortText = "Hi";
        assertEquals("Hi", StringUtil.truncate(shortText, 5));
    }

    @Test
    void testAbbreviate() {
        String text = "Hello World";
        String abbreviated = StringUtil.abbreviate(text, 8);
        assertEquals("Hello...", abbreviated);
    }

    @Test
    void testEquals() {
        assertTrue(StringUtil.equals("test", "test"));
        assertFalse(StringUtil.equals("test", "TEST"));
        assertTrue(StringUtil.equals(null, null));
        assertFalse(StringUtil.equals("test", null));
    }

    @Test
    void testEqualsIgnoreCase() {
        assertTrue(StringUtil.equalsIgnoreCase("test", "TEST"));
        assertTrue(StringUtil.equalsIgnoreCase("Test", "test"));
        assertTrue(StringUtil.equalsIgnoreCase(null, null));
        assertFalse(StringUtil.equalsIgnoreCase("test", null));
    }

    @Test
    void testContains() {
        assertTrue(StringUtil.contains("Hello World", "World"));
        assertFalse(StringUtil.contains("Hello World", "world"));
        assertFalse(StringUtil.contains(null, "test"));
    }

    @Test
    void testContainsIgnoreCase() {
        assertTrue(StringUtil.containsIgnoreCase("Hello World", "world"));
        assertTrue(StringUtil.containsIgnoreCase("Hello World", "WORLD"));
        assertFalse(StringUtil.containsIgnoreCase(null, "test"));
    }

    @Test
    void testTrim() {
        assertEquals("test", StringUtil.trim("  test  "));
        assertEquals("", StringUtil.trim("   "));
        assertNull(StringUtil.trim(null));
    }

    @Test
    void testTrimToNull() {
        assertEquals("test", StringUtil.trimToNull("  test  "));
        assertNull(StringUtil.trimToNull("   "));
        assertNull(StringUtil.trimToNull(null));
    }

    @Test
    void testTrimToEmpty() {
        assertEquals("test", StringUtil.trimToEmpty("  test  "));
        assertEquals("", StringUtil.trimToEmpty("   "));
        assertEquals("", StringUtil.trimToEmpty(null));
    }

    @Test
    void testReplace() {
        String result = StringUtil.replace("Hello World", "World", "Java");
        assertEquals("Hello Java", result);
    }

    @Test
    void testIsValidPhone() {
        assertTrue(StringUtil.isValidPhone("13812345678"));
        assertTrue(StringUtil.isValidPhone("15912345678"));
        assertFalse(StringUtil.isValidPhone("12345678901"));
        assertFalse(StringUtil.isValidPhone("1381234567"));
        assertFalse(StringUtil.isValidPhone(null));
    }

    @Test
    void testIsValidEmail() {
        assertTrue(StringUtil.isValidEmail("test@example.com"));
        assertTrue(StringUtil.isValidEmail("username@example.com"));
        assertFalse(StringUtil.isValidEmail("invalid-email"));
        assertFalse(StringUtil.isValidEmail("@example.com"));
        assertFalse(StringUtil.isValidEmail(null));
    }

    @Test
    void testIsValidIdCard() {
        assertTrue(StringUtil.isValidIdCard("110101199001011234"));
        assertTrue(StringUtil.isValidIdCard("110101900101123"));
        assertFalse(StringUtil.isValidIdCard("12345"));
        assertFalse(StringUtil.isValidIdCard(null));
    }

    @Test
    void testIsNumeric() {
        assertTrue(StringUtil.isNumeric("12345"));
        assertFalse(StringUtil.isNumeric("123abc"));
        assertFalse(StringUtil.isNumeric(null));
    }

    @Test
    void testIsAlpha() {
        assertTrue(StringUtil.isAlpha("abcdef"));
        assertTrue(StringUtil.isAlpha("ABCDEF"));
        assertFalse(StringUtil.isAlpha("abc123"));
        assertFalse(StringUtil.isAlpha(null));
    }

    @Test
    void testIsAlphanumeric() {
        assertTrue(StringUtil.isAlphanumeric("abc123"));
        assertTrue(StringUtil.isAlphanumeric("123"));
        assertTrue(StringUtil.isAlphanumeric("abc"));
        assertFalse(StringUtil.isAlphanumeric("abc-123"));
        assertFalse(StringUtil.isAlphanumeric(null));
    }

    @Test
    void testDefaultIfBlank() {
        assertEquals("test", StringUtil.defaultIfBlank("test", "default"));
        assertEquals("default", StringUtil.defaultIfBlank("", "default"));
        assertEquals("default", StringUtil.defaultIfBlank(" ", "default"));
        assertEquals("default", StringUtil.defaultIfBlank(null, "default"));
    }

    @Test
    void testDefaultIfEmpty() {
        assertEquals("test", StringUtil.defaultIfEmpty("test", "default"));
        assertEquals("default", StringUtil.defaultIfEmpty("", "default"));
        assertEquals(" ", StringUtil.defaultIfEmpty(" ", "default"));
        assertEquals("default", StringUtil.defaultIfEmpty(null, "default"));
    }

    @Test
    void testRepeat() {
        assertEquals("aaa", StringUtil.repeat("a", 3));
        assertEquals("", StringUtil.repeat("a", 0));
        assertNull(StringUtil.repeat(null, 3));
    }

    @Test
    void testReverse() {
        assertEquals("cba", StringUtil.reverse("abc"));
        assertNull(StringUtil.reverse(null));
    }

    @Test
    void testJoin() {
        String[] array = {"a", "b", "c"};
        assertEquals("a,b,c", StringUtil.join(array, ","));
        assertEquals("a-b-c", StringUtil.join(array, "-"));
    }

    @Test
    void testSplit() {
        String str = "a,b,c";
        String[] result = StringUtil.split(str, ",");
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }
}
