#!/bin/bash

# 创建公共模块基础类
mkdir -p library-common/src/main/java/com/gcrf/library/common/{constant,enums,exception,result,utils}

# 创建统一返回结果类
cat > library-common/src/main/java/com/gcrf/library/common/result/Result.java << 'JAVAEOF'
package com.gcrf.library.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一返回结果
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
JAVAEOF

# 创建业务异常类
cat > library-common/src/main/java/com/gcrf/library/common/exception/BusinessException.java << 'JAVAEOF'
package com.gcrf.library.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private Integer code = 500;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
JAVAEOF

# 创建常量类
cat > library-common/src/main/java/com/gcrf/library/common/constant/CommonConstants.java << 'JAVAEOF'
package com.gcrf.library.common.constant;

/**
 * 公共常量
 */
public class CommonConstants {
    
    /** Token请求头 */
    public static final String HEADER_TOKEN = "Authorization";
    
    /** Token前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";
    
    /** 用户ID请求头 */
    public static final String HEADER_USER_ID = "X-User-Id";
    
    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;
    
    /** 失败状态码 */
    public static final int ERROR_CODE = 500;
    
    /** 未授权状态码 */
    public static final int UNAUTHORIZED_CODE = 401;
    
    /** 禁止访问状态码 */
    public static final int FORBIDDEN_CODE = 403;
}
JAVAEOF

echo "Base classes created successfully!"
