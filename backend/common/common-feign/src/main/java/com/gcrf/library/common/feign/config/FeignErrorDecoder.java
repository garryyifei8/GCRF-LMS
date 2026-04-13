package com.gcrf.library.common.feign.config;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.exception.SystemException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.result.ResultCode;
import com.gcrf.library.common.utils.JsonUtil;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Feign错误解码器
 * 用于统一处理Feign调用的错误响应
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign调用失败: method={}, status={}, reason={}",
                methodKey, response.status(), response.reason());

        // 读取响应体
        String body = null;
        try {
            if (response.body() != null) {
                try (InputStream inputStream = response.body().asInputStream()) {
                    body = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            log.warn("读取Feign错误响应体失败", e);
        }

        // 根据HTTP状态码处理
        int status = response.status();

        // 4xx 客户端错误
        if (status >= 400 && status < 500) {
            return handleClientError(methodKey, status, body);
        }

        // 5xx 服务端错误
        if (status >= 500) {
            return handleServerError(methodKey, status, body);
        }

        // 其他错误使用默认处理
        return defaultDecoder.decode(methodKey, response);
    }

    /**
     * 处理客户端错误（4xx）
     */
    private Exception handleClientError(String methodKey, int status, String body) {
        log.warn("Feign客户端错误: method={}, status={}, body={}", methodKey, status, body);

        // 尝试解析业务错误
        if (body != null && !body.isEmpty()) {
            try {
                Result<?> result = JsonUtil.parseObject(body, Result.class);
                if (result != null && result.getCode() != null) {
                    return new BusinessException(result.getCode(), result.getMessage());
                }
            } catch (Exception e) {
                log.debug("解析Feign错误响应体失败，使用默认错误处理", e);
            }
        }

        // 根据状态码返回不同的异常
        return switch (status) {
            case 400 -> new BusinessException(ResultCode.PARAM_ERROR);
            case 401 -> new BusinessException(ResultCode.UNAUTHORIZED);
            case 403 -> new BusinessException(ResultCode.FORBIDDEN);
            case 404 -> new BusinessException(ResultCode.NOT_FOUND);
            case 405 -> new BusinessException(ResultCode.METHOD_NOT_ALLOWED);
            default -> new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "远程服务调用失败: " + body);
        };
    }

    /**
     * 处理服务端错误（5xx）
     */
    private Exception handleServerError(String methodKey, int status, String body) {
        log.error("Feign服务端错误: method={}, status={}, body={}", methodKey, status, body);

        // 尝试解析业务错误
        if (body != null && !body.isEmpty()) {
            try {
                Result<?> result = JsonUtil.parseObject(body, Result.class);
                if (result != null && result.getCode() != null) {
                    // 如果是业务异常码，返回业务异常
                    if (result.getCode() >= 5000) {
                        return new BusinessException(result.getCode(), result.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("解析Feign错误响应体失败，使用默认错误处理", e);
            }
        }

        // 根据状态码返回系统异常
        return switch (status) {
            case 502 -> new SystemException(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                    "网关错误，远程服务不可用");
            case 503 -> new SystemException(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                    "远程服务暂时不可用");
            case 504 -> new SystemException(ResultCode.GATEWAY_TIMEOUT.getCode(),
                    "网关超时，远程服务响应超时");
            default -> new SystemException(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                    "远程服务内部错误: " + body);
        };
    }
}
