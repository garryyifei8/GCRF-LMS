package com.gcrf.library.circulation.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@TableName("borrows")
public class Borrow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 借阅编号（唯一）
     */
    @TableField("borrow_id")
    @NotBlank(message = "借阅编号不能为空")
    @Size(max = 50, message = "借阅编号长度不能超过50个字符")
    private String borrowId;

    /**
     * 读者ID
     */
    @TableField("reader_id")
    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    /**
     * 图书ID
     */
    @TableField("book_id")
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 图书条码（冗余字段，便于查询）
     */
    @TableField("book_barcode")
    @Size(max = 50, message = "图书条码长度不能超过50个字符")
    private String bookBarcode;

    /**
     * 借阅日期
     */
    @TableField("borrow_date")
    @NotNull(message = "借阅日期不能为空")
    private LocalDateTime borrowDate;

    /**
     * 应还日期
     */
    @TableField("due_date")
    @NotNull(message = "应还日期不能为空")
    private LocalDateTime dueDate;

    /**
     * 实际归还日期（NULL=未归还）
     */
    @TableField("return_date")
    private LocalDateTime returnDate;

    /**
     * 续借次数
     */
    @TableField("renew_count")
    @NotNull(message = "续借次数不能为空")
    @Min(value = 0, message = "续借次数不能为负数")
    private Integer renewCount;

    /**
     * 最大续借次数
     */
    @TableField("max_renew_count")
    @NotNull(message = "最大续借次数不能为空")
    @Min(value = 0, message = "最大续借次数不能为负数")
    private Integer maxRenewCount;

    /**
     * 状态（BORROWED-借阅中, RETURNED-已归还, OVERDUE-已逾期, LOST-遗失）
     */
    @TableField("status")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(BORROWED|RETURNED|OVERDUE|LOST)$",
             message = "状态必须为BORROWED、RETURNED、OVERDUE或LOST")
    private String status;

    /**
     * 罚金金额(元)
     */
    @TableField("fine_amount")
    @NotNull(message = "罚金金额不能为空")
    @DecimalMin(value = "0.00", message = "罚金金额不能为负数")
    private BigDecimal fineAmount;

    /**
     * 是否已支付罚金
     */
    @TableField("fine_paid")
    @NotNull(message = "罚金支付状态不能为空")
    private Boolean finePaid;

    /**
     * 罚金支付日期
     */
    @TableField("fine_paid_date")
    private LocalDateTime finePaidDate;

    /**
     * 备注
     */
    @TableField("remarks")
    private String remarks;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间（软删除标记）
     * Note: 不使用@TableLogic，因为使用timestamp而非整数类型
     * 软删除逻辑由Service层手动处理：deleted_at IS NULL表示未删除
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
