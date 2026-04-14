package com.gcrf.library.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.book.dto.request.*;
import com.gcrf.library.book.dto.response.*;
import com.gcrf.library.book.entity.*;
import com.gcrf.library.book.entity.enums.*;
import com.gcrf.library.book.mapper.*;
import com.gcrf.library.book.service.InventoryService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存服务实现
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryTaskMapper taskMapper;
    private final InventoryTaskItemMapper taskItemMapper;
    private final BookMapper bookMapper;

    // ==================== 库存管理 ====================

    @Override
    public PageResult<InventoryDetailVO> queryInventories(InventoryQueryRequest request) {
        log.info("分页查询库存: {}", request);

        Page<Inventory> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();

        // 位置筛选
        if (StringUtils.hasText(request.getLocation())) {
            queryWrapper.like(Inventory::getLocation, request.getLocation());
        }

        // 书架号筛选
        if (StringUtils.hasText(request.getShelfNumber())) {
            queryWrapper.like(Inventory::getShelfNumber, request.getShelfNumber());
        }

        // 仅预警库存
        if (Boolean.TRUE.equals(request.getAlertOnly())) {
            queryWrapper.apply("available_quantity <= alert_threshold");
        }

        queryWrapper.orderByDesc(Inventory::getUpdatedAt);

        Page<Inventory> result = inventoryMapper.selectPage(page, queryWrapper);

        // 转换为VO并填充图书信息
        List<InventoryDetailVO> records = result.getRecords().stream()
                .map(this::convertToDetailVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    public InventoryDetailVO getInventoryById(Long id) {
        log.info("查询库存详情: id={}", id);
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "库存记录不存在");
        }
        return convertToDetailVO(inventory);
    }

    @Override
    public InventoryDetailVO getInventoryByBookId(Long bookId) {
        log.info("根据图书ID查询库存: bookId={}", bookId);
        Inventory inventory = inventoryMapper.selectByBookId(bookId);
        if (inventory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该图书没有独立库存记录");
        }
        return convertToDetailVO(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryDetailVO adjustInventory(InventoryAdjustRequest request) {
        log.info("库存调整: bookId={}, adjustType={}, quantity={}, reason={}",
                request.getBookId(), request.getAdjustType(), request.getQuantity(), request.getReason());

        // 查找或创建库存记录
        Inventory inventory = inventoryMapper.selectByBookId(request.getBookId());
        boolean isNew = false;

        if (inventory == null) {
            // 验证图书是否存在
            Book book = bookMapper.selectById(request.getBookId());
            if (book == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "图书不存在");
            }

            // 创建新库存记录
            inventory = new Inventory();
            inventory.setBookId(request.getBookId());
            inventory.setTotalQuantity(0);
            inventory.setAvailableQuantity(0);
            inventory.setBorrowedQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setAlertThreshold(5);
            isNew = true;
        }

        // 根据调整类型处理
        int currentTotal = inventory.getTotalQuantity() != null ? inventory.getTotalQuantity() : 0;
        int currentAvailable = inventory.getAvailableQuantity() != null ? inventory.getAvailableQuantity() : 0;
        int borrowedCount = inventory.getBorrowedQuantity() != null ? inventory.getBorrowedQuantity() : 0;

        int newTotal;
        int newAvailable;

        switch (request.getAdjustType()) {
            case "ADD":
                newTotal = currentTotal + request.getQuantity();
                newAvailable = currentAvailable + request.getQuantity();
                break;
            case "REDUCE":
                if (currentAvailable < request.getQuantity()) {
                    throw new BusinessException(ResultCode.OPERATION_FAILED,
                            String.format("可借数量不足，当前可借: %d，要减少: %d", currentAvailable, request.getQuantity()));
                }
                newTotal = currentTotal - request.getQuantity();
                newAvailable = currentAvailable - request.getQuantity();
                break;
            case "SET":
                if (request.getQuantity() < borrowedCount) {
                    throw new BusinessException(ResultCode.OPERATION_FAILED,
                            String.format("新总量(%d)不能小于已借出数量(%d)", request.getQuantity(), borrowedCount));
                }
                newTotal = request.getQuantity();
                newAvailable = request.getQuantity() - borrowedCount;
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "无效的调整类型");
        }

        inventory.setTotalQuantity(newTotal);
        inventory.setAvailableQuantity(newAvailable);

        // 更新位置信息
        if (StringUtils.hasText(request.getLocation())) {
            inventory.setLocation(request.getLocation());
        }
        if (StringUtils.hasText(request.getShelfNumber())) {
            inventory.setShelfNumber(request.getShelfNumber());
        }
        if (request.getAlertThreshold() != null) {
            inventory.setAlertThreshold(request.getAlertThreshold());
        }

        if (isNew) {
            inventoryMapper.insert(inventory);
        } else {
            inventoryMapper.updateById(inventory);
        }

        log.info("库存调整成功: id={}, newTotal={}, newAvailable={}",
                inventory.getId(), inventory.getTotalQuantity(), inventory.getAvailableQuantity());

        return convertToDetailVO(inventory);
    }

    @Override
    public PageResult<InventoryAlertVO> queryAlertInventories(Integer pageNum, Integer pageSize) {
        log.info("查询库存预警列表: pageNum={}, pageSize={}", pageNum, pageSize);

        int offset = (pageNum - 1) * pageSize;
        List<Inventory> inventories = inventoryMapper.selectAlertInventoriesPage(offset, pageSize);
        long total = inventoryMapper.countAlertInventories();

        List<InventoryAlertVO> records = inventories.stream()
                .map(this::convertToAlertVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(total, pageNum, pageSize, records);
    }

    // ==================== 盘点任务管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskVO createTask(InventoryTaskCreateRequest request) {
        log.info("创建盘点任务: taskName={}, taskType={}, scope={}",
                request.getTaskName(), request.getTaskType(), request.getScope());

        // 验证参数
        if ("LOCATION".equals(request.getScope()) && !StringUtils.hasText(request.getLocation())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "按位置盘点时必须指定位置");
        }

        // 创建任务
        InventoryTask task = new InventoryTask();
        task.setTaskName(request.getTaskName());
        task.setTaskType(TaskType.fromCode(request.getTaskType()));
        task.setStatus(TaskStatus.PENDING);
        task.setScope(TaskScope.fromCode(request.getScope()));
        task.setLocation(request.getLocation());
        task.setPlanStartTime(request.getPlanStartTime());
        task.setPlanEndTime(request.getPlanEndTime());
        task.setOperatorId(request.getOperatorId());
        task.setOperatorName(request.getOperatorName());
        task.setNotes(request.getNotes());
        task.setCheckedBooks(0);
        task.setDiscrepancyCount(0);

        taskMapper.insert(task);

        // 生成盘点明细
        List<Book> booksToCheck = getBooksForTask(request);
        task.setTotalBooks(booksToCheck.size());
        taskMapper.updateById(task);

        // 创建明细记录
        if (!booksToCheck.isEmpty()) {
            List<InventoryTaskItem> items = booksToCheck.stream()
                    .map(book -> {
                        InventoryTaskItem item = new InventoryTaskItem();
                        item.setTaskId(task.getId());
                        item.setBookId(book.getId());
                        item.setBookTitle(book.getTitle());
                        item.setIsbn(book.getIsbn());
                        item.setExpectedQuantity(book.getTotalQuantity() != null ? book.getTotalQuantity() : 0);
                        item.setStatus(TaskItemStatus.PENDING);
                        return item;
                    })
                    .collect(Collectors.toList());

            // 批量插入明细
            for (InventoryTaskItem item : items) {
                taskItemMapper.insert(item);
            }
        }

        log.info("盘点任务创建成功: id={}, taskCode={}, totalBooks={}",
                task.getId(), task.getTaskCode(), task.getTotalBooks());

        return InventoryTaskVO.from(task);
    }

    @Override
    public PageResult<InventoryTaskVO> queryTasks(InventoryTaskQueryRequest request) {
        log.info("分页查询盘点任务: {}", request);

        Page<InventoryTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<InventoryTask> queryWrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(InventoryTask::getTaskName, request.getKeyword())
                    .or().like(InventoryTask::getTaskCode, request.getKeyword())
            );
        }

        // 任务类型筛选
        if (StringUtils.hasText(request.getTaskType())) {
            queryWrapper.eq(InventoryTask::getTaskType, TaskType.fromCode(request.getTaskType()));
        }

        // 状态筛选
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(InventoryTask::getStatus, TaskStatus.fromCode(request.getStatus()));
        }

        // 操作人筛选
        if (request.getOperatorId() != null) {
            queryWrapper.eq(InventoryTask::getOperatorId, request.getOperatorId());
        }

        // 创建时间范围
        if (request.getCreatedAtStart() != null) {
            queryWrapper.ge(InventoryTask::getCreatedAt, request.getCreatedAtStart());
        }
        if (request.getCreatedAtEnd() != null) {
            queryWrapper.le(InventoryTask::getCreatedAt, request.getCreatedAtEnd());
        }

        queryWrapper.orderByDesc(InventoryTask::getCreatedAt);

        Page<InventoryTask> result = taskMapper.selectPage(page, queryWrapper);

        List<InventoryTaskVO> records = result.getRecords().stream()
                .map(InventoryTaskVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    public InventoryTaskVO getTaskById(Long id) {
        log.info("查询任务详情: id={}", id);
        InventoryTask task = findTaskById(id);
        return InventoryTaskVO.from(task);
    }

    @Override
    public InventoryTaskVO getTaskByCode(String taskCode) {
        log.info("根据编号查询任务: taskCode={}", taskCode);
        InventoryTask task = taskMapper.selectByTaskCode(taskCode);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        return InventoryTaskVO.from(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskVO updateTask(Long id, InventoryTaskUpdateRequest request) {
        log.info("更新盘点任务: id={}", id);

        InventoryTask task = findTaskById(id);

        if (!task.canEdit()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "只有待执行状态的任务可以编辑");
        }

        // 更新字段
        if (StringUtils.hasText(request.getTaskName())) {
            task.setTaskName(request.getTaskName());
        }
        if (request.getPlanStartTime() != null) {
            task.setPlanStartTime(request.getPlanStartTime());
        }
        if (request.getPlanEndTime() != null) {
            task.setPlanEndTime(request.getPlanEndTime());
        }
        if (request.getOperatorId() != null) {
            task.setOperatorId(request.getOperatorId());
        }
        if (StringUtils.hasText(request.getOperatorName())) {
            task.setOperatorName(request.getOperatorName());
        }
        if (StringUtils.hasText(request.getNotes())) {
            task.setNotes(request.getNotes());
        }

        taskMapper.updateById(task);

        log.info("盘点任务更新成功: id={}", id);
        return InventoryTaskVO.from(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskVO startTask(Long id) {
        log.info("开始盘点: id={}", id);

        InventoryTask task = findTaskById(id);

        if (!task.canStart()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED,
                    String.format("任务状态[%s]不允许开始", task.getStatus().getDescription()));
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("盘点任务已开始: id={}", id);
        return InventoryTaskVO.from(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskVO completeTask(Long id) {
        log.info("完成盘点: id={}", id);

        InventoryTask task = findTaskById(id);

        if (!task.canComplete()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED,
                    String.format("任务状态[%s]不允许完成", task.getStatus().getDescription()));
        }

        // 统计盘点结果
        long checkedCount = taskItemMapper.countByTaskIdAndStatus(id, TaskItemStatus.CHECKED.getCode());
        long discrepancyCount = taskItemMapper.countDiscrepancyByTaskId(id);

        task.setStatus(TaskStatus.COMPLETED);
        task.setEndTime(LocalDateTime.now());
        task.setCheckedBooks((int) checkedCount);
        task.setDiscrepancyCount((int) discrepancyCount);
        taskMapper.updateById(task);

        // 更新图书的最后盘点时间
        List<InventoryTaskItem> checkedItems = taskItemMapper.selectByTaskId(id).stream()
                .filter(item -> TaskItemStatus.CHECKED.equals(item.getStatus()))
                .collect(Collectors.toList());

        if (!checkedItems.isEmpty()) {
            List<Long> bookIds = checkedItems.stream()
                    .map(InventoryTaskItem::getBookId)
                    .collect(Collectors.toList());
            inventoryMapper.batchUpdateLastCheckTime(bookIds);
        }

        log.info("盘点任务已完成: id={}, checkedBooks={}, discrepancyCount={}",
                id, task.getCheckedBooks(), task.getDiscrepancyCount());

        return InventoryTaskVO.from(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskVO cancelTask(Long id) {
        log.info("取消盘点任务: id={}", id);

        InventoryTask task = findTaskById(id);

        if (!task.canCancel()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED,
                    String.format("任务状态[%s]不允许取消", task.getStatus().getDescription()));
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("盘点任务已取消: id={}", id);
        return InventoryTaskVO.from(task);
    }

    // ==================== 盘点明细管理 ====================

    @Override
    public PageResult<InventoryTaskItemVO> queryTaskItems(Long taskId, InventoryTaskItemQueryRequest request) {
        log.info("分页查询盘点明细: taskId={}, request={}", taskId, request);

        // 验证任务存在
        findTaskById(taskId);

        Page<InventoryTaskItem> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<InventoryTaskItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InventoryTaskItem::getTaskId, taskId);

        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(InventoryTaskItem::getBookTitle, request.getKeyword())
                    .or().like(InventoryTaskItem::getIsbn, request.getKeyword())
            );
        }

        // 状态筛选
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(InventoryTaskItem::getStatus, TaskItemStatus.fromCode(request.getStatus()));
        }

        // 仅差异
        if (Boolean.TRUE.equals(request.getDiscrepancyOnly())) {
            queryWrapper.eq(InventoryTaskItem::getStatus, TaskItemStatus.CHECKED);
            queryWrapper.ne(InventoryTaskItem::getDiscrepancy, 0);
        }

        queryWrapper.orderByAsc(InventoryTaskItem::getId);

        Page<InventoryTaskItem> result = taskItemMapper.selectPage(page, queryWrapper);

        List<InventoryTaskItemVO> records = result.getRecords().stream()
                .map(InventoryTaskItemVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTaskItemVO recordTaskItem(Long taskId, InventoryTaskItemRequest request) {
        log.info("录入盘点结果: taskId={}, bookId={}, actualQuantity={}",
                taskId, request.getBookId(), request.getActualQuantity());

        // 验证任务状态
        InventoryTask task = findTaskById(taskId);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "只有进行中的任务可以录入结果");
        }

        // 查找明细
        InventoryTaskItem item = taskItemMapper.selectByTaskIdAndBookId(taskId, request.getBookId());
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该图书不在盘点范围内");
        }

        if (!item.canCheck()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该图书已盘点");
        }

        // 录入结果
        item.setActualQuantity(request.getActualQuantity());
        item.calculateDiscrepancy();
        item.setStatus(TaskItemStatus.CHECKED);
        item.setCheckedTime(LocalDateTime.now());
        item.setCheckerId(request.getCheckerId());
        item.setCheckerName(request.getCheckerName());
        item.setNotes(request.getNotes());

        taskItemMapper.updateById(item);

        // 更新任务统计
        taskMapper.incrementCheckedBooks(taskId, 1);
        if (item.hasDiscrepancy()) {
            taskMapper.incrementDiscrepancyCount(taskId, 1);
        }

        log.info("盘点结果录入成功: itemId={}, discrepancy={}", item.getId(), item.getDiscrepancy());

        return InventoryTaskItemVO.from(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InventoryTaskItemVO> batchRecordTaskItems(Long taskId, InventoryTaskItemBatchRequest request) {
        log.info("批量录入盘点结果: taskId={}, itemCount={}", taskId, request.getItems().size());

        List<InventoryTaskItemVO> results = new ArrayList<>();
        for (InventoryTaskItemRequest itemRequest : request.getItems()) {
            try {
                InventoryTaskItemVO vo = recordTaskItem(taskId, itemRequest);
                results.add(vo);
            } catch (BusinessException e) {
                log.warn("录入失败: bookId={}, error={}", itemRequest.getBookId(), e.getMessage());
            }
        }

        log.info("批量录入完成: taskId={}, successCount={}", taskId, results.size());
        return results;
    }

    @Override
    public List<InventoryTaskItemVO> getDiscrepancyItems(Long taskId) {
        log.info("获取差异明细: taskId={}", taskId);

        // 验证任务存在
        findTaskById(taskId);

        List<InventoryTaskItem> items = taskItemMapper.selectDiscrepancyItems(taskId);

        return items.stream()
                .map(InventoryTaskItemVO::from)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    private InventoryTask findTaskById(Long id) {
        InventoryTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private InventoryDetailVO convertToDetailVO(Inventory inventory) {
        Book book = bookMapper.selectById(inventory.getBookId());
        String bookTitle = book != null ? book.getTitle() : null;
        String isbn = book != null ? book.getIsbn() : null;
        return InventoryDetailVO.from(inventory, bookTitle, isbn);
    }

    private InventoryAlertVO convertToAlertVO(Inventory inventory) {
        Book book = bookMapper.selectById(inventory.getBookId());
        String bookTitle = book != null ? book.getTitle() : null;
        String isbn = book != null ? book.getIsbn() : null;
        return InventoryAlertVO.from(inventory, bookTitle, isbn);
    }

    private List<Book> getBooksForTask(InventoryTaskCreateRequest request) {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Book::getDeletedAt);
        queryWrapper.eq(Book::getStatus, "ACTIVE");

        // 根据范围筛选
        switch (request.getScope()) {
            case "ALL":
                // 全部图书
                break;
            case "LOCATION":
                // 按位置，需要关联库存表
                // 这里简化处理，实际可能需要联表查询
                break;
            case "CATEGORY":
                if (StringUtils.hasText(request.getCategoryCode())) {
                    queryWrapper.eq(Book::getClassificationCode, request.getCategoryCode());
                }
                break;
            default:
                break;
        }

        // 如果是抽查盘点且指定了图书ID
        if ("SPOT".equals(request.getTaskType()) && !CollectionUtils.isEmpty(request.getBookIds())) {
            queryWrapper.in(Book::getId, request.getBookIds());
        }

        queryWrapper.orderByAsc(Book::getId);

        // 限制最大数量，避免一次查询太多
        Page<Book> page = new Page<>(1, 10000);
        return bookMapper.selectPage(page, queryWrapper).getRecords();
    }
}
