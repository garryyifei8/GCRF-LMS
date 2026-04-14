package com.gcrf.library.circulation.service;

import com.gcrf.library.circulation.dto.request.FinePaymentRequest;
import com.gcrf.library.circulation.dto.response.FineVO;
import com.gcrf.library.common.result.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 罚金服务接口
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
public interface FineService {

    /**
     * 查询逾期记录
     */
    PageResult<FineVO> queryOverdueRecords(Long readerId, Boolean paid, Integer pageNum, Integer pageSize);

    /**
     * 计算罚金
     */
    Map<String, Object> calculateFine(Long borrowId);

    /**
     * 支付罚金
     */
    FineVO payFine(FinePaymentRequest request);

    /**
     * 查询罚金记录
     */
    PageResult<FineVO> queryFines(Long readerId, Boolean paid, Integer pageNum, Integer pageSize);

    /**
     * 批量归还
     */
    Map<String, Object> batchReturn(List<Long> borrowIds);

    /**
     * 获取罚金统计
     */
    Map<String, Object> getFineStatistics(Long readerId);
}
