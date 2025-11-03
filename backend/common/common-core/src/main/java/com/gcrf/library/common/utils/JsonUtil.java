package com.gcrf.library.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * JSON工具类
 * <p>
 * 基于Jackson提供JSON序列化和反序列化功能。
 * 线程安全，ObjectMapper配置为可重用的单例。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 对象转JSON字符串
 * String json = JsonUtil.toJson(user);
 *
 * // JSON字符串转对象
 * User user = JsonUtil.parseObject(json, User.class);
 *
 * // JSON字符串转List
 * List&lt;User&gt; users = JsonUtil.parseList(json, User.class);
 *
 * // JSON字符串转Map
 * Map&lt;String, Object&gt; map = JsonUtil.parseMap(json);
 *
 * // 复杂类型转换（如List&lt;Map&lt;String, User&gt;&gt;）
 * TypeReference&lt;List&lt;Map&lt;String, User&gt;&gt;&gt; typeRef = new TypeReference&lt;&gt;() {};
 * List&lt;Map&lt;String, User&gt;&gt; result = JsonUtil.parseObject(json, typeRef);
 *
 * // 美化输出JSON
 * String prettyJson = JsonUtil.toPrettyJson(user);
 *
 * // 获取JsonNode进行复杂操作
 * JsonNode node = JsonUtil.parseTree(json);
 * String name = node.get("name").asText();
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Slf4j
public class JsonUtil {

    /**
     * ObjectMapper实例（线程安全，可复用）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 用于美化输出的ObjectMapper实例
     */
    private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();

    /**
     * 默认日期时间格式
     */
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认时区
     */
    private static final String DEFAULT_TIME_ZONE = "GMT+8";

    // 静态初始化块，配置ObjectMapper
    static {
        // ========== 通用配置 ==========
        configureMapper(OBJECT_MAPPER);
        configureMapper(PRETTY_MAPPER);

        // ========== 美化输出配置 ==========
        PRETTY_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 配置ObjectMapper
     *
     * @param mapper ObjectMapper实例
     */
    private static void configureMapper(ObjectMapper mapper) {
        // 设置日期格式
        mapper.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
        mapper.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));

        // 注册JavaTimeModule以支持Java 8日期时间API
        mapper.registerModule(new JavaTimeModule());

        // 序列化配置
        // 禁用将日期序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 禁用将BigDecimal序列化为科学计数法
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        // 空对象不抛异常
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 反序列化配置
        // 忽略JSON中存在但Java对象不存在的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许字段名不带引号
        mapper.disable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        // 允许单引号
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 序列化时忽略null值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 对象转JSON字符串 ====================

