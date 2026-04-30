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
