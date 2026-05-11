package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.entity.Permission;
import com.gcrf.library.auth.service.PermissionService;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Permission>> list() {
        return Result.success(permissionService.listAll());
    }
}
