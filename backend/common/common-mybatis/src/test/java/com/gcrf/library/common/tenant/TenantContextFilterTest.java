package com.gcrf.library.common.tenant;

import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantContextFilterTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void noAuthHeader_doesNotSetTenant_andClearsAfter() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        TenantContextFilter f = new TenantContextFilter(jwt);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(TenantContext.getTenant()).isNull();
    }

    @Test
    void validToken_setsTenantFromClaim() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwt.parseToken("abc")).thenReturn(claims);
        when(claims.get("tenant", String.class)).thenReturn("school_001");

        TenantContextFilter f = new TenantContextFilter(jwt) {
            @Override
            protected void onChainBeforeClear(HttpServletRequest req, HttpServletResponse res) {
                assertThat(TenantContext.getTenant()).isEqualTo("school_001");
            }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse res = new MockHttpServletResponse();

        f.doFilter(req, res, mock(FilterChain.class));

        assertThat(TenantContext.getTenant()).isNull(); // cleared after filter
    }

    @Test
    void invalidToken_ignoredAndContinues() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.parseToken(any())).thenThrow(new RuntimeException("bad token"));
        TenantContextFilter f = new TenantContextFilter(jwt);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        f.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(TenantContext.getTenant()).isNull();
    }
}
