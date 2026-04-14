package com.gcrf.library.reader.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.reader.dto.request.ReaderTypeCreateRequest;
import com.gcrf.library.reader.dto.request.ReaderTypeUpdateRequest;
import com.gcrf.library.reader.entity.ReaderType;
import com.gcrf.library.reader.mapper.ReaderTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReaderTypeController集成测试
 *
 * 测试覆盖范围：
 * - 读者类型列表查询（验证Flyway V001默认类型）
 * - 根据ID查询读者类型
 * - 创建读者类型（成功/重复/校验失败）
 * - 更新读者类型
 * - 删除读者类型（软删除）
 *
 * 默认类型来自 Flyway V001 基线: STUDENT / TEACHER / STAFF / EXTERNAL
 *
 * @author GCRF Team
 * @date 2026-04-13
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ReaderTypeControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReaderTypeMapper readerTypeMapper;

    // ========== 列表查询测试 ==========

    @Test
    void listAllTypes_shouldReturnAllTypes() throws Exception {
        // Verify 4 default types from Flyway V001 baseline
        mockMvc.perform(get("/api/v1/readers/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data[*].typeCode").value(hasItem("STUDENT")))
                .andExpect(jsonPath("$.data[*].typeCode").value(hasItem("TEACHER")))
                .andExpect(jsonPath("$.data[*].typeCode").value(hasItem("STAFF")))
                .andExpect(jsonPath("$.data[*].typeCode").value(hasItem("EXTERNAL")));
    }

    // ========== 根据ID查询测试 ==========

    @Test
    void getTypeById_success_shouldReturnType() throws Exception {
        // Arrange - find the STUDENT type from V001 baseline
        LambdaQueryWrapper<ReaderType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReaderType::getTypeCode, "STUDENT")
               .isNull(ReaderType::getDeletedAt);
        ReaderType studentType = readerTypeMapper.selectOne(wrapper);

        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/types/{id}", studentType.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(studentType.getId()))
                .andExpect(jsonPath("$.data.typeCode").value("STUDENT"));
    }

    @Test
    void getTypeById_whenNotFound_shouldReturnError() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/types/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 创建读者类型测试 ==========

    @Test
    void createType_success_shouldPersist() throws Exception {
        // Arrange
        ReaderTypeCreateRequest request = new ReaderTypeCreateRequest();
        request.setTypeCode("TEST_TYPE");
        request.setTypeName("测试类型");
        request.setMaxBorrowCount(8);
        request.setMaxBorrowDays(30);
        request.setMaxRenewCount(2);
        request.setDepositAmount(100);
        request.setDescription("集成测试使用的读者类型");
        request.setStatus("ACTIVE");
        request.setSortOrder(99);

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeCode").value("TEST_TYPE"))
                .andExpect(jsonPath("$.data.typeName").value("测试类型"))
                .andExpect(jsonPath("$.data.maxBorrowCount").value(8))
                .andExpect(jsonPath("$.data.maxBorrowDays").value(30))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void createType_withDuplicateCode_shouldReturnError() throws Exception {
        // Arrange - STUDENT already exists via Flyway V001
        ReaderTypeCreateRequest request = new ReaderTypeCreateRequest();
        request.setTypeCode("STUDENT");
        request.setTypeName("重复的学生类型");
        request.setMaxBorrowCount(10);
        request.setMaxBorrowDays(30);
        request.setMaxRenewCount(2);

        // Act & Assert - service throws BusinessException, returned with non-200 code
        mockMvc.perform(post("/api/v1/readers/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void createType_withInvalidData_shouldReturn400() throws Exception {
        // Arrange - Missing required typeCode
        ReaderTypeCreateRequest request = new ReaderTypeCreateRequest();
        // typeCode intentionally omitted (violates @NotBlank)
        request.setTypeName("缺少代码的类型");
        request.setMaxBorrowCount(5);
        request.setMaxBorrowDays(15);
        request.setMaxRenewCount(1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新读者类型测试 ==========

    @Test
    void updateType_success_shouldUpdateFields() throws Exception {
        // Arrange - insert a fresh type to update (rolled back by @Transactional)
        ReaderType type = new ReaderType();
        type.setTypeCode("UPDATE_TEST");
        type.setTypeName("更新测试类型");
        type.setMaxBorrowCount(5);
        type.setMaxBorrowDays(15);
        type.setMaxRenewCount(1);
        type.setDepositAmount(0);
        type.setStatus("ACTIVE");
        type.setSortOrder(90);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        readerTypeMapper.insert(type);

        ReaderTypeUpdateRequest request = new ReaderTypeUpdateRequest();
        request.setTypeName("已更新的类型名");
        request.setMaxBorrowCount(12);
        request.setMaxBorrowDays(45);
        request.setDescription("更新后的描述");

        // Act & Assert
        mockMvc.perform(put("/api/v1/readers/types/{id}", type.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeName").value("已更新的类型名"))
                .andExpect(jsonPath("$.data.maxBorrowCount").value(12))
                .andExpect(jsonPath("$.data.maxBorrowDays").value(45))
                .andExpect(jsonPath("$.data.description").value("更新后的描述"));
    }

    // ========== 删除读者类型测试 ==========

    @Test
    void deleteType_success_shouldSoftDelete() throws Exception {
        // Arrange - create a fresh type with no readers using it (rolled back by @Transactional)
        ReaderType type = new ReaderType();
        type.setTypeCode("DELETE_TEST");
        type.setTypeName("待删除类型");
        type.setMaxBorrowCount(3);
        type.setMaxBorrowDays(7);
        type.setMaxRenewCount(0);
        type.setDepositAmount(0);
        type.setStatus("ACTIVE");
        type.setSortOrder(91);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        readerTypeMapper.insert(type);
        Long typeId = type.getId();

        // Act - delete
        mockMvc.perform(delete("/api/v1/readers/types/{id}", typeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Assert - soft-deleted: deleted_at is set, record still exists in DB
        ReaderType afterDelete = readerTypeMapper.selectById(typeId);
        org.junit.jupiter.api.Assertions.assertNotNull(afterDelete, "Record should still exist (soft delete)");
        org.junit.jupiter.api.Assertions.assertNotNull(afterDelete.getDeletedAt(), "deleted_at should be populated");

        // Assert - subsequent GET by id should error (treated as not-found)
        mockMvc.perform(get("/api/v1/readers/types/{id}", typeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }
}
