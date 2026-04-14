package com.gcrf.library.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.book.dto.request.*;
import com.gcrf.library.book.dto.response.*;
import com.gcrf.library.book.entity.*;
import com.gcrf.library.book.entity.enums.*;
import com.gcrf.library.book.mapper.*;
import com.gcrf.library.book.service.impl.InventoryServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryService单元测试
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存服务测试")
class InventoryServiceTest {

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryTaskMapper taskMapper;

    @Mock
    private InventoryTaskItemMapper taskItemMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory testInventory;
    private Book testBook;
    private InventoryTask testTask;
    private InventoryTaskItem testTaskItem;

    @BeforeEach
    void setUp() {
        // 准备测试图书
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111100001");
        testBook.setTitle("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setTotalQuantity(10);
        testBook.setStatus("ACTIVE");

        // 准备测试库存
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setBookId(1L);
        testInventory.setLocation("一楼书库");
        testInventory.setShelfNumber("A-01-01");
        testInventory.setTotalQuantity(10);
        testInventory.setAvailableQuantity(8);
        testInventory.setBorrowedQuantity(2);
        testInventory.setReservedQuantity(0);
        testInventory.setAlertThreshold(5);
        testInventory.setCreatedAt(LocalDateTime.now());
        testInventory.setUpdatedAt(LocalDateTime.now());

        // 准备测试盘点任务
        testTask = new InventoryTask();
        testTask.setId(1L);
        testTask.setTaskName("测试盘点任务");
        testTask.setTaskCode("INV-20251220-001");
        testTask.setTaskType(TaskType.FULL);
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setScope(TaskScope.ALL);
        testTask.setTotalBooks(100);
        testTask.setCheckedBooks(0);
        testTask.setDiscrepancyCount(0);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());

        // 准备测试盘点明细
        testTaskItem = new InventoryTaskItem();
        testTaskItem.setId(1L);
        testTaskItem.setTaskId(1L);
        testTaskItem.setBookId(1L);
        testTaskItem.setBookTitle("测试图书");
        testTaskItem.setIsbn("9787111100001");
        testTaskItem.setExpectedQuantity(10);
        testTaskItem.setStatus(TaskItemStatus.PENDING);
    }

    // ==================== 库存管理测试 ====================

    @Nested
    @DisplayName("库存查询测试")
    class InventoryQueryTests {

        @Test
        @DisplayName("分页查询库存 - 成功")
        void queryInventories_Success() {
            // Arrange
            InventoryQueryRequest request = new InventoryQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);

