package com.gcrf.library.notification.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.TemplateCreateRequest;
import com.gcrf.library.notification.dto.request.TemplateQueryRequest;
import com.gcrf.library.notification.dto.request.TemplateUpdateRequest;
import com.gcrf.library.notification.dto.response.NotificationTemplateVO;

import java.util.Map;

/**
 * 通知模板服务接口
 */
public interface NotificationTemplateService {

    /**
     * 分页查询模板
     */
    PageResult<NotificationTemplateVO> queryTemplates(Integer pageNum, Integer pageSize, String templateType);

    /**
     * 分页查询模板（使用查询请求对象）
     */
    PageResult<NotificationTemplateVO> queryTemplates(TemplateQueryRequest request);

    /**
     * 根据ID获取模板详情
     */
    NotificationTemplateVO getTemplateById(Long templateId);

    /**
     * 根据code获取模板
     */
    NotificationTemplateVO getTemplateByCode(String templateCode);

    /**
     * 创建模板
     */
    NotificationTemplateVO createTemplate(TemplateCreateRequest request);

    /**
     * 更新模板
     */
    NotificationTemplateVO updateTemplate(Long templateId, TemplateCreateRequest request);

    /**
     * 更新模板（使用更新请求对象）
     */
    NotificationTemplateVO updateTemplate(Long templateId, TemplateUpdateRequest request);

    /**
     * 启用/禁用模板
     */
    NotificationTemplateVO changeTemplateStatus(Long templateId, Boolean enabled);

    /**
     * 删除模板(软删除)
     */
    void deleteTemplate(Long templateId);

    /**
     * 渲染模板(替换变量)
     */
    String renderTemplate(Long templateId, Map<String, Object> variables);
}
