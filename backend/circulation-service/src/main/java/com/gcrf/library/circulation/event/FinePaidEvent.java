package com.gcrf.library.circulation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚金支付事件
 * 当罚金支付完成时发布此事件
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinePaidEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    @Builder.Default
    private String eventType = "FINE_PAID";

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
     * 罚金金额
     */
    private BigDecimal fineAmount;

    /**
     * 支付方式 (CASH, CARD, ONLINE)
     */
    private String paymentMethod;

    /**
     * 支付时间
     */
    private LocalDateTime paidTime;

    /**
     * 支付备注
     */
    private String remarks;
}
