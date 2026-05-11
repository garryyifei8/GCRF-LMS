package com.gcrf.library.auth.service;

import com.gcrf.library.common.security.permission.PermissionLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthPermissionLookup implements PermissionLookup {

    private final PermissionService permissionService;

    @Override
    // TODO: add @Cacheable(value = "perms", key = "#userId") in Task 11 when caching infra wired
    public Set<String> lookup(Long userId) {
        return permissionService.codesForUser(userId);
    }
}
