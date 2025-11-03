package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.TemplateCreateRequest;
import com.gcrf.library.notification.dto.response.NotificationTemplateVO;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateMapper templateMapper;

    @Override
    public PageResult<NotificationTemplateVO> queryTemplates(Integer pageNum, Integer pageSize, String templateType) {
        Page<NotificationTemplate> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(NotificationTemplate::getDeletedAt)
               .eq(StringUtils.hasText(templateType),
                   NotificationTemplate::getTemplateType, templateType)
               .orderByAsc(NotificationTemplate::getTemplateCode);

        Page<NotificationTemplate> result = templateMapper.selectPage(page, wrapper);

        List<NotificationTemplateVO> voList = result.getRecords().stream()
                .map(NotificationTemplateVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                voList
        );
    }

    @Override
    public NotificationTemplateVO getTemplateById(Long templateId) {
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("模板不存在, id: " + templateId);
        }

        return NotificationTemplateVO.from(template);
    }

    @Override
    public NotificationTemplateVO getTemplateByCode(String templateCode) {
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getTemplateCode, templateCode)
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("模板不存在, code: " + templateCode);
        }

        return NotificationTemplateVO.from(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationTemplateVO createTemplate(TemplateCreateRequest request) {
        // 检查模板编码是否已存在
        Long count = templateMapper.selectCount(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getTemplateCode, request.getTemplateCode())
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (count > 0) {
            throw new BusinessException("模板编码已存在: " + request.getTemplateCode());
        }

        // 创建模板
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());

        // 将变量列表转换为JSON字符串
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            template.setVariables(String.join(",", request.getVariables()));
        }
        template.setStatus(request.getStatus());

        templateMapper.insert(template);
        log.info("创建模板成功, code: {}", request.getTemplateCode());

        return NotificationTemplateVO.from(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationTemplateVO updateTemplate(Long templateId, TemplateCreateRequest request) {
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("模板不存在, id: " + templateId);
        }

        // 更新可修改字段
        if (StringUtils.hasText(request.getTemplateName())) {
            template.setTemplateName(request.getTemplateName());
        }
        if (StringUtils.hasText(request.getSubject())) {
            template.setSubject(request.getSubject());
        }
        if (StringUtils.hasText(request.getContent())) {
            template.setContent(request.getContent());
        }
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            template.setVariables(String.join(",", request.getVariables()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            template.setStatus(request.getStatus());
        }

        templateMapper.updateById(template);
        log.info("更新模板成功, id: {}", templateId);

        return NotificationTemplateVO.from(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("模板不存在, id: " + templateId);
        }

        // 软删除
        template.setDeletedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        log.info("删除模板成功, id: {}", templateId);
    }

    @Override
    public String renderTemplate(Long templateId, Map<String, Object> variables) {
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("模板不存在, id: " + templateId);
        }

        String content = template.getContent();
        if (content == null || content.isEmpty()) {
            throw new BusinessException("模板内容为空");
        }

        // 简单字符串替换实现模板渲染
        // 格式: "尊敬的{username},您的订单{orderId}已发货"
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                content = content.replace(placeholder, value);
            }
        }

        return content;
    }
}
