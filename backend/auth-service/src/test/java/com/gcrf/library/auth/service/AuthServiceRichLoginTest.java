package com.gcrf.library.auth.service;

import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRichLoginTest {

    @Mock UserMapper userMapper;
    @Mock RoleService roleService;
    @Mock PermissionService permissionService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock RedissonClient redissonClient;

    JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "test-secret-must-be-at-least-64-bytes-long-to-satisfy-hs512-requirement-2026!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        ReflectionTestUtils.setField(authService, "jwtUtil", jwtUtil);
    }

    @Test
    @DisplayName("login_richResponse_carriesRolesTenantScope")
    void login_richResponse_carriesRolesTenantScope() {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword(encoder.encode("admin123"));
        adminUser.setStatus("ACTIVE");
        adminUser.setUserType("ADMIN");
        adminUser.setTenantSchema(null);  // region-level user

        when(userMapper.selectOne(any())).thenReturn(adminUser);

        Role regionAdmin = new Role();
        regionAdmin.setId(1L);
        regionAdmin.setCode("REGION_ADMIN");
        regionAdmin.setScopeDefault("REGION");
        when(roleService.rolesOfUser(1L)).thenReturn(List.of(regionAdmin));
        when(permissionService.codesForUser(1L)).thenReturn(Set.of("book.read", "book.write"));
        when(refreshTokenService.issue(1L)).thenReturn("test-refresh-token-uuid");

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        LoginResponse resp = authService.login(req);

        assertNotNull(resp.getAccessToken());
        assertEquals("test-refresh-token-uuid", resp.getRefreshToken());
        assertEquals(List.of("REGION_ADMIN"), resp.getRoles());
        assertNull(resp.getTenant());
        assertEquals("REGION", resp.getScope());
        assertTrue(resp.getPermissions().contains("book.read"));
    }
}
