package com.gcrf.library.circulation.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约记录实体类
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@TableName("reserves")
public class Reserve implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预约编号（唯一）
     */
    @TableField("reserve_id")
    @NotBlank(message = "预约编号不能为空")
    @Size(max = 50, message = "预约编号长度不能超过50个字符")
    private String reserveId;

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
     * 预约日期
     */
    @TableField("reserve_date")
    @NotNull(message = "预约日期不能为空")
    private LocalDateTime reserveDate;

    /**
     * 预约过期日期
     */
    @TableField("expiry_date")
    @NotNull(message = "预约过期日期不能为空")
    private LocalDateTime expiryDate;

    /**
     * 取书日期（NULL=未取书）
     */
    @TableField("pickup_date")
    private LocalDateTime pickupDate;

    /**
     * 取消日期（NULL=未取消）
     */
    @TableField("cancel_date")
    private LocalDateTime cancelDate;

    /**
     * 状态（RESERVED-已预约, PICKED_UP-已取书, CANCELLED-已取消, EXPIRED-已过期）
     */
    @TableField("status")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(RESERVED|PICKED_UP|CANCELLED|EXPIRED)$",
             message = "状态必须为RESERVED、PICKED_UP、CANCELLED或EXPIRED")
    private String status;

    /**
     * 是否已发送通知
     */
    @TableField("notify_sent")
    @NotNull(message = "通知发送状态不能为空")
    private Boolean notifySent;

    /**
     * 通知发送日期
     */
    @TableField("notify_sent_date")
    private LocalDateTime notifySentDate;

    /**
     * 通知发送次数
     */
    @TableField("notify_count")
    @NotNull(message = "通知发送次数不能为空")
    private Integer notifyCount;

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
