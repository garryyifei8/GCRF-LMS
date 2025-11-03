package com.gcrf.library.circulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录详情响应VO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowDetailVO {

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
     * 读者证号
     */
    private String readerCardId;

    /**
     * 读者类型
     */
    private String readerType;

    /**
     * 读者联系电话 (脱敏)
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
     * 图书ISBN
     */
    private String bookIsbn;

    /**
     * 图书条码
     */
    private String bookBarcode;

    /**
     * 图书作者
     */
    private String bookAuthor;

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
     * 罚金支付日期
     */
    private LocalDateTime finePaidDate;

    /**
     * 是否逾期
     */
    private Boolean overdue;

    /**
     * 逾期天数
     */
    private Long overdueDays;

    /**
     * 是否可以续借
     */
    private Boolean canRenew;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
