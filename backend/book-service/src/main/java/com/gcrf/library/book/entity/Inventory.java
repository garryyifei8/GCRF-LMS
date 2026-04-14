package com.gcrf.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@TableName("inventory")
public class Inventory {

    /**
     * 库存ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 存放位置
     */
    private String location;

    /**
     * 书架号
     */
    @TableField("shelf_number")
    private String shelfNumber;

    /**
     * 馆藏总数
     */
    @TableField("total_quantity")
    private Integer totalQuantity;

    /**
     * 可借数量
     */
    @TableField("available_quantity")
    private Integer availableQuantity;

    /**
     * 已借出数量
     */
    @TableField("borrowed_quantity")
    private Integer borrowedQuantity;

    /**
     * 预约数量
     */
    @TableField("reserved_quantity")
    private Integer reservedQuantity;

    /**
     * 预警阈值
     */
    @TableField("alert_threshold")
    private Integer alertThreshold;

    /**
     * 最后盘点时间
     */
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;

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
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;

    /**
     * 是否需要预警
     */
    public boolean isAlertRequired() {
        return availableQuantity != null && alertThreshold != null
               && availableQuantity <= alertThreshold;
    }
}
