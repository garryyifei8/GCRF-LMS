package com.gcrf.library.opac.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter limiter;
    private final ObjectMapper json;

    @Value("${gcrf.opac.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!enabled || !(handler instanceof HandlerMethod hm)) return true;

        Method m = hm.getMethod();
        RateLimit ann = m.getAnnotation(RateLimit.class);
        if (ann == null) return true;

        String ip = clientIp(req);
        String key = ip + ":" + m.getDeclaringClass().getSimpleName() + "." + m.getName();
        if (limiter.tryAcquire(key, ann.value(), ann.periodSeconds())) return true;

        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(json.writeValueAsString(Result.error(429, "Too Many Requests")));
        return false;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
