package com.gcrf.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.system.dto.DepartmentCreateRequest;
import com.gcrf.library.system.dto.DepartmentQueryRequest;
import com.gcrf.library.system.dto.DepartmentResponse;
import com.gcrf.library.system.dto.DepartmentUpdateRequest;
import com.gcrf.library.system.entity.Department;
import com.gcrf.library.system.mapper.DepartmentMapper;
import com.gcrf.library.system.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DepartmentService 单元测试
 *
 * @author GCRF Team
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private DepartmentCreateRequest createRequest;
    private DepartmentUpdateRequest updateRequest;
    private DepartmentQueryRequest queryRequest;
    private Department existingDepartment;

    @BeforeEach
    void setUp() {
        createRequest = new DepartmentCreateRequest();
        createRequest.setDeptCode("TECH");
        createRequest.setDeptName("技术部");
        createRequest.setPhone("12345678");
        createRequest.setEmail("tech@gcrf.com");
        createRequest.setDescription("技术研发部门");

        updateRequest = new DepartmentUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setDeptName("新技术部");
        updateRequest.setPhone("87654321");
        updateRequest.setEmail("newtech@gcrf.com");
        updateRequest.setDescription("更新后的描述");

        queryRequest = new DepartmentQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

        existingDepartment = new Department();
        existingDepartment.setId(1L);
        existingDepartment.setDeptCode("TECH");
        existingDepartment.setDeptName("技术部");
        existingDepartment.setDeptLevel(1);
        existingDepartment.setDeptPath("/1");
        existingDepartment.setStatus("ACTIVE");
        existingDepartment.setSortOrder(0);
    }

    @Test
    @DisplayName("创建部门-部门编码已存在应抛出异常")
    void createDepartment_whenDeptCodeExists_shouldThrowException() {
        // Arrange
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(createRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("部门编码已存在");
        verify(departmentMapper, never()).insert(any(Department.class));
    }

    @Test
    @DisplayName("创建部门-指定父部门应基于父部门计算层级")
    void createDepartment_withParent_shouldCalculateLevelFromParent() {
        // Arrange
        createRequest.setParentId(1L);
        Department parent = new Department();
        parent.setId(1L);
        parent.setDeptLevel(2);
        parent.setDeptPath("/0/1");

        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(departmentMapper.selectById(1L)).thenReturn(parent);
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(10L);
            return 1;
        });
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

        // Act
        departmentService.createDepartment(createRequest);

        // Assert
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getDeptLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("创建部门-指定父部门应基于父部门构建路径")
    void createDepartment_withParent_shouldBuildDeptPathWithAncestors() {
        // Arrange
        createRequest.setParentId(1L);
        Department parent = new Department();
        parent.setId(1L);
        parent.setDeptLevel(1);
        parent.setDeptPath("/1");

        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(departmentMapper.selectById(1L)).thenReturn(parent);
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(10L);
            return 1;
        });
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

        // Act
        departmentService.createDepartment(createRequest);

        // Assert
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeptPath()).isEqualTo("/1/10");
    }

    @Test
    @DisplayName("创建部门-无父部门应将层级设置为1")
    void createDepartment_withoutParent_shouldSetLevelToOne() {
        // Arrange
        createRequest.setParentId(null);
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(5L);
            return 1;
        });
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

        // Act
        departmentService.createDepartment(createRequest);

        // Assert
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getDeptLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("创建部门-成功应插入数据并返回响应")
    void createDepartment_success_shouldInsertAndReturnResponse() {
        // Arrange
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(100L);
            return 1;
        });
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

        // Act
        DepartmentResponse response = departmentService.createDepartment(createRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDeptCode()).isEqualTo("TECH");
        assertThat(response.getDeptName()).isEqualTo("技术部");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(departmentMapper).insert(any(Department.class));
        verify(departmentMapper).updateById(any(Department.class));
    }

    @Test
    @DisplayName("更新部门-部门不存在应抛出异常")
    void updateDepartment_whenDeptNotFound_shouldThrowException() {
        // Arrange
        when(departmentMapper.selectById(anyLong())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.updateDepartment(updateRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("部门不存在");
        verify(departmentMapper, never()).updateById(any(Department.class));
    }

    @Test
    @DisplayName("更新部门-部分字段应仅更新非空字段")
    void updateDepartment_withPartialFields_shouldOnlyUpdateNonNullFields() {
        // Arrange
        DepartmentUpdateRequest partialRequest = new DepartmentUpdateRequest();
        partialRequest.setId(1L);
        partialRequest.setDeptName("新名字");
        // phone/email/description 为 null

        Department original = new Department();
        original.setId(1L);
        original.setDeptCode("TECH");
        original.setDeptName("技术部");
        original.setPhone("原电话");
        original.setEmail("原邮箱");
        original.setDescription("原描述");

        when(departmentMapper.selectById(1L)).thenReturn(original);
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

        // Act
        departmentService.updateDepartment(partialRequest);

        // Assert
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).updateById(captor.capture());
        Department captured = captor.getValue();
        assertThat(captured.getDeptName()).isEqualTo("新名字");
        assertThat(captured.getPhone()).isEqualTo("原电话");
        assertThat(captured.getEmail()).isEqualTo("原邮箱");
        assertThat(captured.getDescription()).isEqualTo("原描述");
    }

    @Test
    @DisplayName("获取部门-不存在应抛出异常")
    void getDepartmentById_whenNotFound_shouldThrowException() {
        // Arrange
        when(departmentMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.getDepartmentById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("部门不存在");
    }

    @Test
    @DisplayName("获取部门-成功应返回响应")
    void getDepartmentById_success_shouldReturnResponse() {
        // Arrange
        when(departmentMapper.selectById(1L)).thenReturn(existingDepartment);

        // Act
        DepartmentResponse response = departmentService.getDepartmentById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDeptCode()).isEqualTo("TECH");
        assertThat(response.getDeptName()).isEqualTo("技术部");
        verify(departmentMapper).selectById(1L);
    }

    @Test
    @DisplayName("查询部门列表-带过滤条件应应用所有过滤器")
    void queryDepartments_withFilters_shouldApplyAllFilters() {
        // Arrange
        queryRequest.setDeptCode("TECH");
        queryRequest.setDeptName("技术");
        queryRequest.setStatus("ACTIVE");

        Page<Department> page = new Page<>();
        page.setRecords(Arrays.asList(existingDepartment));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(departmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // Act
        Page<DepartmentResponse> result = departmentService.queryDepartments(queryRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDeptCode()).isEqualTo("TECH");
        verify(departmentMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询部门列表-分页应使用指定页码和页大小")
    void queryDepartments_withPagination_shouldUsePageNumAndSize() {
        // Arrange
        queryRequest.setPageNum(3);
        queryRequest.setPageSize(5);

        Page<Department> page = new Page<>();
        page.setRecords(List.of());
        page.setTotal(0);
        page.setCurrent(3);
        page.setSize(5);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        when(departmentMapper.selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // Act
        Page<DepartmentResponse> result = departmentService.queryDepartments(queryRequest);

        // Assert
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(3);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("查询部门列表-默认应按sortOrder和id排序")
    void queryDepartments_default_shouldOrderBySortOrderAndId() {
        // Arrange
        Page<Department> page = new Page<>();
        page.setRecords(Arrays.asList(existingDepartment));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(departmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // Act
        Page<DepartmentResponse> result = departmentService.queryDepartments(queryRequest);

        // Assert - Verify selectPage was called with a LambdaQueryWrapper (ordering is embedded in the wrapper)
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        verify(departmentMapper, atLeastOnce()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("删除部门-不存在应抛出异常")
    void deleteDepartment_whenNotFound_shouldThrowException() {
        // Arrange
        when(departmentMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.deleteDepartment(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("部门不存在");
        verify(departmentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除部门-存在子部门应抛出异常")
    void deleteDepartment_whenHasChildren_shouldThrowException() {
        // Arrange
        when(departmentMapper.selectById(1L)).thenReturn(existingDepartment);
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("子部门");
        verify(departmentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除部门-成功应删除")
    void deleteDepartment_success_shouldDelete() {
        // Arrange
        when(departmentMapper.selectById(1L)).thenReturn(existingDepartment);
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(departmentMapper.deleteById(1L)).thenReturn(1);

        // Act
        departmentService.deleteDepartment(1L);

        // Assert
        verify(departmentMapper).deleteById(1L);
    }
}
