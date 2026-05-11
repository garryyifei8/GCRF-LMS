package com.gcrf.library.common.security.aspect;

import com.gcrf.library.common.security.annotation.RequirePermission;
import com.gcrf.library.common.security.annotation.RequireRole;
import com.gcrf.library.common.security.annotation.RequireScope;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.security.exception.AccessDeniedException;
import com.gcrf.library.common.security.permission.PermissionLookup;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Aspect
@RequiredArgsConstructor
public class SecurityRequirementAspect {

    private final PermissionLookup permissionLookup;

    // ------------------------------------------------------------------ role

    @Around("@annotation(com.gcrf.library.common.security.annotation.RequireRole)" +
            " || @within(com.gcrf.library.common.security.annotation.RequireRole)")
    public Object aroundRole(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole requireRole = resolveAnnotation(pjp, RequireRole.class);
        if (Arrays.stream(requireRole.value()).noneMatch(SecurityContextHolder::hasRole)) {
            throw new AccessDeniedException(
                    "需要角色: " + String.join("/", requireRole.value()));
        }
        return pjp.proceed();
    }

    // -------------------------------------------------------------- permission

    @Around("@annotation(com.gcrf.library.common.security.annotation.RequirePermission)" +
            " || @within(com.gcrf.library.common.security.annotation.RequirePermission)")
    public Object aroundPermission(ProceedingJoinPoint pjp) throws Throwable {
        RequirePermission requirePermission = resolveAnnotation(pjp, RequirePermission.class);
        Long uid = SecurityContextHolder.currentUserId();
        if (uid == null) {
            throw new AccessDeniedException("未认证");
        }
        Set<String> perms = permissionLookup.lookup(uid);
        if (!perms.contains(requirePermission.value())) {
            throw new AccessDeniedException("缺少权限: " + requirePermission.value());
        }
        return pjp.proceed();
    }

    // ----------------------------------------------------------------- scope

    @Around("@annotation(com.gcrf.library.common.security.annotation.RequireScope)" +
            " || @within(com.gcrf.library.common.security.annotation.RequireScope)")
    public Object aroundScope(ProceedingJoinPoint pjp) throws Throwable {
        RequireScope requireScope = resolveAnnotation(pjp, RequireScope.class);
        if (!SecurityContextHolder.hasScope(requireScope.value())) {
            throw new AccessDeniedException("权限范围不足: 需要 " + requireScope.value());
        }
        return pjp.proceed();
    }

    // ---------------------------------------------------------------- helper

    /**
     * Resolves annotation from method first (method wins), then falls back to class level.
     * This handles both @annotation and @within matches correctly.
     */
    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(
            ProceedingJoinPoint pjp, Class<A> annotationType) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        A annotation = method.getAnnotation(annotationType);
        if (annotation == null) {
            annotation = pjp.getTarget().getClass().getAnnotation(annotationType);
        }
        if (annotation == null) {
            throw new IllegalStateException(
                    "Expected @" + annotationType.getSimpleName() + " on method or class but found none");
        }
        return annotation;
    }
}
