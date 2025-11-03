package com.gcrf.library.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonUtil类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class JsonUtilTest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private Integer age;
    }

    @Test
    void testToJson() {
        TestUser user = new TestUser(1L, "张三", 25);
        String json = JsonUtil.toJson(user);

        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    void testToJsonWithNull() {
        String json = JsonUtil.toJson(null);
        assertNull(json);
    }

    @Test
    void testToPrettyJson() {
        TestUser user = new TestUser(1L, "张三", 25);
        String prettyJson = JsonUtil.toPrettyJson(user);

        assertNotNull(prettyJson);
        assertTrue(prettyJson.contains("\n"));
        assertTrue(prettyJson.contains("\"name\" : \"张三\""));
    }

    @Test
    void testToJsonBytes() {
        TestUser user = new TestUser(1L, "张三", 25);
        byte[] jsonBytes = JsonUtil.toJsonBytes(user);

        assertNotNull(jsonBytes);
        assertTrue(jsonBytes.length > 0);
    }

    @Test
    void testParseObject() {
        String json = "{\"id\":1,\"name\":\"张三\",\"age\":25}";
        TestUser user = JsonUtil.parseObject(json, TestUser.class);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("张三", user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    void testParseObjectWithInvalidJson() {
        String invalidJson = "not a json";
        TestUser user = JsonUtil.parseObject(invalidJson, TestUser.class);
        assertNull(user);
    }

    @Test
    void testParseObjectWithTypeReference() {
        String json = "[{\"id\":1,\"name\":\"张三\"},{\"id\":2,\"name\":\"李四\"}]";
        TypeReference<List<TestUser>> typeRef = new TypeReference<List<TestUser>>() {};
        List<TestUser> users = JsonUtil.parseObject(json, typeRef);

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("张三", users.get(0).getName());
        assertEquals("李四", users.get(1).getName());
    }

    @Test
    void testParseObjectFromBytes() {
        String json = "{\"id\":1,\"name\":\"张三\",\"age\":25}";
        byte[] jsonBytes = json.getBytes();
        TestUser user = JsonUtil.parseObject(jsonBytes, TestUser.class);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("张三", user.getName());
    }

    @Test
    void testParseList() {
        String json = "[{\"id\":1,\"name\":\"张三\"},{\"id\":2,\"name\":\"李四\"}]";
        List<TestUser> users = JsonUtil.parseList(json, TestUser.class);

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("张三", users.get(0).getName());
        assertEquals("李四", users.get(1).getName());
    }

    @Test
    void testParseListFromBytes() {
        String json = "[{\"id\":1,\"name\":\"张三\"}]";
        byte[] jsonBytes = json.getBytes();
        List<TestUser> users = JsonUtil.parseList(jsonBytes, TestUser.class);

        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("张三", users.get(0).getName());
    }

    @Test
    void testParseMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<String, Object> map = JsonUtil.parseMap(json);

        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    void testParseMapWithValueClass() {
        String json = "{\"user1\":{\"id\":1,\"name\":\"张三\"},\"user2\":{\"id\":2,\"name\":\"李四\"}}";
        Map<String, TestUser> map = JsonUtil.parseMap(json, TestUser.class);

        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("张三", map.get("user1").getName());
        assertEquals("李四", map.get("user2").getName());
    }

    @Test
    void testParseTree() {
        String json = "{\"name\":\"张三\",\"age\":25}";
        JsonNode node = JsonUtil.parseTree(json);

        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("张三", node.get("name").asText());
        assertEquals(25, node.get("age").asInt());
    }

    @Test
    void testParseTreeFromBytes() {
        String json = "{\"name\":\"张三\"}";
        byte[] jsonBytes = json.getBytes();
        JsonNode node = JsonUtil.parseTree(jsonBytes);

        assertNotNull(node);
        assertEquals("张三", node.get("name").asText());
    }

    @Test
    void testConvertValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1);
        map.put("name", "张三");
        map.put("age", 25);

        TestUser user = JsonUtil.convertValue(map, TestUser.class);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("张三", user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    void testConvertValueWithTypeReference() {
        List<Map<String, Object>> list = Arrays.asList(
            createUserMap(1L, "张三"),
            createUserMap(2L, "李四")
        );

        TypeReference<List<TestUser>> typeRef = new TypeReference<List<TestUser>>() {};
        List<TestUser> users = JsonUtil.convertValue(list, typeRef);

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("张三", users.get(0).getName());
    }

    @Test
    void testIsValidJson() {
        assertTrue(JsonUtil.isValidJson("{\"name\":\"test\"}"));
        assertTrue(JsonUtil.isValidJson("[1,2,3]"));
        assertFalse(JsonUtil.isValidJson("not a json"));
        assertFalse(JsonUtil.isValidJson(null));
        assertFalse(JsonUtil.isValidJson(""));
    }

    @Test
    void testIsValidJsonObject() {
        assertTrue(JsonUtil.isValidJsonObject("{\"name\":\"test\"}"));
        assertFalse(JsonUtil.isValidJsonObject("[1,2,3]"));
        assertFalse(JsonUtil.isValidJsonObject("not a json"));
    }

    @Test
    void testIsValidJsonArray() {
        assertTrue(JsonUtil.isValidJsonArray("[1,2,3]"));
        assertFalse(JsonUtil.isValidJsonArray("{\"name\":\"test\"}"));
        assertFalse(JsonUtil.isValidJsonArray("not a json"));
    }

    @Test
    void testGetObjectMapper() {
        assertNotNull(JsonUtil.getObjectMapper());
    }

    @Test
    void testGetPrettyMapper() {
        assertNotNull(JsonUtil.getPrettyMapper());
    }

    @Test
    void testMerge() {
        String target = "{\"name\":\"张三\",\"age\":25}";
        String source = "{\"age\":26,\"city\":\"北京\"}";
        String merged = JsonUtil.merge(target, source);

        assertNotNull(merged);
        JsonNode node = JsonUtil.parseTree(merged);
        assertEquals("张三", node.get("name").asText());
        assertEquals(26, node.get("age").asInt());
        assertEquals("北京", node.get("city").asText());
    }

    @Test
    void testMergeWithNullTarget() {
        String source = "{\"name\":\"张三\"}";
        String merged = JsonUtil.merge(null, source);
        assertEquals(source, merged);
    }

    @Test
    void testMergeWithNullSource() {
        String target = "{\"name\":\"张三\"}";
        String merged = JsonUtil.merge(target, null);
        assertEquals(target, merged);
    }

    @Test
    void testCompress() {
        String prettyJson = "{\n  \"name\" : \"张三\",\n  \"age\" : 25\n}";
        String compressed = JsonUtil.compress(prettyJson);

        assertNotNull(compressed);
        assertFalse(compressed.contains("\n"));
        assertTrue(compressed.contains("\"name\":\"张三\""));
    }

    @Test
    void testRoundTrip() {
        TestUser original = new TestUser(1L, "张三", 25);
        String json = JsonUtil.toJson(original);
        TestUser parsed = JsonUtil.parseObject(json, TestUser.class);

        assertNotNull(parsed);
        assertEquals(original.getId(), parsed.getId());
        assertEquals(original.getName(), parsed.getName());
        assertEquals(original.getAge(), parsed.getAge());
    }

    @Test
    void testNullSafety() {
        assertNull(JsonUtil.toJson(null));
        assertNull(JsonUtil.toPrettyJson(null));
        assertNull(JsonUtil.toJsonBytes(null));
        assertNull(JsonUtil.parseObject((String) null, TestUser.class));
        assertNull(JsonUtil.parseList((String) null, TestUser.class));
        assertNull(JsonUtil.parseMap(null));
        assertNull(JsonUtil.parseTree((String) null));
        assertNull(JsonUtil.convertValue(null, TestUser.class));
    }

    private Map<String, Object> createUserMap(Long id, String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        return map;
    }
}
