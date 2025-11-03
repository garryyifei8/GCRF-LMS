package com.gcrf.library.circulation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class BorrowQueryRequest {

    /**
     * 页码 (从1开始)
     */
    private Integer pageNum = 1;

    /**
     * 每页记录数
     */
    private Integer pageSize = 20;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 借阅编号
     */
    private String borrowId;

    /**
     * 图书条码
     */
    private String bookBarcode;

    /**
     * 状态 (BORROWED, RETURNED, OVERDUE, LOST)
     */
    private String status;

    /**
     * 借阅日期开始
     */
    private LocalDateTime borrowDateStart;

    /**
     * 借阅日期结束
     */
    private LocalDateTime borrowDateEnd;

    /**
     * 应还日期开始
     */
    private LocalDateTime dueDateStart;

    /**
     * 应还日期结束
     */
    private LocalDateTime dueDateEnd;

    /**
     * 是否仅查询逾期未归还
     */
    private Boolean overdueOnly;

    /**
     * 是否仅查询未支付罚金
     */
    private Boolean unpaidFineOnly;
}
