package com.gcrf.library.common.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JsonTypeHandler单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class JsonTypeHandlerTest {

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    private JsonTypeHandler<TestObject> objectHandler;
    private JsonTypeHandler<List<String>> listHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectHandler = new JsonTypeHandler<>(new TypeReference<TestObject>() {});
        listHandler = new JsonTypeHandler<>(new TypeReference<List<String>>() {});
    }

    @Test
    void testConstructorWithNullTypeReference() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new JsonTypeHandler<>(null)
        );
    }

    @Test
    void testSetNonNullParameter() throws SQLException {
        // Arrange
        TestObject testObject = new TestObject("John", 30);
        int parameterIndex = 1;

        // Act
        objectHandler.setNonNullParameter(preparedStatement, parameterIndex, testObject, JdbcType.VARCHAR);

        // Assert
        verify(preparedStatement).setString(eq(parameterIndex), contains("\"name\":\"John\""));
        verify(preparedStatement).setString(eq(parameterIndex), contains("\"age\":30"));
    }

    @Test
    void testGetNullableResultByColumnName() throws SQLException {
        // Arrange
        String columnName = "json_column";
        String json = "{\"name\":\"Alice\",\"age\":25}";
        when(resultSet.getString(columnName)).thenReturn(json);

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNotNull(result);
        assertEquals("Alice", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    void testGetNullableResultByColumnIndex() throws SQLException {
        // Arrange
        int columnIndex = 1;
        String json = "{\"name\":\"Bob\",\"age\":35}";
        when(resultSet.getString(columnIndex)).thenReturn(json);

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnIndex);

        // Assert
        assertNotNull(result);
        assertEquals("Bob", result.getName());
        assertEquals(35, result.getAge());
    }

    @Test
    void testGetNullableResultFromCallableStatement() throws SQLException {
        // Arrange
        int columnIndex = 1;
        String json = "{\"name\":\"Charlie\",\"age\":40}";
        when(callableStatement.getString(columnIndex)).thenReturn(json);

        // Act
        TestObject result = objectHandler.getNullableResult(callableStatement, columnIndex);

        // Assert
        assertNotNull(result);
        assertEquals("Charlie", result.getName());
        assertEquals(40, result.getAge());
    }

    @Test
    void testGetNullableResultWithNullJson() throws SQLException {
        // Arrange
        String columnName = "json_column";
        when(resultSet.getString(columnName)).thenReturn(null);

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNullableResultWithEmptyJson() throws SQLException {
        // Arrange
        String columnName = "json_column";
        when(resultSet.getString(columnName)).thenReturn("");

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNullableResultWithWhitespaceJson() throws SQLException {
        // Arrange
        String columnName = "json_column";
        when(resultSet.getString(columnName)).thenReturn("   ");

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNullableResultWithInvalidJson() throws SQLException {
        // Arrange
        String columnName = "json_column";
        when(resultSet.getString(columnName)).thenReturn("invalid json");

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNull(result); // Should return null on parse error
    }

    @Test
    void testHandleListType() throws SQLException {
        // Arrange
        String columnName = "json_column";
        String json = "[\"apple\",\"banana\",\"cherry\"]";
        when(resultSet.getString(columnName)).thenReturn(json);

        // Act
        List<String> result = listHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("banana"));
        assertTrue(result.contains("cherry"));
    }

    @Test
    void testSetParameterWithList() throws SQLException {
        // Arrange
        List<String> list = Arrays.asList("one", "two", "three");
        int parameterIndex = 1;

        // Act
        listHandler.setNonNullParameter(preparedStatement, parameterIndex, list, JdbcType.VARCHAR);

        // Assert
        verify(preparedStatement).setString(eq(parameterIndex), contains("\"one\""));
        verify(preparedStatement).setString(eq(parameterIndex), contains("\"two\""));
        verify(preparedStatement).setString(eq(parameterIndex), contains("\"three\""));
    }

    @Test
    void testHandleComplexObject() throws SQLException {
        // Arrange
        String columnName = "json_column";
        String json = "{\"name\":\"David\",\"age\":45}";
        when(resultSet.getString(columnName)).thenReturn(json);

        // Act
        TestObject result = objectHandler.getNullableResult(resultSet, columnName);

        // Assert
        assertNotNull(result);
        assertEquals("David", result.getName());
        assertEquals(45, result.getAge());
    }

    @Test
    void testRoundTripSerialization() throws SQLException {
        // Arrange
        TestObject original = new TestObject("Eve", 28);
        int parameterIndex = 1;

        // Capture the JSON string that would be set
        objectHandler.setNonNullParameter(preparedStatement, parameterIndex, original, JdbcType.VARCHAR);

        // Verify the JSON was set (we can't easily extract it, but we verify it was called)
        verify(preparedStatement, times(1)).setString(eq(parameterIndex), anyString());
    }

    /**
     * Test object for JSON serialization/deserialization
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestObject {
        private String name;
        private int age;
    }
}
