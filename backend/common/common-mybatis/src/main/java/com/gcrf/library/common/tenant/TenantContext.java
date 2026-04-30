package com.gcrf.library.common.tenant;

/**
 * 当前请求的多租户 schema 上下文（ThreadLocal）。
 * 由 TenantContextFilter 在请求进入时写入，由 SearchPathInterceptor 读取。
 */
public final class TenantContext {

    public static final String REGION_SCHEMA = "gcrf_region";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String tenantSchema) {
        CURRENT.set(tenantSchema);
    }

    public static String getTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 返回 PostgreSQL search_path 字符串。
     * 有 tenant：`<tenant>, gcrf_region`；无 tenant：`gcrf_region`。
     */
    public static String resolveSearchPath() {
        String t = CURRENT.get();
        return t == null || t.isBlank() ? REGION_SCHEMA : t + ", " + REGION_SCHEMA;
    }
}
