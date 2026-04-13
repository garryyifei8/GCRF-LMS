package com.gcrf.library.reader.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.reader.dto.request.ReaderTypeCreateRequest;
import com.gcrf.library.reader.dto.request.ReaderTypeUpdateRequest;
import com.gcrf.library.reader.dto.response.ReaderTypeVO;
import com.gcrf.library.reader.entity.ReaderType;
import com.gcrf.library.reader.mapper.ReaderTypeMapper;
import com.gcrf.library.reader.service.impl.ReaderTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ReaderTypeService Unit Tests
 *
 * Tests coverage:
 * - List operations (2 tests)
 * - CRUD operations (8 tests)
 * - Business validation (4 tests)
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReaderTypeService Unit Tests")
class ReaderTypeServiceTest {

    @Mock
    private ReaderTypeMapper readerTypeMapper;

    @InjectMocks
    private ReaderTypeServiceImpl readerTypeService;

    private ReaderType testType;

    @BeforeEach
    void setUp() {
        testType = createTestReaderType("STUDENT", "Student", 10, 30, 2);
    }

    // ==================== Helper Methods ====================

    private ReaderType createTestReaderType(String typeCode, String typeName,
                                             Integer maxBorrow, Integer maxDays, Integer maxRenew) {
        ReaderType type = new ReaderType();
        type.setId(1L);
        type.setTypeCode(typeCode);
        type.setTypeName(typeName);
        type.setMaxBorrowCount(maxBorrow);
        type.setMaxBorrowDays(maxDays);
        type.setMaxRenewCount(maxRenew);
        type.setDepositAmount(0);
        type.setDescription("Test reader type: " + typeName);
        type.setStatus("ACTIVE");
        type.setSortOrder(1);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        return type;
    }

    private ReaderTypeCreateRequest createCreateRequest(String typeCode, String typeName) {
        ReaderTypeCreateRequest request = new ReaderTypeCreateRequest();
        request.setTypeCode(typeCode);
        request.setTypeName(typeName);
        request.setMaxBorrowCount(10);
        request.setMaxBorrowDays(30);
        request.setMaxRenewCount(2);
        request.setDepositAmount(0);
        request.setDescription("New type");
        request.setStatus("ACTIVE");
        request.setSortOrder(1);
        return request;
    }

    private ReaderTypeUpdateRequest createUpdateRequest(Long id) {
        ReaderTypeUpdateRequest request = new ReaderTypeUpdateRequest();
        request.setId(id);
        request.setTypeName("Updated Name");
        request.setMaxBorrowCount(15);
        request.setMaxBorrowDays(45);
        request.setMaxRenewCount(3);
        request.setStatus("ACTIVE");
        return request;
    }

    // ==================== List Operations Tests (2) ====================

    @Test
    @DisplayName("List all types - Returns sorted list")
    void testListAllTypes_Success() {
        // Arrange
        ReaderType type1 = createTestReaderType("STUDENT", "Student", 10, 30, 2);
        type1.setSortOrder(1);
        ReaderType type2 = createTestReaderType("TEACHER", "Teacher", 20, 60, 3);
        type2.setId(2L);
        type2.setSortOrder(2);

        when(readerTypeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(type1, type2));
        when(readerTypeMapper.countReadersByType(anyString())).thenReturn(5);

        // Act
        List<ReaderTypeVO> result = readerTypeService.listAllTypes();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTypeCode()).isEqualTo("STUDENT");
        assertThat(result.get(1).getTypeCode()).isEqualTo("TEACHER");
        assertThat(result.get(0).getReaderCount()).isEqualTo(5);

        verify(readerTypeMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(readerTypeMapper, times(2)).countReadersByType(anyString());
    }

