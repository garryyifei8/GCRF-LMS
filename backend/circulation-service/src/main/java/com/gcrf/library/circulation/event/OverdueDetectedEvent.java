package com.gcrf.library.circulation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 逾期检测事件
 * 当检测到借阅逾期时发布此事件，触发邮件通知
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdueDetectedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    @Builder.Default
    private String eventType = "OVERDUE_DETECTED";

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
     * 读者邮箱
     */
    private String readerEmail;

    /**
     * 读者电话
     */
    private String readerPhone;

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
     * 应还日期
     */
    private LocalDateTime dueDate;

    /**
     * 逾期天数
     */
    private Integer overdueDays;

    /**
     * 当前罚金金额
     */
    private BigDecimal currentFineAmount;

    /**
     * 通知类型 (EMAIL, SMS, BOTH)
     */
    @Builder.Default
    private String notificationType = "EMAIL";
}
