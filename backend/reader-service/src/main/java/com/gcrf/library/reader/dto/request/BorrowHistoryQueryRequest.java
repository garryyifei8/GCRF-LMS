package com.gcrf.library.reader.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 借阅历史查询请求
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class BorrowHistoryQueryRequest {

    /**
     * 状态过滤 (BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期, LOST-遗失)
     */
    private String status;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer pageSize = 10;
}
