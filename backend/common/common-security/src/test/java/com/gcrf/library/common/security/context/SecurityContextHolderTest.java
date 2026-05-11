package com.gcrf.library.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHolderTest {

    @AfterEach
    void cleanup() { SecurityContextHolder.clear(); }

    @Test
    void emptyByDefault() {
        assertTrue(SecurityContextHolder.current().isEmpty());
        assertNull(SecurityContextHolder.currentUserId());
        assertNull(SecurityContextHolder.currentTenant());
        assertEquals(List.of(), SecurityContextHolder.currentRoles());
        assertFalse(SecurityContextHolder.hasRole("REGION_ADMIN"));
        assertEquals(Scope.SELF, SecurityContextHolder.currentScope());
    }

    @Test
    void setAndRead() {
        SecurityContext ctx = SecurityContext.builder()
            .userId(42L)
            .username("alice")
            .tenant("school_000001")
            .tenantId(1L)
            .roles(List.of("LIBRARIAN", "TEACHER"))
            .scope(Scope.SCHOOL)
            .orgPath("/100/200/305/")
            .build();
        SecurityContextHolder.set(ctx);

        assertEquals(42L, SecurityContextHolder.currentUserId());
        assertEquals("school_000001", SecurityContextHolder.currentTenant());
        assertEquals(Scope.SCHOOL, SecurityContextHolder.currentScope());
        assertTrue(SecurityContextHolder.hasRole("LIBRARIAN"));
        assertFalse(SecurityContextHolder.hasRole("REGION_ADMIN"));
        assertTrue(SecurityContextHolder.hasScope(Scope.SCHOOL));
        assertTrue(SecurityContextHolder.hasScope(Scope.SELF));      // SCHOOL ⊇ SELF
        assertFalse(SecurityContextHolder.hasScope(Scope.REGION));   // SCHOOL ⊉ REGION
        assertEquals("/100/200/305/", SecurityContextHolder.currentOrgPath());
    }

    @Test
    void clearWipesContext() {
        SecurityContextHolder.set(SecurityContext.builder().userId(1L).build());
        SecurityContextHolder.clear();
        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void scopeHierarchy_regionContainsAll() {
        SecurityContextHolder.set(SecurityContext.builder()
            .scope(Scope.REGION).build());
        assertTrue(SecurityContextHolder.hasScope(Scope.REGION));
        assertTrue(SecurityContextHolder.hasScope(Scope.SCHOOL));
        assertTrue(SecurityContextHolder.hasScope(Scope.GRADE));
        assertTrue(SecurityContextHolder.hasScope(Scope.CLASS));
        assertTrue(SecurityContextHolder.hasScope(Scope.SELF));
    }
}
