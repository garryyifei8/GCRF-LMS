package com.gcrf.library.circulation.service;

import com.gcrf.library.circulation.dto.ReserveRequest;
import com.gcrf.library.circulation.dto.response.ReserveDetailVO;
import com.gcrf.library.circulation.dto.response.ReserveVO;
import com.gcrf.library.common.result.PageResult;

import java.util.List;

/**
 * 预约服务接口
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
public interface ReserveService {

    /**
     * 分页查询预约记录
     *
     * @param readerId 读者ID（可选）
     * @param status   预约状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页记录数
     * @return 预约记录分页列表
     */
    PageResult<ReserveVO> queryReserves(Long readerId, String status, Integer pageNum, Integer pageSize);

    /**
     * 根据ID获取预约详情
     *
     * @param id 预约记录ID
     * @return 预约详情
     */
    ReserveDetailVO getReserveById(Long id);

    /**
     * 预约图书
     *
     * @param request 预约请求
     * @return 预约详情
     */
    ReserveDetailVO reserveBook(ReserveRequest request);

    /**
     * 取书（完成预约）
     *
     * @param id 预约记录ID
     * @return 预约详情
     */
    ReserveDetailVO pickupReserve(Long id);

    /**
     * 取消预约
     *
     * @param id 预约记录ID
     * @return 预约详情
     */
    ReserveDetailVO cancelReserve(Long id);

    /**
     * 批量过期处理（定时任务调用）
     * 将已超过过期日期且状态仍为RESERVED的记录状态更新为EXPIRED
     */
    void expireReserves();

    /**
     * 获取需要发送通知的预约记录
     * （预约状态为RESERVED，图书已到馆，且未发送通知或需要重复提醒）
     *
     * @return 待通知预约记录列表
     */
    List<ReserveVO> getPendingNotifications();

    /**
     * 发送预约通知（更新通知状态）
     *
     * @param id 预约记录ID
     * @return 预约详情
     */
    ReserveDetailVO notifyReserve(Long id);
}
