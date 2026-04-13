package com.gcrf.library.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流通记录DTO - 用于服务间调用
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CirculationRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 图书ID
     */
    private Long bookId;

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
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 状态：BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期
     */
    private String status;

    /**
     * 罚款金额
     */
    private BigDecimal fineAmount;

    /**
     * 罚款状态：UNPAID-未支付, PAID-已支付
     */
    private String fineStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