            Page<Inventory> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testInventory));
            mockPage.setTotal(1);

            when(inventoryMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            PageResult<InventoryDetailVO> result = inventoryService.queryInventories(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getBookTitle()).isEqualTo("测试图书");
        }

        @Test
        @DisplayName("分页查询库存 - 按位置筛选")
        void queryInventories_WithLocationFilter() {
            // Arrange
            InventoryQueryRequest request = new InventoryQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);
            request.setLocation("一楼书库");

            Page<Inventory> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testInventory));
            mockPage.setTotal(1);

            when(inventoryMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            PageResult<InventoryDetailVO> result = inventoryService.queryInventories(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            verify(inventoryMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("根据ID查询库存 - 成功")
        void getInventoryById_Success() {
            // Arrange
            when(inventoryMapper.selectById(1L)).thenReturn(testInventory);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.getInventoryById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getBookTitle()).isEqualTo("测试图书");
            assertThat(result.getTotalQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("根据ID查询库存 - 库存不存在")
        void getInventoryById_NotFound() {
            // Arrange
            when(inventoryMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.getInventoryById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存记录不存在");
        }

        @Test
        @DisplayName("根据图书ID查询库存 - 成功")
        void getInventoryByBookId_Success() {
            // Arrange
            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.getInventoryByBookId(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBookId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("根据图书ID查询库存 - 不存在")
        void getInventoryByBookId_NotFound() {
            // Arrange
            when(inventoryMapper.selectByBookId(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.getInventoryByBookId(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("没有独立库存记录");
        }
    }

    @Nested
    @DisplayName("库存调整测试")
    class InventoryAdjustTests {

        @Test
        @DisplayName("库存调整 - ADD增加成功")
        void adjustInventory_AddSuccess() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("ADD");
            request.setQuantity(5);
            request.setReason("采购入库");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);
            when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.adjustInventory(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalQuantity()).isEqualTo(15);  // 10 + 5
            assertThat(result.getAvailableQuantity()).isEqualTo(13);  // 8 + 5
            verify(inventoryMapper).updateById(any(Inventory.class));
        }

        @Test
        @DisplayName("库存调整 - REDUCE减少成功")
        void adjustInventory_ReduceSuccess() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("REDUCE");
            request.setQuantity(3);
            request.setReason("报废处理");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);
            when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.adjustInventory(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalQuantity()).isEqualTo(7);  // 10 - 3
            assertThat(result.getAvailableQuantity()).isEqualTo(5);  // 8 - 3
        }

        @Test
        @DisplayName("库存调整 - REDUCE失败（可借数量不足）")
        void adjustInventory_ReduceFail_InsufficientQuantity() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("REDUCE");
            request.setQuantity(20);  // 超过可借数量
            request.setReason("报废处理");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.adjustInventory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("可借数量不足");
        }

        @Test
        @DisplayName("库存调整 - SET设置成功")
        void adjustInventory_SetSuccess() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("SET");
            request.setQuantity(20);
            request.setReason("盘点调整");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);
            when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.adjustInventory(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalQuantity()).isEqualTo(20);
            assertThat(result.getAvailableQuantity()).isEqualTo(18);  // 20 - 2(borrowed)
        }

        @Test
        @DisplayName("库存调整 - SET失败（新总量小于已借出）")
        void adjustInventory_SetFail_LessThanBorrowed() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("SET");
            request.setQuantity(1);  // 小于已借出数量2
            request.setReason("盘点调整");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.adjustInventory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能小于已借出数量");
        }

        @Test
        @DisplayName("库存调整 - 新建库存记录")
        void adjustInventory_CreateNewInventory() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(2L);
            request.setAdjustType("ADD");
            request.setQuantity(10);
            request.setReason("新书入库");
            request.setLocation("二楼阅览室");
            request.setShelfNumber("B-01-01");

            Book newBook = new Book();
            newBook.setId(2L);
            newBook.setTitle("新图书");
            newBook.setIsbn("9787111100002");

            when(inventoryMapper.selectByBookId(2L)).thenReturn(null);
            when(bookMapper.selectById(2L)).thenReturn(newBook);
            when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

            // Act
            InventoryDetailVO result = inventoryService.adjustInventory(request);

            // Assert
            assertThat(result).isNotNull();
            verify(inventoryMapper).insert(any(Inventory.class));
            verify(inventoryMapper, never()).updateById(any(Inventory.class));
        }

        @Test
        @DisplayName("库存调整 - 图书不存在")
        void adjustInventory_BookNotFound() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(999L);
            request.setAdjustType("ADD");
            request.setQuantity(5);
            request.setReason("采购入库");

            when(inventoryMapper.selectByBookId(999L)).thenReturn(null);
            when(bookMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.adjustInventory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("图书不存在");
        }

        @Test
        @DisplayName("库存调整 - 无效的调整类型")
        void adjustInventory_InvalidAdjustType() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("INVALID");
            request.setQuantity(5);
            request.setReason("测试");

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.adjustInventory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的调整类型");
        }
    }

    @Nested
    @DisplayName("库存预警测试")
    class InventoryAlertTests {

        @Test
        @DisplayName("查询库存预警列表 - 成功")
        void queryAlertInventories_Success() {
            // Arrange
            Inventory alertInventory = new Inventory();
            alertInventory.setId(2L);
            alertInventory.setBookId(2L);
            alertInventory.setTotalQuantity(5);
            alertInventory.setAvailableQuantity(2);
            alertInventory.setAlertThreshold(5);

            when(inventoryMapper.selectAlertInventoriesPage(anyInt(), anyInt()))
                    .thenReturn(Arrays.asList(alertInventory));
            when(inventoryMapper.countAlertInventories()).thenReturn(1L);
            when(bookMapper.selectById(2L)).thenReturn(testBook);

            // Act
            PageResult<InventoryAlertVO> result = inventoryService.queryAlertInventories(1, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
        }
    }

    // ==================== 盘点任务测试 ====================

    @Nested
    @DisplayName("盘点任务创建测试")
    class TaskCreateTests {

        @Test
        @DisplayName("创建盘点任务 - 成功")
        void createTask_Success() {
            // Arrange
            InventoryTaskCreateRequest request = new InventoryTaskCreateRequest();
            request.setTaskName("2024年年终盘点");
            request.setTaskType("FULL");
            request.setScope("ALL");
            request.setOperatorId(1L);
            request.setOperatorName("管理员");

            Page<Book> mockBookPage = new Page<>(1, 10000);
            mockBookPage.setRecords(Arrays.asList(testBook));

            when(taskMapper.insert(any(InventoryTask.class))).thenReturn(1);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);
            when(bookMapper.selectPage(any(Page.class), any())).thenReturn(mockBookPage);
            when(taskItemMapper.insert(any(InventoryTaskItem.class))).thenReturn(1);

            // Act
            InventoryTaskVO result = inventoryService.createTask(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTaskName()).isEqualTo("2024年年终盘点");
            assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
            verify(taskMapper).insert(any(InventoryTask.class));
            verify(taskItemMapper, atLeastOnce()).insert(any(InventoryTaskItem.class));
        }

        @Test
        @DisplayName("创建盘点任务 - 按位置盘点但未指定位置")
        void createTask_LocationScopeWithoutLocation() {
            // Arrange
            InventoryTaskCreateRequest request = new InventoryTaskCreateRequest();
            request.setTaskName("一楼盘点");
            request.setTaskType("PARTIAL");
            request.setScope("LOCATION");
            // 未设置location

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.createTask(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("必须指定位置");
        }
    }

    @Nested
    @DisplayName("盘点任务查询测试")
    class TaskQueryTests {

        @Test
        @DisplayName("分页查询任务 - 成功")
        void queryTasks_Success() {
            // Arrange
            InventoryTaskQueryRequest request = new InventoryTaskQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);

            Page<InventoryTask> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testTask));
            mockPage.setTotal(1);

            when(taskMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            // Act
            PageResult<InventoryTaskVO> result = inventoryService.queryTasks(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTaskName()).isEqualTo("测试盘点任务");
        }

        @Test
        @DisplayName("分页查询任务 - 按状态筛选")
        void queryTasks_FilterByStatus() {
            // Arrange
            InventoryTaskQueryRequest request = new InventoryTaskQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);
            request.setStatus("PENDING");

            Page<InventoryTask> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testTask));
            mockPage.setTotal(1);

            when(taskMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            // Act
            PageResult<InventoryTaskVO> result = inventoryService.queryTasks(request);

            // Assert
            assertThat(result).isNotNull();
            verify(taskMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("根据ID查询任务 - 成功")
        void getTaskById_Success() {
            // Arrange
            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act
            InventoryTaskVO result = inventoryService.getTaskById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTaskName()).isEqualTo("测试盘点任务");
        }

        @Test
        @DisplayName("根据ID查询任务 - 任务不存在")
        void getTaskById_NotFound() {
            // Arrange
            when(taskMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.getTaskById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("任务不存在");
        }

        @Test
        @DisplayName("根据编号查询任务 - 成功")
        void getTaskByCode_Success() {
            // Arrange
            when(taskMapper.selectByTaskCode("INV-20251220-001")).thenReturn(testTask);

            // Act
            InventoryTaskVO result = inventoryService.getTaskByCode("INV-20251220-001");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTaskCode()).isEqualTo("INV-20251220-001");
        }
    }

    @Nested
    @DisplayName("盘点任务状态变更测试")
    class TaskStatusTests {

        @Test
        @DisplayName("开始盘点 - 成功")
        void startTask_Success() {
            // Arrange
            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);

            // Act
            InventoryTaskVO result = inventoryService.startTask(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            verify(taskMapper).updateById(any(InventoryTask.class));
        }

        @Test
        @DisplayName("开始盘点 - 任务状态不允许")
        void startTask_InvalidStatus() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);  // 已经在进行中
            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.startTask(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许开始");
        }

        @Test
        @DisplayName("完成盘点 - 成功")
        void completeTask_Success() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);
            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);
            when(taskItemMapper.countByTaskIdAndStatus(1L, "CHECKED")).thenReturn(50L);
            when(taskItemMapper.countDiscrepancyByTaskId(1L)).thenReturn(2L);
            when(taskItemMapper.selectByTaskId(1L)).thenReturn(Collections.emptyList());

            // Act
            InventoryTaskVO result = inventoryService.completeTask(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            verify(taskMapper).updateById(any(InventoryTask.class));
        }

        @Test
        @DisplayName("完成盘点 - 任务状态不允许")
        void completeTask_InvalidStatus() {
            // Arrange
            testTask.setStatus(TaskStatus.PENDING);  // 还未开始
            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.completeTask(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许完成");
        }

        @Test
        @DisplayName("取消任务 - 成功（待执行状态）")
        void cancelTask_Success_Pending() {
            // Arrange
            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);

            // Act
            InventoryTaskVO result = inventoryService.cancelTask(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("取消任务 - 成功（进行中状态）")
        void cancelTask_Success_InProgress() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);
            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);

            // Act
            InventoryTaskVO result = inventoryService.cancelTask(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("取消任务 - 已完成的任务无法取消")
        void cancelTask_CannotCancelCompleted() {
            // Arrange
            testTask.setStatus(TaskStatus.COMPLETED);
            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.cancelTask(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许取消");
        }

        @Test
        @DisplayName("更新任务 - 成功")
        void updateTask_Success() {
            // Arrange
            InventoryTaskUpdateRequest request = new InventoryTaskUpdateRequest();
            request.setTaskName("更新后的任务名");
            request.setNotes("更新备注");

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskMapper.updateById(any(InventoryTask.class))).thenReturn(1);

            // Act
            InventoryTaskVO result = inventoryService.updateTask(1L, request);

            // Assert
            assertThat(result).isNotNull();
            verify(taskMapper).updateById(any(InventoryTask.class));
        }

        @Test
        @DisplayName("更新任务 - 非待执行状态无法编辑")
        void updateTask_CannotEditNonPending() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);
            InventoryTaskUpdateRequest request = new InventoryTaskUpdateRequest();
            request.setTaskName("更新后的任务名");

            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.updateTask(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只有待执行状态");
        }
    }

    // ==================== 盘点明细测试 ====================

    @Nested
    @DisplayName("盘点明细测试")
    class TaskItemTests {

        @Test
        @DisplayName("分页查询盘点明细 - 成功")
        void queryTaskItems_Success() {
            // Arrange
            InventoryTaskItemQueryRequest request = new InventoryTaskItemQueryRequest();
            request.setPageNum(1);
            request.setPageSize(20);

            Page<InventoryTaskItem> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Arrays.asList(testTaskItem));
            mockPage.setTotal(1);

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            // Act
            PageResult<InventoryTaskItemVO> result = inventoryService.queryTaskItems(1L, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getBookTitle()).isEqualTo("测试图书");
        }

        @Test
        @DisplayName("录入盘点结果 - 成功")
        void recordTaskItem_Success() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);
            testTaskItem.setStatus(TaskItemStatus.PENDING);

            InventoryTaskItemRequest request = new InventoryTaskItemRequest();
            request.setBookId(1L);
            request.setActualQuantity(10);
            request.setCheckerId(1L);
            request.setCheckerName("盘点员");

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectByTaskIdAndBookId(1L, 1L)).thenReturn(testTaskItem);
            when(taskItemMapper.updateById(any(InventoryTaskItem.class))).thenReturn(1);
            doNothing().when(taskMapper).incrementCheckedBooks(anyLong(), anyInt());

            // Act
            InventoryTaskItemVO result = inventoryService.recordTaskItem(1L, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getActualQuantity()).isEqualTo(10);
            assertThat(result.getDiscrepancy()).isEqualTo(0);  // 10 - 10 = 0
            verify(taskItemMapper).updateById(any(InventoryTaskItem.class));
        }

        @Test
        @DisplayName("录入盘点结果 - 任务未开始")
        void recordTaskItem_TaskNotStarted() {
            // Arrange
            testTask.setStatus(TaskStatus.PENDING);

            InventoryTaskItemRequest request = new InventoryTaskItemRequest();
            request.setBookId(1L);
            request.setActualQuantity(10);

            when(taskMapper.selectById(1L)).thenReturn(testTask);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.recordTaskItem(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只有进行中的任务");
        }

        @Test
        @DisplayName("录入盘点结果 - 图书不在盘点范围")
        void recordTaskItem_BookNotInScope() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);

            InventoryTaskItemRequest request = new InventoryTaskItemRequest();
            request.setBookId(999L);
            request.setActualQuantity(10);

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectByTaskIdAndBookId(1L, 999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.recordTaskItem(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不在盘点范围内");
        }

        @Test
        @DisplayName("录入盘点结果 - 图书已盘点")
        void recordTaskItem_AlreadyChecked() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);
            testTaskItem.setStatus(TaskItemStatus.CHECKED);

            InventoryTaskItemRequest request = new InventoryTaskItemRequest();
            request.setBookId(1L);
            request.setActualQuantity(10);

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectByTaskIdAndBookId(1L, 1L)).thenReturn(testTaskItem);

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.recordTaskItem(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已盘点");
        }

        @Test
        @DisplayName("批量录入盘点结果 - 部分成功")
        void batchRecordTaskItems_PartialSuccess() {
            // Arrange
            testTask.setStatus(TaskStatus.IN_PROGRESS);

            InventoryTaskItem item1 = new InventoryTaskItem();
            item1.setId(1L);
            item1.setTaskId(1L);
            item1.setBookId(1L);
            item1.setExpectedQuantity(10);
            item1.setStatus(TaskItemStatus.PENDING);

            InventoryTaskItem item2 = new InventoryTaskItem();
            item2.setId(2L);
            item2.setTaskId(1L);
            item2.setBookId(2L);
            item2.setExpectedQuantity(5);
            item2.setStatus(TaskItemStatus.CHECKED);  // 已盘点

            InventoryTaskItemRequest req1 = new InventoryTaskItemRequest();
            req1.setBookId(1L);
            req1.setActualQuantity(10);

            InventoryTaskItemRequest req2 = new InventoryTaskItemRequest();
            req2.setBookId(2L);
            req2.setActualQuantity(5);

            InventoryTaskItemBatchRequest batchRequest = new InventoryTaskItemBatchRequest();
            batchRequest.setItems(Arrays.asList(req1, req2));

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectByTaskIdAndBookId(1L, 1L)).thenReturn(item1);
            when(taskItemMapper.selectByTaskIdAndBookId(1L, 2L)).thenReturn(item2);
            when(taskItemMapper.updateById(any(InventoryTaskItem.class))).thenReturn(1);
            doNothing().when(taskMapper).incrementCheckedBooks(anyLong(), anyInt());

            // Act
            List<InventoryTaskItemVO> results = inventoryService.batchRecordTaskItems(1L, batchRequest);

            // Assert
            assertThat(results).hasSize(1);  // 只有1个成功
            assertThat(results.get(0).getBookId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("获取差异明细 - 成功")
        void getDiscrepancyItems_Success() {
            // Arrange
            InventoryTaskItem discrepancyItem = new InventoryTaskItem();
            discrepancyItem.setId(1L);
            discrepancyItem.setTaskId(1L);
            discrepancyItem.setBookId(1L);
            discrepancyItem.setBookTitle("测试图书");
            discrepancyItem.setExpectedQuantity(10);
            discrepancyItem.setActualQuantity(8);
            discrepancyItem.setDiscrepancy(-2);
            discrepancyItem.setStatus(TaskItemStatus.CHECKED);

            when(taskMapper.selectById(1L)).thenReturn(testTask);
            when(taskItemMapper.selectDiscrepancyItems(1L)).thenReturn(Arrays.asList(discrepancyItem));

            // Act
            List<InventoryTaskItemVO> results = inventoryService.getDiscrepancyItems(1L);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDiscrepancy()).isEqualTo(-2);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("库存调整 - 更新位置和预警阈值")
        void adjustInventory_UpdateLocationAndThreshold() {
            // Arrange
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setBookId(1L);
            request.setAdjustType("ADD");
            request.setQuantity(0);  // 不调整数量，只更新位置
            request.setReason("位置变更");
            request.setLocation("三楼特藏室");
            request.setShelfNumber("C-01-01");
            request.setAlertThreshold(10);

            when(inventoryMapper.selectByBookId(1L)).thenReturn(testInventory);
            when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            // Act
            InventoryDetailVO result = inventoryService.adjustInventory(request);

            // Assert
            assertThat(result).isNotNull();
            verify(inventoryMapper).updateById(any(Inventory.class));
        }

        @Test
        @DisplayName("任务进度计算 - 零图书")
        void taskProgressPercentage_ZeroBooks() {
            // Arrange
            testTask.setTotalBooks(0);
            testTask.setCheckedBooks(0);

            // Act & Assert
            assertThat(testTask.getProgressPercentage()).isEqualTo(0);
        }

        @Test
        @DisplayName("任务进度计算 - 部分完成")
        void taskProgressPercentage_PartialComplete() {
            // Arrange
            testTask.setTotalBooks(100);
            testTask.setCheckedBooks(50);

            // Act & Assert
            assertThat(testTask.getProgressPercentage()).isEqualTo(50);
        }

        @Test
        @DisplayName("库存预警判断 - 需要预警")
        void inventoryAlertRequired_True() {
            // Arrange
            testInventory.setAvailableQuantity(3);
            testInventory.setAlertThreshold(5);

            // Act & Assert
            assertThat(testInventory.isAlertRequired()).isTrue();
        }

        @Test
        @DisplayName("库存预警判断 - 不需要预警")
        void inventoryAlertRequired_False() {
            // Arrange
            testInventory.setAvailableQuantity(10);
            testInventory.setAlertThreshold(5);

            // Act & Assert
            assertThat(testInventory.isAlertRequired()).isFalse();
        }
    }
}
