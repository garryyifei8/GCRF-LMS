package com.gcrf.library.circulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚金视图对象
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineVO {

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
     * 应还日期
     */
    private LocalDateTime dueDate;

    /**
     * 实际归还日期
     */
    private LocalDateTime returnDate;

    /**
     * 逾期天数
     */
    private Integer overdueDays;

    /**
     * 罚金金额(元)
     */
    private BigDecimal fineAmount;

    /**
     * 是否已支付
     */
    private Boolean finePaid;

    /**
     * 支付日期
     */
    private LocalDateTime finePaidDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
