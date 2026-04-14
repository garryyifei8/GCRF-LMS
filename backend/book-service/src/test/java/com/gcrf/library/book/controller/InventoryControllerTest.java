package com.gcrf.library.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.book.dto.request.*;
import com.gcrf.library.book.dto.response.*;
import com.gcrf.library.book.entity.enums.TaskItemStatus;
import com.gcrf.library.book.entity.enums.TaskScope;
import com.gcrf.library.book.entity.enums.TaskStatus;
import com.gcrf.library.book.entity.enums.TaskType;
import com.gcrf.library.book.service.InventoryService;
import com.gcrf.library.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 库存控制器单元测试
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("库存控制器测试")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private InventoryDetailVO testInventory;
    private InventoryTaskVO testTask;
    private InventoryTaskItemVO testTaskItem;

    @BeforeEach
    void setUp() {
        // 初始化测试库存
        testInventory = new InventoryDetailVO();
        testInventory.setId(1L);
        testInventory.setBookId(100L);
        testInventory.setBookTitle("测试图书");
        testInventory.setIsbn("9787111111111");
        testInventory.setLocation("一楼书库");
        testInventory.setShelfNumber("A-001");
        testInventory.setTotalQuantity(10);
        testInventory.setAvailableQuantity(8);
        testInventory.setAlertThreshold(5);
        testInventory.setLastCheckTime(LocalDateTime.now());

        // 初始化测试任务
        testTask = new InventoryTaskVO();
        testTask.setId(1L);
        testTask.setTaskCode("INV-2025-001");
        testTask.setTaskName("年终盘点");
        testTask.setTaskType(TaskType.FULL);
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setScope(TaskScope.ALL);
        testTask.setTotalBooks(100);
        testTask.setCheckedBooks(0);
        testTask.setDiscrepancyCount(0);
        testTask.setOperatorId(1L);
        testTask.setOperatorName("管理员");
        testTask.setCreatedAt(LocalDateTime.now());

        // 初始化测试盘点明细
        testTaskItem = new InventoryTaskItemVO();
        testTaskItem.setId(1L);
        testTaskItem.setTaskId(1L);
        testTaskItem.setBookId(100L);
        testTaskItem.setBookTitle("测试图书");
        testTaskItem.setIsbn("9787111111111");
        testTaskItem.setExpectedQuantity(10);
        testTaskItem.setActualQuantity(10);
        testTaskItem.setDiscrepancy(0);
        testTaskItem.setStatus(TaskItemStatus.CHECKED);
        testTaskItem.setCheckedTime(LocalDateTime.now());
    }

    // ==================== 库存查询测试 ====================

    @Nested
    @DisplayName("库存查询测试")
    class InventoryQueryTests {

        @Test
        @DisplayName("分页查询库存 - 成功")
        void queryInventories_Success() throws Exception {
            // Arrange
            PageResult<InventoryDetailVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.singletonList(testInventory));
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(1);

            when(inventoryService.queryInventories(any())).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records[0].id").value(1))
                    .andExpect(jsonPath("$.data.records[0].bookTitle").value("测试图书"))
                    .andExpect(jsonPath("$.data.total").value(1));

            verify(inventoryService).queryInventories(any());
        }

        @Test
        @DisplayName("分页查询库存 - 带筛选条件")
        void queryInventories_WithFilters() throws Exception {
            // Arrange
            PageResult<InventoryDetailVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.singletonList(testInventory));
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(1);

            when(inventoryService.queryInventories(any())).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory")
                            .param("location", "一楼书库")
                            .param("shelfNumber", "A-001")
                            .param("alertOnly", "true")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());

            verify(inventoryService).queryInventories(any());
        }

        @Test
        @DisplayName("查询库存详情 - 成功")
        void getInventory_Success() throws Exception {
            // Arrange
            when(inventoryService.getInventoryById(1L)).thenReturn(testInventory);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.bookTitle").value("测试图书"))
                    .andExpect(jsonPath("$.data.location").value("一楼书库"));

            verify(inventoryService).getInventoryById(1L);
        }

        @Test
        @DisplayName("查询库存详情 - 不存在返回null")
        void getInventory_NotFound() throws Exception {
            // Arrange
            when(inventoryService.getInventoryById(999L)).thenReturn(null);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/{id}", 999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(inventoryService).getInventoryById(999L);
        }
    }

    // ==================== 库存调整测试 ====================

    @Nested
    @DisplayName("库存调整测试")
    class InventoryAdjustTests {

        @Test
        @DisplayName("库存调整 - 增加库存成功")
        void adjustInventory_Add_Success() throws Exception {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(100L);
            request.setAdjustType("ADD");
            request.setQuantity(5);
            request.setReason("采购入库");

            InventoryDetailVO result = new InventoryDetailVO();
            result.setId(1L);
            result.setBookId(100L);
            result.setTotalQuantity(15);
            result.setAvailableQuantity(13);

            when(inventoryService.adjustInventory(any())).thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.totalQuantity").value(15))
                    .andExpect(jsonPath("$.data.availableQuantity").value(13));

            verify(inventoryService).adjustInventory(any());
        }

        @Test
        @DisplayName("库存调整 - 减少库存成功")
        void adjustInventory_Reduce_Success() throws Exception {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(100L);
            request.setAdjustType("REDUCE");
            request.setQuantity(2);
            request.setReason("报废处理");

            InventoryDetailVO result = new InventoryDetailVO();
            result.setId(1L);
            result.setBookId(100L);
            result.setTotalQuantity(8);
            result.setAvailableQuantity(6);

            when(inventoryService.adjustInventory(any())).thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.totalQuantity").value(8));

            verify(inventoryService).adjustInventory(any());
        }

        @Test
        @DisplayName("库存调整 - 设置库存成功")
        void adjustInventory_Set_Success() throws Exception {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(100L);
            request.setAdjustType("SET");
            request.setQuantity(20);
            request.setReason("盘点调整");

            InventoryDetailVO result = new InventoryDetailVO();
            result.setId(1L);
            result.setBookId(100L);
            result.setTotalQuantity(20);
            result.setAvailableQuantity(18);

            when(inventoryService.adjustInventory(any())).thenReturn(result);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.totalQuantity").value(20));

            verify(inventoryService).adjustInventory(any());
        }

        @Test
        @DisplayName("库存调整 - 缺少必填参数")
        void adjustInventory_MissingRequired() throws Exception {
            // Arrange - 缺少bookId
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setAdjustType("ADD");
            request.setQuantity(5);
            request.setReason("采购入库");

            // Act & Assert - 应该返回400验证错误
            mockMvc.perform(post("/api/v1/inventory/adjust")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(inventoryService, never()).adjustInventory(any());
        }
    }

    // ==================== 库存预警测试 ====================

    @Nested
    @DisplayName("库存预警测试")
    class InventoryAlertTests {

        @Test
        @DisplayName("查询库存预警列表 - 成功")
        void queryAlertInventories_Success() throws Exception {
            // Arrange
            InventoryAlertVO alertVO = new InventoryAlertVO();
            alertVO.setId(1L);
            alertVO.setBookId(100L);
            alertVO.setBookTitle("测试图书");
            alertVO.setAvailableQuantity(2);
            alertVO.setAlertThreshold(5);

            PageResult<InventoryAlertVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.singletonList(alertVO));
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(1);

            when(inventoryService.queryAlertInventories(1, 10)).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/alerts")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records[0].bookTitle").value("测试图书"))
                    .andExpect(jsonPath("$.data.records[0].availableQuantity").value(2));

            verify(inventoryService).queryAlertInventories(1, 10);
        }

        @Test
        @DisplayName("查询库存预警列表 - 空列表")
        void queryAlertInventories_Empty() throws Exception {
            // Arrange
            PageResult<InventoryAlertVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.emptyList());
            pageResult.setTotal(0L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(0);

            when(inventoryService.queryAlertInventories(1, 10)).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/alerts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isEmpty())
                    .andExpect(jsonPath("$.data.total").value(0));

            verify(inventoryService).queryAlertInventories(1, 10);
        }
    }

    // ==================== 盘点任务测试 ====================

    @Nested
    @DisplayName("盘点任务测试")
    class TaskTests {

        @Test
        @DisplayName("创建盘点任务 - 成功")
        void createTask_Success() throws Exception {
            // Arrange
            InventoryTaskCreateRequest request = new InventoryTaskCreateRequest();
            request.setTaskName("年终盘点");
            request.setTaskType("FULL");
            request.setScope("ALL");
            request.setOperatorId(1L);
            request.setOperatorName("管理员");
            request.setNotes("年度例行盘点");

            when(inventoryService.createTask(any())).thenReturn(testTask);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.taskName").value("年终盘点"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(inventoryService).createTask(any());
        }

        @Test
        @DisplayName("分页查询盘点任务 - 成功")
        void queryTasks_Success() throws Exception {
            // Arrange
            PageResult<InventoryTaskVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.singletonList(testTask));
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(1);

            when(inventoryService.queryTasks(any())).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/tasks")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records[0].id").value(1))
                    .andExpect(jsonPath("$.data.records[0].taskName").value("年终盘点"));

            verify(inventoryService).queryTasks(any());
        }

        @Test
        @DisplayName("查询任务详情 - 成功")
        void getTask_Success() throws Exception {
            // Arrange
            when(inventoryService.getTaskById(1L)).thenReturn(testTask);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/tasks/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.taskCode").value("INV-2025-001"))
                    .andExpect(jsonPath("$.data.taskName").value("年终盘点"));

            verify(inventoryService).getTaskById(1L);
        }

        @Test
        @DisplayName("更新盘点任务 - 成功")
        void updateTask_Success() throws Exception {
            // Arrange
            InventoryTaskUpdateRequest request = new InventoryTaskUpdateRequest();
            request.setTaskName("更新后的任务名");
            request.setNotes("更新备注");

            InventoryTaskVO updatedTask = new InventoryTaskVO();
            updatedTask.setId(1L);
            updatedTask.setTaskName("更新后的任务名");
            updatedTask.setStatus(TaskStatus.PENDING);

            when(inventoryService.updateTask(eq(1L), any())).thenReturn(updatedTask);

            // Act & Assert
            mockMvc.perform(put("/api/v1/inventory/tasks/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.taskName").value("更新后的任务名"));

            verify(inventoryService).updateTask(eq(1L), any());
        }
    }

    // ==================== 任务状态转换测试 ====================

    @Nested
    @DisplayName("任务状态转换测试")
    class TaskStatusTransitionTests {

        @Test
        @DisplayName("开始盘点任务 - 成功")
        void startTask_Success() throws Exception {
            // Arrange
            InventoryTaskVO startedTask = new InventoryTaskVO();
            startedTask.setId(1L);
            startedTask.setTaskName("年终盘点");
            startedTask.setStatus(TaskStatus.IN_PROGRESS);
            startedTask.setStartTime(LocalDateTime.now());

            when(inventoryService.startTask(1L)).thenReturn(startedTask);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks/{id}/start", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.startTime").isNotEmpty());

            verify(inventoryService).startTask(1L);
        }

        @Test
        @DisplayName("完成盘点任务 - 成功")
        void completeTask_Success() throws Exception {
            // Arrange
            InventoryTaskVO completedTask = new InventoryTaskVO();
            completedTask.setId(1L);
            completedTask.setTaskName("年终盘点");
            completedTask.setStatus(TaskStatus.COMPLETED);
            completedTask.setStartTime(LocalDateTime.now().minusHours(2));
            completedTask.setEndTime(LocalDateTime.now());
            completedTask.setTotalBooks(100);
            completedTask.setCheckedBooks(100);
            completedTask.setDiscrepancyCount(3);

            when(inventoryService.completeTask(1L)).thenReturn(completedTask);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks/{id}/complete", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.endTime").isNotEmpty())
                    .andExpect(jsonPath("$.data.discrepancyCount").value(3));

            verify(inventoryService).completeTask(1L);
        }

        @Test
        @DisplayName("取消盘点任务 - 成功")
        void cancelTask_Success() throws Exception {
            // Arrange
            InventoryTaskVO cancelledTask = new InventoryTaskVO();
            cancelledTask.setId(1L);
            cancelledTask.setTaskName("年终盘点");
            cancelledTask.setStatus(TaskStatus.CANCELLED);

            when(inventoryService.cancelTask(1L)).thenReturn(cancelledTask);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks/{id}/cancel", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));

            verify(inventoryService).cancelTask(1L);
        }
    }

    // ==================== 盘点明细测试 ====================

    @Nested
    @DisplayName("盘点明细测试")
    class TaskItemTests {

        @Test
        @DisplayName("查询盘点明细 - 成功")
        void queryTaskItems_Success() throws Exception {
            // Arrange
            PageResult<InventoryTaskItemVO> pageResult = new PageResult<>();
            pageResult.setRecords(Collections.singletonList(testTaskItem));
            pageResult.setTotal(1L);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setPages(1);

            when(inventoryService.queryTaskItems(eq(1L), any())).thenReturn(pageResult);

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/tasks/{id}/items", 1L)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records[0].bookTitle").value("测试图书"))
                    .andExpect(jsonPath("$.data.records[0].status").value("CHECKED"));

            verify(inventoryService).queryTaskItems(eq(1L), any());
        }

        @Test
        @DisplayName("批量录入盘点结果 - 成功")
        void recordTaskItems_Success() throws Exception {
            // Arrange
            InventoryTaskItemRequest item1 = new InventoryTaskItemRequest();
            item1.setBookId(100L);
            item1.setActualQuantity(10);

            InventoryTaskItemRequest item2 = new InventoryTaskItemRequest();
            item2.setBookId(101L);
            item2.setActualQuantity(8);

            InventoryTaskItemBatchRequest request = new InventoryTaskItemBatchRequest();
            request.setItems(Arrays.asList(item1, item2));

            InventoryTaskItemVO result1 = new InventoryTaskItemVO();
            result1.setId(1L);
            result1.setBookId(100L);
            result1.setActualQuantity(10);
            result1.setStatus(TaskItemStatus.CHECKED);

            InventoryTaskItemVO result2 = new InventoryTaskItemVO();
            result2.setId(2L);
            result2.setBookId(101L);
            result2.setActualQuantity(8);
            result2.setStatus(TaskItemStatus.CHECKED);

            when(inventoryService.batchRecordTaskItems(eq(1L), any()))
                    .thenReturn(Arrays.asList(result1, result2));

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks/{id}/items", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].bookId").value(100))
                    .andExpect(jsonPath("$.data[1].bookId").value(101));

            verify(inventoryService).batchRecordTaskItems(eq(1L), any());
        }

        @Test
        @DisplayName("录入单个盘点结果 - 成功")
        void recordSingleTaskItem_Success() throws Exception {
            // Arrange - bookId在请求体中仍需提供（虽然controller会覆盖）
            InventoryTaskItemRequest request = new InventoryTaskItemRequest();
            request.setBookId(100L);
            request.setActualQuantity(10);

            when(inventoryService.recordTaskItem(eq(1L), any())).thenReturn(testTaskItem);

            // Act & Assert
            mockMvc.perform(post("/api/v1/inventory/tasks/{taskId}/items/{bookId}", 1L, 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.bookId").value(100))
                    .andExpect(jsonPath("$.data.actualQuantity").value(10));

            verify(inventoryService).recordTaskItem(eq(1L), any());
        }

        @Test
        @DisplayName("获取差异明细 - 成功")
        void getDiscrepancyItems_Success() throws Exception {
            // Arrange
            InventoryTaskItemVO discrepancyItem = new InventoryTaskItemVO();
            discrepancyItem.setId(2L);
            discrepancyItem.setBookId(101L);
            discrepancyItem.setBookTitle("差异图书");
            discrepancyItem.setExpectedQuantity(10);
            discrepancyItem.setActualQuantity(8);
            discrepancyItem.setDiscrepancy(-2);
            discrepancyItem.setStatus(TaskItemStatus.CHECKED);

            when(inventoryService.getDiscrepancyItems(1L))
                    .thenReturn(Collections.singletonList(discrepancyItem));

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/tasks/{id}/discrepancies", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].discrepancy").value(-2))
                    .andExpect(jsonPath("$.data[0].bookTitle").value("差异图书"));

            verify(inventoryService).getDiscrepancyItems(1L);
        }

        @Test
        @DisplayName("获取差异明细 - 无差异")
        void getDiscrepancyItems_Empty() throws Exception {
            // Arrange
            when(inventoryService.getDiscrepancyItems(1L)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/tasks/{id}/discrepancies", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(inventoryService).getDiscrepancyItems(1L);
        }
    }

    // ==================== 健康检查测试 ====================

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("健康检查 - 成功")
        void health_Success() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/inventory/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("Inventory Service is running"));
        }
    }
}
