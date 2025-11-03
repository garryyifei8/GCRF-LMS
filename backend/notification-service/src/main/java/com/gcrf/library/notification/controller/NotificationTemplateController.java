package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.TemplateCreateRequest;
import com.gcrf.library.notification.dto.request.TemplateQueryRequest;
import com.gcrf.library.notification.dto.request.TemplateUpdateRequest;
import com.gcrf.library.notification.dto.response.NotificationTemplateVO;
import com.gcrf.library.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知模板管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    /**
     * 创建通知模板
     *
     * POST /api/v1/notification-templates
     */
    @PostMapping
    public Result<NotificationTemplateVO> createTemplate(@Valid @RequestBody TemplateCreateRequest request) {
        log.info("创建通知模板: templateCode={}, templateType={}",
                request.getTemplateCode(), request.getTemplateType());
        NotificationTemplateVO template = templateService.createTemplate(request);
        return Result.success(template);
    }

    /**
     * 更新通知模板
     *
     * PUT /api/v1/notification-templates/{id}
     */
    @PutMapping("/{id}")
    public Result<NotificationTemplateVO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUpdateRequest request
    ) {
        log.info("更新通知模板: id={}", id);
        NotificationTemplateVO template = templateService.updateTemplate(id, request);
        return Result.success(template);
    }

    /**
     * 删除通知模板
     *
     * DELETE /api/v1/notification-templates/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        log.info("删除通知模板: id={}", id);
        templateService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 获取通知模板详情
     *
     * GET /api/v1/notification-templates/{id}
     */
    @GetMapping("/{id}")
    public Result<NotificationTemplateVO> getTemplateById(@PathVariable Long id) {
        log.info("获取通知模板详情: id={}", id);
        NotificationTemplateVO template = templateService.getTemplateById(id);
        return Result.success(template);
    }

    /**
     * 根据模板编码获取模板
     *
     * GET /api/v1/notification-templates/by-code/{code}
     */
    @GetMapping("/by-code/{code}")
    public Result<NotificationTemplateVO> getTemplateByCode(@PathVariable String code) {
        log.info("根据编码获取通知模板: code={}", code);
        NotificationTemplateVO template = templateService.getTemplateByCode(code);
        return Result.success(template);
    }

    /**
     * 查询通知模板列表(分页)
     *
     * GET /api/v1/notification-templates?pageNum=1&pageSize=20
     */
    @GetMapping
    public Result<PageResult<NotificationTemplateVO>> queryTemplates(@Valid TemplateQueryRequest request) {
        log.info("查询通知模板列表: pageNum={}, pageSize={}", request.getPageNum(), request.getPageSize());
        PageResult<NotificationTemplateVO> result = templateService.queryTemplates(request);
        return Result.success(result);
    }

    /**
     * 启用/禁用模板
     *
     * PUT /api/v1/notification-templates/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<NotificationTemplateVO> changeTemplateStatus(
            @PathVariable Long id,
            @RequestParam Boolean enabled
    ) {
        log.info("修改模板状态: id={}, enabled={}", id, enabled);
        NotificationTemplateVO template = templateService.changeTemplateStatus(id, enabled);
        return Result.success(template);
    }

    /**
     * 渲染模板(预览)
     *
     * POST /api/v1/notification-templates/{id}/render
     */
    @PostMapping("/{id}/render")
    public Result<String> renderTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> variables
    ) {
        log.info("渲染模板预览: id={}", id);
        String content = templateService.renderTemplate(id, variables);
        return Result.success(content);
    }
}
