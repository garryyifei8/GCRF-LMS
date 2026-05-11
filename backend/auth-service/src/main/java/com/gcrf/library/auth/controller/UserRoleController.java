package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.AssignRoleRequest;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.service.RoleService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<List<Role>> roles(@PathVariable Long userId) {
        return Result.success(roleService.rolesOfUser(userId));
    }

    @PostMapping
    @RequireRole({"REGION_ADMIN"})
    public Result<Void> assign(@PathVariable Long userId, @Valid @RequestBody AssignRoleRequest req) {
        Role r = roleService.listSystemRoles().stream()
            .filter(role -> role.getCode().equals(req.getRoleCode()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(404, "角色不存在: " + req.getRoleCode()));
        Long operator = SecurityContextHolder.currentUserId();
        roleService.assignRole(userId, r.getId(), req.getSchoolId(), req.getExpiresAt(), operator);
        return Result.success();
    }

    @DeleteMapping("/{roleId}")
    @RequireRole({"REGION_ADMIN"})
    public Result<Void> revoke(@PathVariable Long userId, @PathVariable Long roleId,
                                @RequestParam(required = false) Long schoolId) {
        roleService.revokeRole(userId, roleId, schoolId);
        return Result.success();
    }
}
