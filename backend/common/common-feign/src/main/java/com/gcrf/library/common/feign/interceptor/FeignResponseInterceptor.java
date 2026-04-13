package com.gcrf.library.common.feign.interceptor;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Feign响应拦截器
 * 用于统一处理Feign调用的响应结果
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Aspect
public class FeignResponseInterceptor {

    /**
     * 拦截所有Feign客户端的调用
     * 对返回Result类型的方法进行结果检查
     */
    @Around("@within(org.springframework.cloud.openfeign.FeignClient)")
    public Object handleFeignResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Feign调用成功: method={}, duration={}ms", methodName, duration);

            // 如果返回类型是Result，检查业务状态码
            if (result instanceof Result<?> feignResult) {
                if (!feignResult.isSuccess()) {
                    log.warn("Feign调用业务失败: method={}, code={}, message={}",
                            methodName, feignResult.getCode(), feignResult.getMessage());

                    // 根据错误码决定是否抛出异常
                    // 这里可以根据业务需求自定义处理逻辑
                    // 默认不抛出异常，由调用方自行处理
                }
            }

            return result;

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Feign调用业务异常: method={}, duration={}ms, code={}, message={}",
                    methodName, duration, e.getCode(), e.getMessage());
            throw e;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Feign调用系统异常: method={}, duration={}ms, error={}",
                    methodName, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 检查并解包Feign响应结果
     * 如果响应失败，抛出业务异常
     *
     * @param result Feign响应结果
     * @param <T>    数据类型
     * @return 响应数据
     */
    public static <T> T unwrap(Result<T> result) {
        if (result == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                    "远程服务响应为空");
        }
        if (!result.isSuccess()) {
            throw new BusinessException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }

    /**
     * 检查Feign响应是否成功
     *
     * @param result Feign响应结果
     * @return 是否成功
     */
    public static boolean isSuccess(Result<?> result) {
        return result != null && result.isSuccess();
    }

    /**
     * 安全获取Feign响应数据
     * 如果失败返回默认值
     *
     * @param result       Feign响应结果
     * @param defaultValue 默认值
     * @param <T>          数据类型
     * @return 响应数据或默认值
     */
    public static <T> T getOrDefault(Result<T> result, T defaultValue) {
        if (result == null || !result.isSuccess()) {
            return defaultValue;
        }
        return result.getData() != null ? result.getData() : defaultValue;
    }
}