    @Test
    @DisplayName("List all types - Empty list")
    void testListAllTypes_Empty() {
        // Arrange
        when(readerTypeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<ReaderTypeVO> result = readerTypeService.listAllTypes();

        // Assert
        assertThat(result).isEmpty();
        verify(readerTypeMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(readerTypeMapper, never()).countReadersByType(anyString());
    }

    // ==================== Get By ID Tests (2) ====================

    @Test
    @DisplayName("Get type by ID - Success")
    void testGetTypeById_Success() {
        // Arrange
        when(readerTypeMapper.selectById(1L)).thenReturn(testType);
        when(readerTypeMapper.countReadersByType("STUDENT")).thenReturn(10);

        // Act
        ReaderTypeVO result = readerTypeService.getTypeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTypeCode()).isEqualTo("STUDENT");
        assertThat(result.getReaderCount()).isEqualTo(10);

        verify(readerTypeMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("Get type by ID - Not found")
    void testGetTypeById_NotFound() {
        // Arrange
        when(readerTypeMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> readerTypeService.getTypeById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999");

        verify(readerTypeMapper, times(1)).selectById(999L);
    }

    // ==================== Create Tests (3) ====================

    @Test
    @DisplayName("Create type - Success")
    void testCreateType_Success() {
        // Arrange
        ReaderTypeCreateRequest request = createCreateRequest("VIP", "VIP Reader");

        when(readerTypeMapper.existsByTypeCode("VIP", null)).thenReturn(0);
        when(readerTypeMapper.insert(any(ReaderType.class))).thenReturn(1);

        // Act
        ReaderTypeVO result = readerTypeService.createType(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTypeCode()).isEqualTo("VIP");
        assertThat(result.getTypeName()).isEqualTo("VIP Reader");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<ReaderType> captor = ArgumentCaptor.forClass(ReaderType.class);
        verify(readerTypeMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getTypeCode()).isEqualTo("VIP");
    }

    @Test
    @DisplayName("Create type - Duplicate type code")
    void testCreateType_DuplicateCode() {
        // Arrange
        ReaderTypeCreateRequest request = createCreateRequest("STUDENT", "Duplicate");

        when(readerTypeMapper.existsByTypeCode("STUDENT", null)).thenReturn(1);

        // Act & Assert
        assertThatThrownBy(() -> readerTypeService.createType(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("STUDENT");

        verify(readerTypeMapper, never()).insert(any(ReaderType.class));
    }

    @Test
    @DisplayName("Create type - With default values")
    void testCreateType_WithDefaults() {
        // Arrange
        ReaderTypeCreateRequest request = createCreateRequest("TEMP", "Temporary");
        request.setStatus(null); // Should default to ACTIVE
        request.setSortOrder(null); // Should default to 0

        when(readerTypeMapper.existsByTypeCode("TEMP", null)).thenReturn(0);
        when(readerTypeMapper.insert(any(ReaderType.class))).thenReturn(1);

        // Act
        ReaderTypeVO result = readerTypeService.createType(request);

        // Assert
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSortOrder()).isEqualTo(0);
    }

    // ==================== Update Tests (3) ====================

    @Test
    @DisplayName("Update type - Success")
    void testUpdateType_Success() {
        // Arrange
        ReaderTypeUpdateRequest request = createUpdateRequest(1L);

        when(readerTypeMapper.selectById(1L)).thenReturn(testType);
        when(readerTypeMapper.updateById(any(ReaderType.class))).thenReturn(1);
        when(readerTypeMapper.countReadersByType("STUDENT")).thenReturn(5);

        // Act
        ReaderTypeVO result = readerTypeService.updateType(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTypeName()).isEqualTo("Updated Name");
        assertThat(result.getMaxBorrowCount()).isEqualTo(15);
        assertThat(result.getMaxBorrowDays()).isEqualTo(45);

        verify(readerTypeMapper, times(1)).updateById(any(ReaderType.class));
    }

    @Test
    @DisplayName("Update type - Not found")
    void testUpdateType_NotFound() {
        // Arrange
        ReaderTypeUpdateRequest request = createUpdateRequest(999L);

        when(readerTypeMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> readerTypeService.updateType(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999");

        verify(readerTypeMapper, never()).updateById(any(ReaderType.class));
    }

    @Test
    @DisplayName("Update type - Partial update")
    void testUpdateType_PartialUpdate() {
        // Arrange
        ReaderTypeUpdateRequest request = new ReaderTypeUpdateRequest();
        request.setId(1L);
        request.setTypeName("Only Name Updated");
        // Other fields remain null

        when(readerTypeMapper.selectById(1L)).thenReturn(testType);
        when(readerTypeMapper.updateById(any(ReaderType.class))).thenReturn(1);
        when(readerTypeMapper.countReadersByType("STUDENT")).thenReturn(5);

        // Act
        ReaderTypeVO result = readerTypeService.updateType(request);

        // Assert
        assertThat(result.getTypeName()).isEqualTo("Only Name Updated");
        // Original values should be preserved
        assertThat(result.getMaxBorrowCount()).isEqualTo(10);
        assertThat(result.getMaxBorrowDays()).isEqualTo(30);
    }

    // ==================== Delete Tests (3) ====================

    @Test
    @DisplayName("Delete type - Success")
    void testDeleteType_Success() {
        // Arrange
        when(readerTypeMapper.selectById(1L)).thenReturn(testType);
        when(readerTypeMapper.countReadersByType("STUDENT")).thenReturn(0);
        when(readerTypeMapper.updateById(any(ReaderType.class))).thenReturn(1);

        // Act
        readerTypeService.deleteType(1L);

        // Assert
        ArgumentCaptor<ReaderType> captor = ArgumentCaptor.forClass(ReaderType.class);
        verify(readerTypeMapper, times(1)).updateById(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Delete type - Not found")
    void testDeleteType_NotFound() {
        // Arrange
        when(readerTypeMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> readerTypeService.deleteType(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("999");

        verify(readerTypeMapper, never()).updateById(any(ReaderType.class));
    }

    @Test
    @DisplayName("Delete type - In use by readers")
    void testDeleteType_InUse() {
        // Arrange
        when(readerTypeMapper.selectById(1L)).thenReturn(testType);
        when(readerTypeMapper.countReadersByType("STUDENT")).thenReturn(10);

        // Act & Assert
        assertThatThrownBy(() -> readerTypeService.deleteType(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10");

        verify(readerTypeMapper, never()).updateById(any(ReaderType.class));
    }

    // ==================== Helper Method Tests (1) ====================

    @Test
    @DisplayName("Check type code exists - True")
    void testExistsByTypeCode_True() {
        // Arrange
        when(readerTypeMapper.existsByTypeCode("STUDENT", null)).thenReturn(1);

        // Act
        boolean result = readerTypeService.existsByTypeCode("STUDENT", null);

        // Assert
        assertThat(result).isTrue();
    }
}
