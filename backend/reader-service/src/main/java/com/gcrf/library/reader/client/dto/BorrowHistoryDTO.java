package com.gcrf.library.reader.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅历史DTO（从Circulation Service获取）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowHistoryDTO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 借阅编号
     */
    private String borrowId;

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
     * 实际归还日期 (NULL=未归还)
     */
    private LocalDateTime returnDate;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 最大续借次数
     */
    private Integer maxRenewCount;

    /**
     * 状态 (BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期, LOST-遗失)
     */
    private String status;

    /**
     * 罚金金额(元)
     */
    private BigDecimal fineAmount;

    /**
     * 是否已支付罚金
     */
    private Boolean finePaid;

    /**
     * 是否逾期
     */
    private Boolean overdue;

    /**
     * 逾期天数
     */
    private Long overdueDays;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
