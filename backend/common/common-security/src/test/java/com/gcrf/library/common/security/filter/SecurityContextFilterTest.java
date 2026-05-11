package com.gcrf.library.common.security.filter;

import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextFilterTest {

    private JwtUtil jwtUtil;
    private SecurityContextFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "test-secret-must-be-at-least-64-bytes-long-to-satisfy-hs512-requirement-2026!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        filter = new SecurityContextFilter(jwtUtil);
        SecurityContextHolder.clear();
    }

    @Test
    void parsesRichJwtIntoContext() throws Exception {
        String token = jwtUtil.generateToken("42", Map.of(
            "userId", 42L,
            "username", "alice",
            "tenant", "school_000001",
            "tenantId", 1L,
            "roles", List.of("LIBRARIAN"),
            "scope", "SCHOOL",
            "orgPath", "/100/200/305/"
        ));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (request, response) -> {
            // Inside the chain, context should be populated.
            assertEquals(42L, SecurityContextHolder.currentUserId());
            assertEquals("school_000001", SecurityContextHolder.currentTenant());
            assertEquals(Scope.SCHOOL, SecurityContextHolder.currentScope());
            assertTrue(SecurityContextHolder.hasRole("LIBRARIAN"));
            assertEquals("/100/200/305/", SecurityContextHolder.currentOrgPath());
        };

        filter.doFilter(req, res, chain);

        // After chain, context must be cleared.
        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void noAuthHeader_leavesContextEmpty() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertTrue(SecurityContextHolder.current().isEmpty());
    }

    @Test
    void malformedToken_leavesContextEmptyAndDoesNotFail() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not.a.valid.jwt.token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        // 200 chain still ran (auth enforced elsewhere if needed)
        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        assertTrue(SecurityContextHolder.current().isEmpty());
    }
}
