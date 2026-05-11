package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.RoleDetailVO;
import com.gcrf.library.auth.dto.RoleVO;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.service.PermissionService;
import com.gcrf.library.auth.service.RoleService;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listSystemRoles().stream()
            .map(this::toVO).toList());
    }

    @GetMapping("/{id}")
    public Result<RoleDetailVO> detail(@PathVariable Long id) {
        Role r = roleService.getById(id);
        return Result.success(RoleDetailVO.builder()
            .role(toVO(r))
            .permissions(permissionService.listForRole(id))
            .build());
    }

    private RoleVO toVO(Role r) {
        return RoleVO.builder()
            .id(r.getId())
            .code(r.getCode())
            .name(r.getName())
            .description(r.getDescription())
            .scopeDefault(r.getScopeDefault())
            .isSystem(r.getIsSystem())
            .build();
    }
}
