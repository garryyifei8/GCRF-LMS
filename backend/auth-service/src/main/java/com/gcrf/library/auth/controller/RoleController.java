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
        java.util.Map<Long, Integer> permCounts = roleService.permissionCountsByRole();
        java.util.Map<Long, Integer> userCounts = roleService.userCountsByRole();
        return Result.success(roleService.listSystemRoles().stream()
            .map(r -> toVO(r, permCounts, userCounts)).toList());
    }

    @GetMapping("/{id}")
    public Result<RoleDetailVO> detail(@PathVariable Long id) {
        Role r = roleService.getById(id);
        var perms = permissionService.listForRole(id);
        RoleVO vo = toVO(r, java.util.Map.of(), java.util.Map.of());
        vo.setPermissionCount(perms.size());
        return Result.success(RoleDetailVO.builder()
            .role(vo)
            .permissions(perms)
            .build());
    }

    private RoleVO toVO(Role r,
                        java.util.Map<Long, Integer> permCounts,
                        java.util.Map<Long, Integer> userCounts) {
        return RoleVO.builder()
            .id(r.getId())
            .code(r.getCode())
            .name(r.getName())
            .description(r.getDescription())
            .scopeDefault(r.getScopeDefault())
            .isSystem(r.getIsSystem())
            .permissionCount(permCounts.getOrDefault(r.getId(), 0))
            .userCount(userCounts.getOrDefault(r.getId(), 0))
            .build();
    }
}
