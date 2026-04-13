package com.gcrf.library.common.feign.constant;

/**
 * Feign常量定义
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
public final class FeignConstants {

    private FeignConstants() {
        throw new IllegalStateException("Constants class");
    }

    // ==================== 服务名称 ====================
    /**
     * 认证服务
     */
    public static final String AUTH_SERVICE = "auth-service";

    /**
     * 图书服务
     */
    public static final String BOOK_SERVICE = "book-service";

    /**
     * 读者服务
     */
    public static final String READER_SERVICE = "reader-service";

    /**
     * 流通服务
     */
    public static final String CIRCULATION_SERVICE = "circulation-service";

    /**
     * 系统服务
     */
    public static final String SYSTEM_SERVICE = "system-service";

    /**
     * 通知服务
     */
    public static final String NOTIFICATION_SERVICE = "notification-service";

    /**
     * 推荐服务
     */
    public static final String RECOMMEND_SERVICE = "recommend-service";

    /**
     * 分析服务
     */
    public static final String ANALYTICS_SERVICE = "analytics-service";

    /**
     * 聊天服务
     */
    public static final String CHAT_SERVICE = "chat-service";

    // ==================== API路径前缀 ====================
    /**
     * 认证服务API前缀
     */
    public static final String AUTH_API_PREFIX = "/api/v1/auth";

    /**
     * 图书服务API前缀
     */
    public static final String BOOK_API_PREFIX = "/api/v1/books";

    /**
     * 读者服务API前缀
     */
    public static final String READER_API_PREFIX = "/api/v1/readers";

    /**
     * 流通服务API前缀
     */
    public static final String CIRCULATION_API_PREFIX = "/api/v1/circulation";

    /**
     * 借阅服务API前缀
     */
    public static final String BORROW_API_PREFIX = "/api/v1/borrows";

    /**
     * 系统服务API前缀
     */
    public static final String SYSTEM_API_PREFIX = "/api/v1/system";

    /**
     * 通知服务API前缀
     */
    public static final String NOTIFICATION_API_PREFIX = "/api/v1/notifications";

    // ==================== 请求头常量 ====================
    /**
     * 授权头
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Bearer前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 用户ID头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 用户名头
     */
    public static final String HEADER_USERNAME = "X-Username";

    /**
     * 租户ID头
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 追踪ID头
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求来源头
     */
    public static final String HEADER_SOURCE = "X-Request-Source";

    /**
     * 服务间调用标识
     */
    public static final String SOURCE_FEIGN = "feign";

    // ==================== 超时配置 ====================
    /**
     * 默认连接超时（毫秒）
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /**
     * 默认读取超时（毫秒）
     */
    public static final int DEFAULT_READ_TIMEOUT = 10000;

    /**
     * 长操作读取超时（毫秒）
     */
    public static final int LONG_READ_TIMEOUT = 30000;

    // ==================== 重试配置 ====================
    /**
     * 默认最大重试次数
     */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 默认重试间隔（毫秒）
     */
    public static final long DEFAULT_RETRY_INTERVAL = 1000;
}
