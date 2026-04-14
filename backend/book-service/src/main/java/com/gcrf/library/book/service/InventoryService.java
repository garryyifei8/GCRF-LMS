package com.gcrf.library.book.service;

import com.gcrf.library.book.dto.request.*;
import com.gcrf.library.book.dto.response.*;
import com.gcrf.library.common.result.PageResult;

import java.util.List;

/**
 * 库存服务接口
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
public interface InventoryService {

    // ==================== 库存管理 ====================

    /**
     * 分页查询库存
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<InventoryDetailVO> queryInventories(InventoryQueryRequest request);

    /**
     * 根据ID查询库存详情
     *
     * @param id 库存ID
     * @return 库存详情
     */
    InventoryDetailVO getInventoryById(Long id);

    /**
     * 根据图书ID查询库存
     *
     * @param bookId 图书ID
     * @return 库存详情
     */
    InventoryDetailVO getInventoryByBookId(Long bookId);

    /**
     * 库存调整
     *
     * @param request 调整请求
     * @return 调整后的库存详情
     */
    InventoryDetailVO adjustInventory(InventoryAdjustRequest request);

    /**
     * 查询库存预警列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 预警库存分页结果
     */
    PageResult<InventoryAlertVO> queryAlertInventories(Integer pageNum, Integer pageSize);

    // ==================== 盘点任务管理 ====================

    /**
     * 创建盘点任务
     *
     * @param request 创建请求
     * @return 任务详情
     */
    InventoryTaskVO createTask(InventoryTaskCreateRequest request);

    /**
     * 分页查询盘点任务
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<InventoryTaskVO> queryTasks(InventoryTaskQueryRequest request);

    /**
     * 根据ID查询任务详情
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InventoryTaskVO getTaskById(Long id);

    /**
     * 根据任务编号查询任务详情
     *
     * @param taskCode 任务编号
     * @return 任务详情
     */
    InventoryTaskVO getTaskByCode(String taskCode);

    /**
     * 更新盘点任务
     *
     * @param id      任务ID
     * @param request 更新请求
     * @return 更新后的任务详情
     */
    InventoryTaskVO updateTask(Long id, InventoryTaskUpdateRequest request);

    /**
     * 开始盘点
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InventoryTaskVO startTask(Long id);

    /**
     * 完成盘点
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InventoryTaskVO completeTask(Long id);

    /**
     * 取消盘点任务
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InventoryTaskVO cancelTask(Long id);

    // ==================== 盘点明细管理 ====================

    /**
     * 分页查询盘点明细
     *
     * @param taskId  任务ID
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<InventoryTaskItemVO> queryTaskItems(Long taskId, InventoryTaskItemQueryRequest request);

    /**
     * 录入单个盘点结果
     *
     * @param taskId  任务ID
     * @param request 录入请求
     * @return 明细详情
     */
    InventoryTaskItemVO recordTaskItem(Long taskId, InventoryTaskItemRequest request);

    /**
     * 批量录入盘点结果
     *
     * @param taskId  任务ID
     * @param request 批量录入请求
     * @return 录入成功的明细列表
     */
    List<InventoryTaskItemVO> batchRecordTaskItems(Long taskId, InventoryTaskItemBatchRequest request);

    /**
     * 获取任务的差异明细
     *
     * @param taskId 任务ID
     * @return 差异明细列表
     */
    List<InventoryTaskItemVO> getDiscrepancyItems(Long taskId);
}
