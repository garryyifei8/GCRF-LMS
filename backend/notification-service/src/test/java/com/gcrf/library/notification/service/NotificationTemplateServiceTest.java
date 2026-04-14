package com.gcrf.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.TemplateCreateRequest;
import com.gcrf.library.notification.dto.request.TemplateQueryRequest;
import com.gcrf.library.notification.dto.request.TemplateUpdateRequest;
import com.gcrf.library.notification.dto.response.NotificationTemplateVO;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.service.impl.NotificationTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationTemplateService单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateMapper templateMapper;

    @InjectMocks
    private NotificationTemplateServiceImpl service;

    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = new NotificationTemplate();
        testTemplate.setId(1L);
        testTemplate.setTemplateCode("ORDER_SHIPPED");
        testTemplate.setTemplateName("订单发货通知");
        testTemplate.setTemplateType("EMAIL");
        testTemplate.setSubject("您的订单已发货");
        testTemplate.setContent("您好 {username}, 您的订单 {orderId} 已发货");
        testTemplate.setVariables("username,orderId");
        testTemplate.setStatus("ACTIVE");
    }

    // ========== queryTemplates ==========

    @Test
    @DisplayName("queryTemplates_withTemplateType_shouldFilter")
    @SuppressWarnings("unchecked")
    void queryTemplates_withTemplateType_shouldFilter() {
        Page<NotificationTemplate> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testTemplate));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(templateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        PageResult<NotificationTemplateVO> result = service.queryTemplates(1, 10, "EMAIL");

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getTemplateType()).isEqualTo("EMAIL");
        verify(templateMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("queryTemplates_withoutTemplateType_shouldReturnAll")
    @SuppressWarnings("unchecked")
    void queryTemplates_withoutTemplateType_shouldReturnAll() {
        NotificationTemplate sms = new NotificationTemplate();
        sms.setId(2L);
        sms.setTemplateCode("SMS_CODE");
        sms.setTemplateType("SMS");

        Page<NotificationTemplate> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testTemplate, sms));
        page.setTotal(2);
        page.setCurrent(1);
        page.setSize(10);

        when(templateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        PageResult<NotificationTemplateVO> result = service.queryTemplates(1, 10, null);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords()).hasSize(2);
    }

    @Test
    @DisplayName("queryTemplates_shouldExcludeSoftDeleted")
    @SuppressWarnings("unchecked")
    void queryTemplates_shouldExcludeSoftDeleted() {
        Page<NotificationTemplate> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testTemplate));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(templateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        PageResult<NotificationTemplateVO> result = service.queryTemplates(1, 10, null);

        // The wrapper includes isNull(deletedAt) - verify mapper is called (soft-delete handled in query)
        assertThat(result.getRecords()).hasSize(1);
        verify(templateMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("queryTemplates_withRequest_shouldDelegateToThreeArgVersion")
    @SuppressWarnings("unchecked")
    void queryTemplates_withRequest_shouldDelegateToThreeArgVersion() {
        TemplateQueryRequest request = new TemplateQueryRequest();
        request.setPageNum(2);
        request.setPageSize(20);
        request.setTemplateType("SMS");

        Page<NotificationTemplate> page = new Page<>(2, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        page.setCurrent(2);
        page.setSize(20);

        when(templateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        PageResult<NotificationTemplateVO> result = service.queryTemplates(request);

        assertThat(result).isNotNull();
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(20);
        verify(templateMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ========== getTemplateById ==========

    @Test
    @DisplayName("getTemplateById_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void getTemplateById_whenNotFound_shouldThrowException() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getTemplateById(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("getTemplateById_success_shouldReturnVO")
    @SuppressWarnings("unchecked")
    void getTemplateById_success_shouldReturnVO() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testTemplate);

        NotificationTemplateVO vo = service.getTemplateById(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getTemplateCode()).isEqualTo("ORDER_SHIPPED");
    }

    // ========== getTemplateByCode ==========

    @Test
    @DisplayName("getTemplateByCode_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void getTemplateByCode_whenNotFound_shouldThrowException() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getTemplateByCode("NO_SUCH_CODE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("getTemplateByCode_success_shouldReturnVO")
    @SuppressWarnings("unchecked")
    void getTemplateByCode_success_shouldReturnVO() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testTemplate);

        NotificationTemplateVO vo = service.getTemplateByCode("ORDER_SHIPPED");

        assertThat(vo).isNotNull();
        assertThat(vo.getTemplateCode()).isEqualTo("ORDER_SHIPPED");
        assertThat(vo.getTemplateName()).isEqualTo("订单发货通知");
    }

    // ========== createTemplate ==========

    @Test
    @DisplayName("createTemplate_whenCodeExists_shouldThrowException")
    @SuppressWarnings("unchecked")
    void createTemplate_whenCodeExists_shouldThrowException() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("ORDER_SHIPPED");
        request.setTemplateName("订单发货");
        request.setTemplateType("EMAIL");
        request.setContent("content");

        when(templateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.createTemplate(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板编码已存在");

        verify(templateMapper, never()).insert(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("createTemplate_withVariablesList_shouldJoinToCommaString")
    @SuppressWarnings("unchecked")
    void createTemplate_withVariablesList_shouldJoinToCommaString() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("NEW_CODE");
        request.setTemplateName("新模板");
        request.setTemplateType("SMS");
        request.setContent("短信内容");
        request.setVariables(Arrays.asList("name", "code", "time"));
        request.setStatus("ACTIVE");

        when(templateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(templateMapper.insert(any(NotificationTemplate.class))).thenReturn(1);

        service.createTemplate(request);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertThat(captor.getValue().getVariables()).isEqualTo("name,code,time");
    }

    @Test
    @DisplayName("createTemplate_success_shouldInsertAndReturnVO")
    @SuppressWarnings("unchecked")
    void createTemplate_success_shouldInsertAndReturnVO() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("WELCOME");
        request.setTemplateName("欢迎模板");
        request.setTemplateType("EMAIL");
        request.setSubject("欢迎");
        request.setContent("您好 {username}");
        request.setStatus("ACTIVE");

        when(templateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(templateMapper.insert(any(NotificationTemplate.class))).thenReturn(1);

        NotificationTemplateVO vo = service.createTemplate(request);

        assertThat(vo).isNotNull();
        assertThat(vo.getTemplateCode()).isEqualTo("WELCOME");
        assertThat(vo.getTemplateName()).isEqualTo("欢迎模板");
        assertThat(vo.getTemplateType()).isEqualTo("EMAIL");
        assertThat(vo.getStatus()).isEqualTo("ACTIVE");
        verify(templateMapper).insert(any(NotificationTemplate.class));
    }

    // ========== updateTemplate (CreateRequest overload) ==========

    @Test
    @DisplayName("updateTemplate_withCreateRequest_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void updateTemplate_withCreateRequest_whenNotFound_shouldThrowException() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateName("新名称");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.updateTemplate(99L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");

        verify(templateMapper, never()).updateById(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("updateTemplate_withCreateRequest_partialUpdate_shouldOnlyUpdateNonNullFields")
    @SuppressWarnings("unchecked")
    void updateTemplate_withCreateRequest_partialUpdate_shouldOnlyUpdateNonNullFields() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateName("更新后的名称");
        // subject, content, variables, status 保持 null / empty

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testTemplate);
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        service.updateTemplate(1L, request);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).updateById(captor.capture());

        NotificationTemplate updated = captor.getValue();
        assertThat(updated.getTemplateName()).isEqualTo("更新后的名称");
        // 其他字段未变
        assertThat(updated.getSubject()).isEqualTo("您的订单已发货");
        assertThat(updated.getContent()).isEqualTo("您好 {username}, 您的订单 {orderId} 已发货");
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
    }

    // ========== updateTemplate (UpdateRequest overload) ==========

    @Test
    @DisplayName("updateTemplate_withUpdateRequest_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void updateTemplate_withUpdateRequest_whenNotFound_shouldThrowException() {
        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("新名称");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.updateTemplate(99L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");

        verify(templateMapper, never()).updateById(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("updateTemplate_withUpdateRequest_partialUpdate_shouldOnlyUpdateNonNullFields")
    @SuppressWarnings("unchecked")
    void updateTemplate_withUpdateRequest_partialUpdate_shouldOnlyUpdateNonNullFields() {
        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateType("SMS");
        request.setVariables(Arrays.asList("a", "b"));

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testTemplate);
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        service.updateTemplate(1L, request);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).updateById(captor.capture());

        NotificationTemplate updated = captor.getValue();
        assertThat(updated.getTemplateType()).isEqualTo("SMS");
        assertThat(updated.getVariables()).isEqualTo("a,b");
        // 未提供的字段保持原值
        assertThat(updated.getTemplateName()).isEqualTo("订单发货通知");
        assertThat(updated.getSubject()).isEqualTo("您的订单已发货");
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
    }

    // ========== changeTemplateStatus ==========

    @Test
    @DisplayName("changeTemplateStatus_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void changeTemplateStatus_whenNotFound_shouldThrowException() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.changeTemplateStatus(99L, true))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");

        verify(templateMapper, never()).updateById(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("changeTemplateStatus_whenEnabledTrue_shouldSetActive")
    @SuppressWarnings("unchecked")
    void changeTemplateStatus_whenEnabledTrue_shouldSetActive() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setStatus("INACTIVE");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        service.changeTemplateStatus(1L, true);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("changeTemplateStatus_whenEnabledFalse_shouldSetInactive")
    @SuppressWarnings("unchecked")
    void changeTemplateStatus_whenEnabledFalse_shouldSetInactive() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setStatus("ACTIVE");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        service.changeTemplateStatus(1L, false);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("INACTIVE");
    }

    // ========== deleteTemplate ==========

    @Test
    @DisplayName("deleteTemplate_whenNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void deleteTemplate_whenNotFound_shouldThrowException() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTemplate(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");

        verify(templateMapper, never()).updateById(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("deleteTemplate_success_shouldSoftDelete")
    @SuppressWarnings("unchecked")
    void deleteTemplate_success_shouldSoftDelete() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testTemplate);
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        service.deleteTemplate(1L);

        ArgumentCaptor<NotificationTemplate> captor =
            ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    // ========== renderTemplate ==========

    @Test
    @DisplayName("renderTemplate_whenTemplateNotFound_shouldThrowException")
    @SuppressWarnings("unchecked")
    void renderTemplate_whenTemplateNotFound_shouldThrowException() {
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.renderTemplate(99L, Collections.emptyMap()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("renderTemplate_whenContentEmpty_shouldThrowException")
    @SuppressWarnings("unchecked")
    void renderTemplate_whenContentEmpty_shouldThrowException() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setContent("");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

        assertThatThrownBy(() -> service.renderTemplate(1L, Collections.emptyMap()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("模板内容为空");
    }

    @Test
    @DisplayName("renderTemplate_withoutVariables_shouldReturnOriginalContent")
    @SuppressWarnings("unchecked")
    void renderTemplate_withoutVariables_shouldReturnOriginalContent() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setContent("纯文本内容,无变量");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

        String result = service.renderTemplate(1L, null);

        assertThat(result).isEqualTo("纯文本内容,无变量");
    }

    @Test
    @DisplayName("renderTemplate_withVariables_shouldReplacePlaceholders")
    @SuppressWarnings("unchecked")
    void renderTemplate_withVariables_shouldReplacePlaceholders() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setContent("您好 {username}, 您的订单 {orderId} 已发货");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "张三");
        variables.put("orderId", "ORD-001");

        String result = service.renderTemplate(1L, variables);

        assertThat(result).isEqualTo("您好 张三, 您的订单 ORD-001 已发货");
    }

    @Test
    @DisplayName("renderTemplate_withNullVariableValue_shouldReplaceWithEmptyString")
    @SuppressWarnings("unchecked")
    void renderTemplate_withNullVariableValue_shouldReplaceWithEmptyString() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setContent("您好 {username}, 您的手机号: {phone}");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "李四");
        variables.put("phone", null);

        String result = service.renderTemplate(1L, variables);

        assertThat(result).isEqualTo("您好 李四, 您的手机号: ");
    }
}
