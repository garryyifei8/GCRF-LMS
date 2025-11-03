package com.gcrf.library.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知模板实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_templates")
public class NotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码（唯一标识）
     */
    @TableField("template_code")
    private String templateCode;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 模板类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信
     */
    @TableField("template_type")
    private String templateType;

    /**
     * 模板主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 内容模板（支持变量占位符）
     */
    @TableField("content")
    private String content;

    /**
     * 变量列表（JSON字符串）
     */
    @TableField("variables")
    private String variables;

    /**
     * 模板状态: ACTIVE-启用, INACTIVE-停用
     */
    @TableField("status")
    private String status;

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
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
