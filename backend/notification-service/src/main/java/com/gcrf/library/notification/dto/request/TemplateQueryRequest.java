package com.gcrf.library.notification.dto.request;

import lombok.Data;

/**
 * 查询通知模板请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class TemplateQueryRequest {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 模板类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信
     */
    private String templateType;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 模板状态: ACTIVE-启用, INACTIVE-停用
     */
    private String status;
}
