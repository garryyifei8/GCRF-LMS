package com.gcrf.library.notification.dto.response;

import com.gcrf.library.notification.entity.EmailLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件记录VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class EmailLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 收件人邮箱
     */
    private String recipient;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件内容
     */
    private String content;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 发送状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static EmailLogVO from(EmailLog entity) {
        if (entity == null) {
            return null;
        }
        EmailLogVO vo = new EmailLogVO();
        vo.setId(entity.getId());
        vo.setRecipient(entity.getRecipient());
        vo.setSubject(entity.getSubject());
        vo.setContent(entity.getContent());
        vo.setTemplateId(entity.getTemplateId());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setRetryCount(entity.getRetryCount());
        vo.setSentAt(entity.getSentAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