    /**
     * 将对象转换为JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON: {}", obj, e);
            return null;
        }
    }

    /**
     * 将对象转换为格式化的JSON字符串（美化输出）
     *
     * @param obj 对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return PRETTY_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to pretty JSON: {}", obj, e);
            return null;
        }
    }

    /**
     * 将对象转换为JSON字节数组
     *
     * @param obj 对象
     * @return JSON字节数组
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON bytes: {}", obj, e);
            return null;
        }
    }

    // ==================== JSON字符串转对象 ====================

    /**
     * 将JSON字符串转换为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 对象实例
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StringUtil.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to object: json={}, class={}", json, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为对象（支持复杂类型）
     * <p>使用TypeReference处理泛型类型</p>
     * <p>示例：TypeReference&lt;List&lt;User&gt;&gt; typeRef = new TypeReference&lt;&gt;() {};</p>
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 对象实例
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (StringUtil.isBlank(json) || typeReference == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to object: json={}, typeReference={}", json, typeReference.getType(), e);
            return null;
        }
    }

    /**
     * 将JSON字节数组转换为对象
     *
     * @param jsonBytes JSON字节数组
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 对象实例
     */
    public static <T> T parseObject(byte[] jsonBytes, Class<T> clazz) {
        if (jsonBytes == null || jsonBytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonBytes, clazz);
        } catch (IOException e) {
            log.error("Failed to parse JSON bytes to object: class={}", clazz.getName(), e);
            return null;
        }
    }

    // ==================== JSON字符串转List ====================

    /**
     * 将JSON字符串转换为List
     *
     * @param json  JSON字符串
     * @param clazz 列表元素类型
     * @param <T>   泛型类型
     * @return List对象
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (StringUtil.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to list: json={}, class={}", json, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 将JSON字节数组转换为List
     *
     * @param jsonBytes JSON字节数组
     * @param clazz     列表元素类型
     * @param <T>       泛型类型
     * @return List对象
     */
    public static <T> List<T> parseList(byte[] jsonBytes, Class<T> clazz) {
        if (jsonBytes == null || jsonBytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(jsonBytes, javaType);
        } catch (IOException e) {
            log.error("Failed to parse JSON bytes to list: class={}", clazz.getName(), e);
            return null;
        }
    }

    // ==================== JSON字符串转Map ====================

    /**
     * 将JSON字符串转换为Map
     * <p>Map的值类型为Object</p>
     *
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> parseMap(String json) {
        return parseObject(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 将JSON字符串转换为Map（指定值类型）
     *
     * @param json       JSON字符串
     * @param valueClass Map值的类型
     * @param <V>        值的泛型类型
     * @return Map对象
     */
    public static <V> Map<String, V> parseMap(String json, Class<V> valueClass) {
        if (StringUtil.isBlank(json) || valueClass == null) {
            return null;
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, valueClass);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to map: json={}, valueClass={}", json, valueClass.getName(), e);
            return null;
        }
    }

    // ==================== JSON字符串转JsonNode ====================

    /**
     * 将JSON字符串解析为JsonNode树结构
     * <p>JsonNode可以用于复杂的JSON操作</p>
     *
     * @param json JSON字符串
     * @return JsonNode对象
     */
    public static JsonNode parseTree(String json) {
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to tree: json={}", json, e);
            return null;
        }
    }

    /**
     * 将JSON字节数组解析为JsonNode树结构
     *
     * @param jsonBytes JSON字节数组
     * @return JsonNode对象
     */
    public static JsonNode parseTree(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(jsonBytes);
        } catch (IOException e) {
            log.error("Failed to parse JSON bytes to tree", e);
            return null;
        }
    }

    // ==================== 对象转换方法 ====================

    /**
     * 对象转换（通过JSON序列化和反序列化实现）
     * <p>适用于同结构不同类型的对象转换</p>
     *
     * @param fromValue 源对象
     * @param toClass   目标类型
     * @param <T>       泛型类型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, Class<T> toClass) {
        if (fromValue == null || toClass == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toClass);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert value: fromValue={}, toClass={}", fromValue, toClass.getName(), e);
            return null;
        }
    }

    /**
     * 对象转换（通过JSON序列化和反序列化实现，支持复杂类型）
     *
     * @param fromValue     源对象
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> typeReference) {
        if (fromValue == null || typeReference == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, typeReference);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert value: fromValue={}, typeReference={}", fromValue, typeReference.getType(), e);
            return null;
        }
    }

    // ==================== JSON验证方法 ====================

    /**
     * 判断字符串是否为有效的JSON格式
     *
     * @param json JSON字符串
     * @return true: 有效的JSON格式
     */
    public static boolean isValidJson(String json) {
        if (StringUtil.isBlank(json)) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为有效的JSON对象格式
     *
     * @param json JSON字符串
     * @return true: 有效的JSON对象格式
     */
    public static boolean isValidJsonObject(String json) {
        if (StringUtil.isBlank(json)) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为有效的JSON数组格式
     *
     * @param json JSON字符串
     * @return true: 有效的JSON数组格式
     */
    public static boolean isValidJsonArray(String json) {
        if (StringUtil.isBlank(json)) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    // ==================== ObjectMapper获取方法 ====================

    /**
     * 获取ObjectMapper实例
     * <p>如果需要自定义配置，可以使用此方法获取ObjectMapper</p>
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 获取用于美化输出的ObjectMapper实例
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getPrettyMapper() {
        return PRETTY_MAPPER;
    }

    // ==================== JSON合并方法 ====================

    /**
     * 合并两个JSON字符串
     * <p>将source的字段合并到target中，相同字段以source为准</p>
     *
     * @param target 目标JSON字符串
     * @param source 源JSON字符串
     * @return 合并后的JSON字符串
     */
    public static String merge(String target, String source) {
        if (StringUtil.isBlank(target)) {
            return source;
        }
        if (StringUtil.isBlank(source)) {
            return target;
        }
        try {
            JsonNode targetNode = OBJECT_MAPPER.readTree(target);
            JsonNode sourceNode = OBJECT_MAPPER.readTree(source);
            JsonNode merged = merge(targetNode, sourceNode);
            return OBJECT_MAPPER.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            log.error("Failed to merge JSON: target={}, source={}", target, source, e);
            return null;
        }
    }

    /**
     * 合并两个JsonNode
     *
     * @param target 目标JsonNode
     * @param source 源JsonNode
     * @return 合并后的JsonNode
     */
    private static JsonNode merge(JsonNode target, JsonNode source) {
        if (target == null || target.isMissingNode()) {
            return source;
        }
        if (source == null || source.isMissingNode()) {
            return target;
        }
        if (!target.isObject() || !source.isObject()) {
            return source;
        }

        source.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            JsonNode targetValue = target.get(key);
            if (targetValue != null && targetValue.isObject() && value.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) target).set(key, merge(targetValue, value));
            } else {
                ((com.fasterxml.jackson.databind.node.ObjectNode) target).set(key, value);
            }
        });

        return target;
    }

    // ==================== JSON压缩方法 ====================

    /**
     * 压缩JSON字符串（移除所有空白字符）
     *
     * @param json JSON字符串
     * @return 压缩后的JSON字符串
     */
    public static String compress(String json) {
        if (StringUtil.isBlank(json)) {
            return json;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Failed to compress JSON: json={}", json, e);
            return json;
        }
    }
}
