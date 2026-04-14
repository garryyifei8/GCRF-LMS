package com.gcrf.library.circulation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 归还完成事件
 * 当图书归还完成时发布此事件，记录到系统日志
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    @Builder.Default
    private String eventType = "RETURN_COMPLETED";

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 借阅记录ID
     */
    private Long borrowId;

    /**
     * 借阅编号
     */
    private String borrowIdStr;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 读者姓名
     */
    private String readerName;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 图书条码
     */
    private String bookBarcode;

    /**
     * 借阅日期
     */
    private LocalDateTime borrowDate;

    /**
     * 应还日期
     */
    private LocalDateTime dueDate;

    /**
     * 实际归还日期
     */
    private LocalDateTime returnDate;

    /**
     * 是否逾期
     */
    private Boolean isOverdue;

    /**
     * 逾期天数（如果逾期）
     */
    private Integer overdueDays;

    /**
     * 罚金金额
     */
    private BigDecimal fineAmount;

    /**
     * 罚金是否已支付
     */
    private Boolean finePaid;

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员姓名
     */
    private String operatorName;
}
