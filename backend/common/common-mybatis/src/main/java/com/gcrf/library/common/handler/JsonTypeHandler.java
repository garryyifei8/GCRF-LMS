package com.gcrf.library.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gcrf.library.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JSON类型处理器
 * 用于处理PostgreSQL的JSON/JSONB类型字段
 * 将Java对象转换为JSON字符串存储，读取时将JSON字符串转换为Java对象
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@Slf4j
@MappedTypes({Object.class})
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final TypeReference<T> typeReference;

    public JsonTypeHandler(TypeReference<T> typeReference) {
        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference cannot be null");
        }
        this.typeReference = typeReference;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JsonUtil.toJson(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    /**
     * 解析JSON字符串
     */
    private T parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonUtil.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", json, e);
            return null;
        }
    }
}
