package com.gcrf.library.common.security.context;

import java.util.List;
import java.util.Optional;

/**
 * ThreadLocal 容器，持有当前请求线程的 {@link SecurityContext}。
 *
 * <p>生命周期：
 * <ol>
 *   <li>JWT 过滤器解析 token 后调用 {@link #set(SecurityContext)}；</li>
 *   <li>请求结束后（finally 块或过滤器出口）调用 {@link #clear()} 防止内存泄漏。</li>
 * </ol>
 *
 * @author GCRF Team
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CURRENT = new ThreadLocal<>();

    /** 空上下文单例，避免每次 get 都 new 对象 */
    private static final SecurityContext EMPTY = SecurityContext.builder().build();

    private SecurityContextHolder() {}

    /** 设置当前线程的安全上下文 */
    public static void set(SecurityContext ctx) {
        CURRENT.set(ctx);
    }

    /** 清除当前线程的安全上下文（请求结束时必须调用） */
    public static void clear() {
        CURRENT.remove();
    }

    /** 返回当前上下文的 Optional 包装（未认证时为空） */
    public static Optional<SecurityContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    private static SecurityContext currentOrEmpty() {
        SecurityContext c = CURRENT.get();
        return c == null ? EMPTY : c;
    }

    public static Long currentUserId()        { return currentOrEmpty().getUserId(); }
    public static String currentUsername()    { return currentOrEmpty().getUsername(); }
    public static String currentTenant()      { return currentOrEmpty().getTenant(); }
    public static Long currentTenantId()      { return currentOrEmpty().getTenantId(); }
    public static List<String> currentRoles() { return currentOrEmpty().getRoles(); }
    public static Scope currentScope()        { return currentOrEmpty().getScope(); }
    public static String currentOrgPath()     { return currentOrEmpty().getOrgPath(); }

    /** 判断当前用户是否持有指定角色 */
    public static boolean hasRole(String code) {
        return currentOrEmpty().getRoles().contains(code);
    }

    /** 判断当前用户的数据范围是否覆盖所要求的范围（scope hierarchy） */
    public static boolean hasScope(Scope required) {
        return currentOrEmpty().getScope().covers(required);
    }
}
