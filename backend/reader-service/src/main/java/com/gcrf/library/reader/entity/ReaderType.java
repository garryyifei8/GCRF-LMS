package com.gcrf.library.reader.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 读者类型实体类
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Data
@TableName("reader_types")
public class ReaderType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型代码（唯一,如: STUDENT, TEACHER, VIP）
     */
    @TableField("type_code")
    @NotBlank(message = "类型代码不能为空")
    @Size(max = 50, message = "类型代码长度不能超过50个字符")
    private String typeCode;

    /**
     * 类型名称
     */
    @TableField("type_name")
    @NotBlank(message = "类型名称不能为空")
    @Size(max = 100, message = "类型名称长度不能超过100个字符")
    private String typeName;

    /**
     * 最大借阅数量
     */
    @TableField("max_borrow_count")
    @NotNull(message = "最大借阅数量不能为空")
    @Min(value = 1, message = "最大借阅数量至少为1")
    @Max(value = 50, message = "最大借阅数量不能超过50")
    private Integer maxBorrowCount;

    /**
     * 最长借阅天数
     */
    @TableField("max_borrow_days")
    @NotNull(message = "最长借阅天数不能为空")
    @Min(value = 1, message = "最长借阅天数至少为1天")
    @Max(value = 365, message = "最长借阅天数不能超过365天")
    private Integer maxBorrowDays;

    /**
     * 最大续借次数
     */
    @TableField("max_renew_count")
    @NotNull(message = "最大续借次数不能为空")
    @Min(value = 0, message = "最大续借次数不能为负数")
    @Max(value = 10, message = "最大续借次数不能超过10次")
    private Integer maxRenewCount;

    /**
     * 押金金额(元)
     */
    @TableField("deposit_amount")
    @Min(value = 0, message = "押金金额不能为负数")
    private Integer depositAmount;

    /**
     * 描述
     */
    @TableField("description")
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /**
     * 状态 (ACTIVE-启用, INACTIVE-禁用)
     */
    @TableField("status")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "状态必须为ACTIVE或INACTIVE")
    private String status;

    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;

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
     * 删除时间（软删除）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
