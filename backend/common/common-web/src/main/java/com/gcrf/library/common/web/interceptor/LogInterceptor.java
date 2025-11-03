package com.gcrf.library.common.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 请求日志拦截器
 * <p>
 * 记录HTTP请求和响应信息，用于调试和监控:
 * <ul>
 *   <li>记录请求方法、URI、参数、请求头</li>
 *   <li>记录响应状态码、执行时间</li>
 *   <li>生成唯一请求ID用于请求追踪</li>
 *   <li>排除敏感请求头(Authorization, Cookie等)</li>
 *   <li>可配置是否启用日志记录</li>
 * </ul>
 * <p>
 * 配置项(application.yml):
 * <pre>
 * library:
 *   web:
 *     logging:
 *       enabled: true  # 是否启用请求日志
 *       log-headers: false  # 是否记录请求头
 *       log-parameters: true  # 是否记录请求参数
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    /**
     * 请求开始时间的线程变量
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 请求ID的线程变量
     */
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    /**
     * 敏感请求头列表(不会被记录)
     */
    private static final String[] SENSITIVE_HEADERS = {
            "authorization", "cookie", "set-cookie", "x-auth-token",
            "x-csrf-token", "api-key", "x-api-key"
    };

    @Value("${library.web.logging.enabled:true}")
    private Boolean loggingEnabled;

    @Value("${library.web.logging.log-headers:false}")
    private Boolean logHeaders;

    @Value("${library.web.logging.log-parameters:true}")
    private Boolean logParameters;

    /**
     * 请求处理前执行
     * <p>
     * 记录请求信息并生成请求ID
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true继续处理，false中断处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!loggingEnabled) {
            return true;
        }

        // 生成唯一请求ID
        String requestId = UUID.randomUUID().toString().replace("-", "");
        REQUEST_ID.set(requestId);
        response.setHeader("X-Request-Id", requestId);

        // 记录开始时间
        START_TIME.set(System.currentTimeMillis());

        // 构建日志信息
        StringBuilder logMessage = new StringBuilder("\n========== 请求开始 ==========\n");
        logMessage.append("请求ID: ").append(requestId).append("\n");
        logMessage.append("请求方法: ").append(request.getMethod()).append("\n");
        logMessage.append("请求URI: ").append(request.getRequestURI()).append("\n");

        // 记录查询参数
        if (logParameters && request.getQueryString() != null) {
            logMessage.append("查询参数: ").append(request.getQueryString()).append("\n");
        }

        // 记录请求参数
        if (logParameters) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (!parameterMap.isEmpty()) {
                logMessage.append("请求参数: ").append(formatParameters(parameterMap)).append("\n");
            }
        }

        // 记录请求头
        if (logHeaders) {
            Map<String, String> headers = getRequestHeaders(request);
            if (!headers.isEmpty()) {
                logMessage.append("请求头: ").append(headers).append("\n");
            }
        }

        logMessage.append("客户端IP: ").append(getClientIp(request)).append("\n");
        logMessage.append("============================");

        log.info(logMessage.toString());
        return true;
    }

    /**
     * 请求处理后、视图渲染前执行
     *
     * @param request      HTTP请求
     * @param response     HTTP响应
     * @param handler      处理器
     * @param modelAndView 模型和视图
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // 可以在这里记录业务处理后的信息
    }

    /**
     * 请求完成后执行
     * <p>
     * 记录响应信息和执行时间
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @param ex       异常(如果有)
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!loggingEnabled) {
            return;
        }

        try {
            Long startTime = START_TIME.get();
            String requestId = REQUEST_ID.get();

            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;

                StringBuilder logMessage = new StringBuilder("\n========== 请求结束 ==========\n");
                logMessage.append("请求ID: ").append(requestId).append("\n");
                logMessage.append("请求URI: ").append(request.getRequestURI()).append("\n");
                logMessage.append("响应状态: ").append(response.getStatus()).append("\n");
                logMessage.append("执行时间: ").append(executionTime).append("ms\n");

                if (ex != null) {
                    logMessage.append("异常信息: ").append(ex.getMessage()).append("\n");
                }

                logMessage.append("============================");

                if (ex != null || response.getStatus() >= 400) {
                    log.error(logMessage.toString());
                } else if (executionTime > 3000) {
                    log.warn(logMessage.toString() + "\n注意: 请求执行时间过长(>3秒)");
                } else {
                    log.info(logMessage.toString());
                }
            }
        } finally {
            // 清理线程变量，避免内存泄漏
            START_TIME.remove();
            REQUEST_ID.remove();
        }
    }

    /**
     * 获取请求头信息
     * <p>
     * 排除敏感请求头
     *
     * @param request HTTP请求
     * @return 请求头映射
     */
    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // 跳过敏感请求头
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
        }

        return headers;
    }

    /**
     * 判断是否为敏感请求头
     *
     * @param headerName 请求头名称
     * @return true表示敏感，false表示非敏感
     */
    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null) {
            return false;
        }

        String lowerCaseHeaderName = headerName.toLowerCase();
        for (String sensitiveHeader : SENSITIVE_HEADERS) {
            if (lowerCaseHeaderName.contains(sensitiveHeader)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化请求参数
     *
     * @param parameterMap 参数映射
     * @return 格式化后的字符串
     */
    private String formatParameters(Map<String, String[]> parameterMap) {
        StringBuilder sb = new StringBuilder("{");
        parameterMap.forEach((key, values) -> {
            // 脱敏处理：密码、令牌等敏感字段
            if (isSensitiveParameter(key)) {
                sb.append(key).append("=").append("******").append(", ");
            } else {
                sb.append(key).append("=");
                if (values.length == 1) {
                    sb.append(values[0]);
                } else {
                    sb.append(java.util.Arrays.toString(values));
                }
                sb.append(", ");
            }
        });

        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 判断是否为敏感参数
     *
     * @param parameterName 参数名称
     * @return true表示敏感，false表示非敏感
     */
    private boolean isSensitiveParameter(String parameterName) {
        if (parameterName == null) {
            return false;
        }

        String lowerCaseParamName = parameterName.toLowerCase();
        return lowerCaseParamName.contains("password")
                || lowerCaseParamName.contains("pwd")
                || lowerCaseParamName.contains("token")
                || lowerCaseParamName.contains("secret")
                || lowerCaseParamName.contains("key")
                || lowerCaseParamName.contains("apikey");
    }

    /**
     * 获取客户端真实IP地址
     * <p>
     * 支持代理和负载均衡环境
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个IP时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
