package com.gcrf.library.notification.dto.response;

import com.gcrf.library.notification.entity.NotificationTemplate;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知模板VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationTemplateVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模板编码（唯一标识）
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信
     */
    private String templateType;

    /**
     * 模板主题
     */
    private String subject;

    /**
     * 内容模板（支持变量占位符）
     */
    private String content;

    /**
     * 变量列表（JSON字符串）
     */
    private String variables;

    /**
     * 模板状态: ACTIVE-启用, INACTIVE-停用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static NotificationTemplateVO from(NotificationTemplate entity) {
        if (entity == null) {
            return null;
        }
        NotificationTemplateVO vo = new NotificationTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setTemplateType(entity.getTemplateType());
        vo.setSubject(entity.getSubject());
        vo.setContent(entity.getContent());
        vo.setVariables(entity.getVariables());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
