package com.gcrf.library.common.exception;

import com.gcrf.library.common.result.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统异常类
 * <p>
 * 用于系统级错误，如配置错误、基础设施故障、外部系统调用失败等。
 * 与BusinessException区分：SystemException通常表示系统层面的技术问题，
 * 而BusinessException表示业务逻辑层面的问题。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 抛出带默认错误码的系统异常
 * throw new SystemException("数据库连接失败");
 *
 * // 抛出带自定义错误码的系统异常
 * throw new SystemException(5500, "Redis连接超时");
 *
 * // 使用ResultCode枚举抛出系统异常
 * throw new SystemException(ResultCode.SERVICE_UNAVAILABLE);
 *
 * // 使用ResultCode枚举并自定义消息
 * throw new SystemException(ResultCode.SERVICE_UNAVAILABLE, "配置中心服务不可用");
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 构造系统异常
     * <p>使用默认的系统错误码（500）</p>
     *
     * @param message 错误消息
     */
    public SystemException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.message = message;
    }

    /**
     * 构造系统异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public SystemException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造系统异常
     *
     * @param resultCode 响应码枚举
     */
    public SystemException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造系统异常
     * <p>使用ResultCode的错误码，但自定义错误消息</p>
     *
     * @param resultCode 响应码枚举
     * @param message    自定义错误消息
     */
    public SystemException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 构造系统异常
     * <p>包含原始异常的堆栈信息，便于排查问题</p>
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.message = message;
    }

    /**
     * 构造系统异常
     * <p>包含原始异常的堆栈信息，便于排查问题</p>
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public SystemException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造系统异常
     * <p>包含原始异常的堆栈信息，便于排查问题</p>
     *
     * @param resultCode 响应码枚举
     * @param cause      原始异常
     */
    public SystemException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造系统异常
     * <p>包含原始异常的堆栈信息，便于排查问题</p>
     *
     * @param resultCode 响应码枚举
     * @param message    自定义错误消息
     * @param cause      原始异常
     */
    public SystemException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
        this.message = message;
    }
}
