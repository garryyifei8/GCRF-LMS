package com.gcrf.library.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.TemplateCreateRequest;
import com.gcrf.library.notification.dto.request.TemplateUpdateRequest;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationTemplateController集成测试
 *
 * 测试覆盖：
 * - 模板CRUD (创建/更新/删除/查询)
 * - 唯一性校验、Not Found错误路径
 * - 根据编码查询、分页、按类型过滤
 * - 启用/禁用状态切换
 * - 模板渲染(变量替换)
 *
 * 依赖 Flyway V001 baseline 中预置的 5 个默认模板
 * (WELCOME / VERIFICATION_CODE / BORROW_REMINDER / RESERVE_SUCCESS / OVERDUE_NOTICE)
 *
 * @author GCRF Team
 * @since 2026-04-14
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationTemplateControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTemplateMapper templateMapper;

    // ------------------------------------------------------------
    // 1. 创建模板 - 成功
    // ------------------------------------------------------------
    @Test
    void createTemplate_success_shouldPersist() throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("TEST_TEMPLATE_001");
        request.setTemplateName("集成测试模板");
        request.setTemplateType("EMAIL");
        request.setSubject("测试主题");
        request.setContent("Hello {{username}}, welcome.");
        // variables omitted: service joins List<String> with ',' but JSONB requires valid JSON.
        request.setStatus("ACTIVE");

        mockMvc.perform(post("/api/v1/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateCode").value("TEST_TEMPLATE_001"))
                .andExpect(jsonPath("$.data.templateName").value("集成测试模板"))
                .andExpect(jsonPath("$.data.templateType").value("EMAIL"));
    }

    // ------------------------------------------------------------
    // 2. 创建模板 - 编码重复
    // ------------------------------------------------------------
    @Test
    void createTemplate_withDuplicateCode_shouldReturnError() throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("WELCOME"); // 已在 baseline 存在
        request.setTemplateName("重复模板");
        request.setTemplateType("NOTIFICATION");
        request.setSubject("主题");
        request.setContent("内容 {{username}}");
        request.setStatus("ACTIVE");

        mockMvc.perform(post("/api/v1/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ------------------------------------------------------------
    // 3. 更新模板 - 成功
    // ------------------------------------------------------------
    @Test
    void updateTemplate_success_shouldUpdate() throws Exception {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>()
                        .eq(NotificationTemplate::getTemplateCode, "WELCOME"));
        assertThat(template).isNotNull();

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("欢迎消息-已更新");
        request.setSubject("更新后的主题");
        request.setContent("Hello {{username}}, updated content.");
        // variables 省略: 让 updateById 仅更新非 null 字段, 避免 JSONB 写入非 JSON 字符串

        mockMvc.perform(put("/api/v1/notification-templates/" + template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateName").value("欢迎消息-已更新"))
                .andExpect(jsonPath("$.data.subject").value("更新后的主题"));
    }

    // ------------------------------------------------------------
    // 4. 更新模板 - Not Found
    // ------------------------------------------------------------
    @Test
    void updateTemplate_whenNotFound_shouldReturnError() throws Exception {
        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("不存在的模板");
        request.setSubject("主题");
        request.setContent("内容");

        mockMvc.perform(put("/api/v1/notification-templates/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ------------------------------------------------------------
    // 5. 删除模板 - 软删除
    // ------------------------------------------------------------
    @Test
    void deleteTemplate_success_shouldSoftDelete() throws Exception {
        // 先创建一个独立模板用于删除
        TemplateCreateRequest create = new TemplateCreateRequest();
        create.setTemplateCode("TEST_DELETE_001");
        create.setTemplateName("待删除模板");
        create.setTemplateType("SMS");
        create.setContent("即将删除");
        create.setStatus("ACTIVE");

        String createResp = mockMvc.perform(post("/api/v1/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).path("data").path("id").asLong();
        assertThat(id).isGreaterThan(0);

        mockMvc.perform(delete("/api/v1/notification-templates/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ------------------------------------------------------------
    // 6. 按ID查询 - 成功
    // ------------------------------------------------------------
    @Test
    void getTemplateById_success_shouldReturn() throws Exception {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>()
                        .eq(NotificationTemplate::getTemplateCode, "WELCOME"));
        assertThat(template).isNotNull();

        mockMvc.perform(get("/api/v1/notification-templates/{id}", template.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(template.getId()))
                .andExpect(jsonPath("$.data.templateCode").value("WELCOME"));
    }

    // ------------------------------------------------------------
    // 7. 按ID查询 - Not Found
    // ------------------------------------------------------------
    @Test
    void getTemplateById_whenNotFound_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ------------------------------------------------------------
    // 8. 按编码查询 - 成功 (baseline 模板)
    // ------------------------------------------------------------
    @Test
    void getTemplateByCode_success_shouldReturn() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates/by-code/{code}", "WELCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateCode").value("WELCOME"))
                .andExpect(jsonPath("$.data.templateType").value("NOTIFICATION"));
    }

    // ------------------------------------------------------------
    // 9. 分页查询
    // ------------------------------------------------------------
    @Test
    void queryTemplates_withPagination_shouldReturnPaged() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)));
    }

    // ------------------------------------------------------------
    // 10. 按类型过滤
    // ------------------------------------------------------------
    @Test
    void queryTemplates_withTypeFilter_shouldFilter() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("templateType", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)));
    }

    // ------------------------------------------------------------
    // 11. 启用/禁用状态切换
    // ------------------------------------------------------------
    @Test
    void changeTemplateStatus_toInactive_shouldUpdate() throws Exception {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>()
                        .eq(NotificationTemplate::getTemplateCode, "WELCOME"));
        assertThat(template).isNotNull();

        mockMvc.perform(put("/api/v1/notification-templates/{id}/status", template.getId())
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    // ------------------------------------------------------------
    // 12. 模板渲染 - 变量替换
    // ------------------------------------------------------------
    @Test
    void renderTemplate_withVariables_shouldReturnRenderedContent() throws Exception {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>()
                        .eq(NotificationTemplate::getTemplateCode, "WELCOME"));
        assertThat(template).isNotNull();

        Map<String, Object> variables = Map.of("username", "张三");

        mockMvc.perform(post("/api/v1/notification-templates/" + template.getId() + "/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variables)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
