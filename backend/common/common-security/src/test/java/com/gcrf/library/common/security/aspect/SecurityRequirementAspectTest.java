package com.gcrf.library.common.security.aspect;

import com.gcrf.library.common.security.annotation.RequirePermission;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.annotation.RequireScope;
import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.security.exception.AccessDeniedException;
import com.gcrf.library.common.security.permission.PermissionLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityRequirementAspectTest {

    static class FakePermissionLookup implements PermissionLookup {
        @Override public Set<String> lookup(Long userId) {
            return userId == 42L ? Set.of("book.read", "book.write") : Set.of();
        }
    }

    static class Target {
        @RequireRole("REGION_ADMIN")
        public String regionOnly() { return "ok"; }

        @RequirePermission("book.write")
        public String writeBook() { return "ok"; }

        @RequireScope(Scope.REGION)
        public String regionScope() { return "ok"; }
    }

    private Target proxy;

    @BeforeEach
    void setUp() {
        AspectJProxyFactory f = new AspectJProxyFactory(new Target());
        f.addAspect(new SecurityRequirementAspect(new FakePermissionLookup()));
        proxy = f.getProxy();
        SecurityContextHolder.clear();
    }
    @AfterEach
    void cleanup() { SecurityContextHolder.clear(); }

    @Test
    void requireRole_allowsWhenRolePresent() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).roles(List.of("REGION_ADMIN")).scope(Scope.REGION).build());
        assertEquals("ok", proxy.regionOnly());
    }

    @Test
    void requireRole_denies403WhenMissing() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).roles(List.of("LIBRARIAN")).scope(Scope.SCHOOL).build());
        assertThrows(AccessDeniedException.class, () -> proxy.regionOnly());
    }

    @Test
    void requirePermission_lookupsAndAllows() {
        SecurityContextHolder.set(SecurityContext.builder().userId(42L).build());
        assertEquals("ok", proxy.writeBook());
    }

    @Test
    void requirePermission_denies() {
        SecurityContextHolder.set(SecurityContext.builder().userId(99L).build());
        assertThrows(AccessDeniedException.class, () -> proxy.writeBook());
    }

    @Test
    void requireScope_allowsWhenCovered() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).scope(Scope.REGION).build());
        assertEquals("ok", proxy.regionScope());
    }

    @Test
    void requireScope_denies() {
        SecurityContextHolder.set(SecurityContext.builder()
            .userId(1L).scope(Scope.SCHOOL).build());
        assertThrows(AccessDeniedException.class, () -> proxy.regionScope());
    }
}
