package com.gcrf.library.reader.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.reader.client.CirculationServiceClient;
import com.gcrf.library.reader.dto.ReaderCreateRequest;
import com.gcrf.library.reader.dto.ReaderUpdateRequest;
import com.gcrf.library.reader.entity.Reader;
import com.gcrf.library.reader.mapper.ReaderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReaderController集成测试
 *
 * 测试覆盖范围：
 * - 查询接口（分页、条件过滤）
 * - 根据ID查询
 * - 根据读者证号查询
 * - 创建读者
 * - 更新读者
 * - 删除读者
 * - 借书卡管理（激活、挂失、注销）
 * - 健康检查
 *
 * 使用正确的15字段Reader实体结构（移除了18个phantom字段）
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ReaderControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @MockBean
    private CirculationServiceClient circulationServiceClient;

    private Reader testReader;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        LambdaQueryWrapper<Reader> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reader::getReaderId, "INTTEST001");
        readerMapper.delete(queryWrapper);

        // Create test reader with corrected 15-field structure
        // ONLY use valid fields: id, readerId, name, idCard, phone, email,
        // readerType, department, studentNo, employeeNo, maxBorrowCount,
        // maxBorrowDays, status, expiryDate, avatarUrl, createdAt, updatedAt, deletedAt
        testReader = new Reader();
        testReader.setReaderId("INTTEST001");
        testReader.setName("集成测试读者");
        testReader.setIdCard("110101199001011234");
        testReader.setPhone("13900139000");
        testReader.setEmail("integration@test.com");
        testReader.setReaderType("STUDENT");
        testReader.setDepartment("计算机学院");
        testReader.setStudentNo("2021001001");
        testReader.setEmployeeNo(null);
        testReader.setMaxBorrowCount(10);
        testReader.setMaxBorrowDays(30);
        testReader.setStatus("ACTIVE");
        testReader.setExpiryDate(LocalDate.of(2025, 12, 31));
        testReader.setAvatarUrl("https://example.com/avatar/test001.jpg");
        testReader.setCreatedAt(LocalDateTime.now());
        testReader.setUpdatedAt(LocalDateTime.now());

        int insertResult = readerMapper.insert(testReader);
        if (insertResult != 1) {
            throw new RuntimeException("Failed to insert test reader");
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        LambdaQueryWrapper<Reader> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reader::getReaderId, "INTTEST001")
                .or().eq(Reader::getReaderId, "INTTEST002")
                .or().eq(Reader::getReaderId, "INTTEST003")
                .or().eq(Reader::getReaderId, "INTTEST004")
                .or().eq(Reader::getReaderId, "INTTEST005");
        readerMapper.delete(queryWrapper);
    }

    // ========== 查询接口测试 (6 tests) ==========

    @Test
    void testQueryReaders_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void testQueryReaders_WithFilters() throws Exception {
        // Act & Assert - 按读者类型过滤
        mockMvc.perform(get("/api/v1/readers")
                        .param("readerType", "STUDENT")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReaders_WithName() throws Exception {
        // Act & Assert - 按姓名模糊查询
        mockMvc.perform(get("/api/v1/readers")
                        .param("name", "集成测试")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReaders_WithDepartment() throws Exception {
        // Act & Assert - 按院系查询
        mockMvc.perform(get("/api/v1/readers")
                        .param("department", "计算机")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReaders_WithStatus() throws Exception {
        // Act & Assert - 按状态查询
        mockMvc.perform(get("/api/v1/readers")
                        .param("status", "ACTIVE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReaders_EmptyResult() throws Exception {
        // Act & Assert - 查询不存在的读者
        mockMvc.perform(get("/api/v1/readers")
                        .param("name", "不存在的读者XXXXXX")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== 根据ID查询测试 (4 tests) ==========

    @Test
    void testGetReaderById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/{id}", testReader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testReader.getId()))
                .andExpect(jsonPath("$.data.readerId").value("INTTEST001"))
                .andExpect(jsonPath("$.data.name").value("集成测试读者"))
                .andExpect(jsonPath("$.data.readerType").value("STUDENT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetReaderById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testGetReaderByReaderId_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/readerId/{readerId}", "INTTEST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerId").value("INTTEST001"))
                .andExpect(jsonPath("$.data.name").value("集成测试读者"));
    }

    @Test
    void testGetReaderByReaderId_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/readerId/{readerId}", "NOTEXIST999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 创建读者测试 (4 tests) ==========

    @Test
    void testCreateReader_Success() throws Exception {
        // Arrange
        ReaderCreateRequest request = new ReaderCreateRequest();
        request.setReaderId("INTTEST002");
        request.setName("新读者测试");
        request.setReaderType("TEACHER");
        request.setIdCard("110101199002022345");
        request.setPhone("13900139001");
        request.setEmail("newreader@test.com");
        request.setDepartment("外语学院");

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerId").value("INTTEST002"))
                .andExpect(jsonPath("$.data.name").value("新读者测试"))
                .andExpect(jsonPath("$.data.readerType").value("TEACHER"));
    }

    @Test
    void testCreateReader_InvalidData() throws Exception {
        // Arrange - Missing required fields
        ReaderCreateRequest request = new ReaderCreateRequest();
        request.setReaderId(""); // Empty readerId
        request.setName(""); // Empty name

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReader_DuplicateReaderId() throws Exception {
        // Arrange - Use same readerId as testReader
        ReaderCreateRequest request = new ReaderCreateRequest();
        request.setReaderId("INTTEST001"); // Duplicate
        request.setName("重复读者证号");
        request.setReaderType("STUDENT");
        request.setPhone("13900139002");
        request.setEmail("duplicate@test.com");

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testCreateReader_InvalidPhone() throws Exception {
        // Arrange - Invalid phone format
        ReaderCreateRequest request = new ReaderCreateRequest();
        request.setReaderId("INTTEST003");
        request.setName("手机号错误测试");
        request.setReaderType("STUDENT");
        request.setPhone("12345678901"); // Invalid format (starts with 1 but second digit not 3-9)
        request.setEmail("invalid@test.com");

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新读者测试 (3 tests) ==========

    @Test
    void testUpdateReader_Success() throws Exception {
        // Arrange
        ReaderUpdateRequest request = new ReaderUpdateRequest();
        request.setId(testReader.getId());
        request.setName("更新后的姓名");
        request.setPhone("13900139999");
        request.setEmail("updated@test.com");
        request.setDepartment("软件学院");
        request.setAvatarUrl("https://example.com/avatar/updated.jpg");
        request.setVersion(1); // Optimistic lock

        // Act & Assert
        mockMvc.perform(put("/api/v1/readers/{id}", testReader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后的姓名"))
                .andExpect(jsonPath("$.data.phone").value("139****9999"));
    }

    @Test
    void testUpdateReader_InvalidData() throws Exception {
        // Arrange - Invalid email format
        ReaderUpdateRequest request = new ReaderUpdateRequest();
        request.setId(testReader.getId());
        request.setEmail("invalid-email"); // Invalid format
        request.setVersion(1);

        // Act & Assert
        mockMvc.perform(put("/api/v1/readers/{id}", testReader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateReader_NotFound() throws Exception {
        // Arrange
        ReaderUpdateRequest request = new ReaderUpdateRequest();
        request.setId(999999L);
        request.setName("不存在的读者");
        request.setVersion(1);

        // Act & Assert
        mockMvc.perform(put("/api/v1/readers/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 删除读者测试 (2 tests) ==========

    @Test
    void testDeleteReader_Success() throws Exception {
        // Arrange - Create a new reader for deletion
        Reader readerToDelete = new Reader();
        readerToDelete.setReaderId("INTTEST004");
        readerToDelete.setName("待删除读者");
        readerToDelete.setIdCard("110101199004044567");
        readerToDelete.setPhone("13900139004");
        readerToDelete.setEmail("delete@test.com");
        readerToDelete.setReaderType("STAFF");
        readerToDelete.setDepartment("行政部");
        readerToDelete.setEmployeeNo("EMP2024004");
        readerToDelete.setMaxBorrowCount(5);
        readerToDelete.setMaxBorrowDays(30);
        readerToDelete.setStatus("ACTIVE");
        readerToDelete.setExpiryDate(LocalDate.of(2025, 12, 31));
        readerToDelete.setCreatedAt(LocalDateTime.now());
        readerToDelete.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(readerToDelete);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/readers/{id}", readerToDelete.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify deletion (soft delete) - should return 404
        mockMvc.perform(get("/api/v1/readers/{id}", readerToDelete.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testDeleteReader_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/readers/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 借书卡管理测试 (6 tests) ==========

    @Test
    void testActivateCard_Success() throws Exception {
        // Arrange - Create a suspended reader
        Reader suspendedReader = new Reader();
        suspendedReader.setReaderId("INTTEST003");
        suspendedReader.setName("挂失读者");
        suspendedReader.setIdCard("110101199003033456");
        suspendedReader.setPhone("13900139003");
        suspendedReader.setEmail("suspended@test.com");
        suspendedReader.setReaderType("STUDENT");
        suspendedReader.setDepartment("经济学院");
        suspendedReader.setStudentNo("2021003003");
        suspendedReader.setMaxBorrowCount(10);
        suspendedReader.setMaxBorrowDays(30);
        suspendedReader.setStatus("SUSPENDED");
        suspendedReader.setExpiryDate(LocalDate.of(2025, 12, 31));
        suspendedReader.setCreatedAt(LocalDateTime.now());
        suspendedReader.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(suspendedReader);

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/activate", suspendedReader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Cleanup
        readerMapper.deleteById(suspendedReader.getId());
    }

    @Test
    void testActivateCard_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/activate", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testSuspendCard_Success() throws Exception {
        // Act & Assert - Suspend the active test reader
        mockMvc.perform(post("/api/v1/readers/{id}/suspend", testReader.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // Restore status for other tests
        testReader.setStatus("ACTIVE");
        readerMapper.updateById(testReader);
    }

    @Test
    void testSuspendCard_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/suspend", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testCancelCard_Success() throws Exception {
        // Arrange - Create a reader for cancellation
        Reader readerToCancel = new Reader();
        readerToCancel.setReaderId("INTTEST005");
        readerToCancel.setName("注销读者");
        readerToCancel.setIdCard("110101199005055678");
        readerToCancel.setPhone("13900139005");
        readerToCancel.setEmail("cancel@test.com");
        readerToCancel.setReaderType("EXTERNAL");
        readerToCancel.setDepartment("校外人员");
        readerToCancel.setMaxBorrowCount(3);
        readerToCancel.setMaxBorrowDays(15);
        readerToCancel.setStatus("ACTIVE");
        readerToCancel.setExpiryDate(LocalDate.of(2025, 12, 31));
        readerToCancel.setCreatedAt(LocalDateTime.now());
        readerToCancel.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(readerToCancel);

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/cancel", readerToCancel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));

        // Cleanup
        readerMapper.deleteById(readerToCancel.getId());
    }

    @Test
    void testCancelCard_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/cancel", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 健康检查测试 (1 test) ==========

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/readers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Reader Service is running"));
    }

    // ========== P0新增端点测试 (5 tests) ==========

    @Test
    void batchDelete_shouldDeleteMultipleReaders() throws Exception {
        // Arrange - Create two readers to delete
        Reader r1 = new Reader();
        r1.setReaderId("INTTEST002");
        r1.setName("批删读者1");
        r1.setIdCard("110101199002020001");
        r1.setPhone("13900139021");
        r1.setEmail("batch1@test.com");
        r1.setReaderType("STUDENT");
        r1.setDepartment("数学学院");
        r1.setStudentNo("2021002001");
        r1.setMaxBorrowCount(10);
        r1.setMaxBorrowDays(30);
        r1.setStatus("ACTIVE");
        r1.setExpiryDate(LocalDate.of(2025, 12, 31));
        r1.setCreatedAt(LocalDateTime.now());
        r1.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(r1);

        Reader r2 = new Reader();
        r2.setReaderId("INTTEST003");
        r2.setName("批删读者2");
        r2.setIdCard("110101199003030002");
        r2.setPhone("13900139022");
        r2.setEmail("batch2@test.com");
        r2.setReaderType("STUDENT");
        r2.setDepartment("物理学院");
        r2.setStudentNo("2021003002");
        r2.setMaxBorrowCount(10);
        r2.setMaxBorrowDays(30);
        r2.setStatus("ACTIVE");
        r2.setExpiryDate(LocalDate.of(2025, 12, 31));
        r2.setCreatedAt(LocalDateTime.now());
        r2.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(r2);

        // Mock no active borrows
        when(circulationServiceClient.getCurrentBorrowCount(anyLong()))
                .thenReturn(Result.success(0));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/readers/batch")
                        .param("ids", r1.getId() + "," + r2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify both readers were soft-deleted
        Reader deleted1 = readerMapper.selectById(r1.getId());
        Reader deleted2 = readerMapper.selectById(r2.getId());
        org.junit.jupiter.api.Assertions.assertNull(deleted1);
        org.junit.jupiter.api.Assertions.assertNull(deleted2);
    }

    @Test
    void issueCard_shouldActivateCard() throws Exception {
        // Arrange - Create a suspended reader
        Reader suspended = new Reader();
        suspended.setReaderId("INTTEST004");
        suspended.setName("办证读者");
        suspended.setIdCard("110101199004040003");
        suspended.setPhone("13900139023");
        suspended.setEmail("issuecard@test.com");
        suspended.setReaderType("STUDENT");
        suspended.setDepartment("化学学院");
        suspended.setStudentNo("2021004001");
        suspended.setMaxBorrowCount(10);
        suspended.setMaxBorrowDays(30);
        suspended.setStatus("SUSPENDED");
        suspended.setExpiryDate(LocalDate.of(2025, 12, 31));
        suspended.setCreatedAt(LocalDateTime.now());
        suspended.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(suspended);

        Map<String, Object> body = Map.of(
                "cardExpireDate", "2027-12-31",
                "depositAmount", 100
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/{id}/card", suspended.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void updateReaderStatus_suspended_shouldSuspendCard() throws Exception {
        // testReader is ACTIVE - suspend it
        Map<String, String> body = Map.of("status", "suspended");

        mockMvc.perform(put("/api/v1/readers/{id}/status", testReader.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void updateReaderStatus_active_shouldActivateCard() throws Exception {
        // Arrange - Create a suspended reader
        Reader suspended = new Reader();
        suspended.setReaderId("INTTEST005");
        suspended.setName("激活读者");
        suspended.setIdCard("110101199005050004");
        suspended.setPhone("13900139024");
        suspended.setEmail("activate@test.com");
        suspended.setReaderType("STUDENT");
        suspended.setDepartment("生物学院");
        suspended.setStudentNo("2021005001");
        suspended.setMaxBorrowCount(10);
        suspended.setMaxBorrowDays(30);
        suspended.setStatus("SUSPENDED");
        suspended.setExpiryDate(LocalDate.of(2025, 12, 31));
        suspended.setCreatedAt(LocalDateTime.now());
        suspended.setUpdatedAt(LocalDateTime.now());
        readerMapper.insert(suspended);

        Map<String, String> body = Map.of("status", "active");

        mockMvc.perform(put("/api/v1/readers/{id}/status", suspended.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getReaderByCardNumber_shouldReturnReader() throws Exception {
        // testReader has readerId "INTTEST001" - use it as the card number
        mockMvc.perform(get("/api/v1/readers/card/{cardNumber}", "INTTEST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerId").value("INTTEST001"))
                .andExpect(jsonPath("$.data.name").value("集成测试读者"));
    }
}
