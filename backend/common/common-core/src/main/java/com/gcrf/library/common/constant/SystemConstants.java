package com.gcrf.library.common.constant;

/**
 * 系统常量
 *
 * @author 张三
 * @date 2025-10-11
 */
public interface SystemConstants {

    /**
     * UTF-8 编码
     */
    String UTF8 = "UTF-8";

    /**
     * GBK 编码
     */
    String GBK = "GBK";

    /**
     * 成功标记
     */
    Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    Integer FAIL = 500;

    /**
     * 验证码有效期（分钟）
     */
    Integer CAPTCHA_EXPIRATION = 2;

    /**
     * Token前缀
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * Token请求头
     */
    String TOKEN_HEADER = "Authorization";

    /**
     * 用户ID字段
     */
    String USER_ID = "userId";

    /**
     * 用户名字段
     */
    String USERNAME = "username";

    /**
     * 登录用户信息键
     */
    String LOGIN_USER_KEY = "login_user:";

    /**
     * 验证码键前缀
     */
    String CAPTCHA_KEY = "captcha:";

    /**
     * 分页默认页码
     */
    Integer DEFAULT_PAGE_NUM = 1;

    /**
     * 分页默认每页大小
     */
    Integer DEFAULT_PAGE_SIZE = 20;

    /**
     * 分页最大每页大小
     */
    Integer MAX_PAGE_SIZE = 100;

    /**
     * 默认排序字段
     */
    String DEFAULT_ORDER_BY = "create_time";

    /**
     * 升序
     */
    String ORDER_ASC = "asc";

    /**
     * 降序
     */
    String ORDER_DESC = "desc";
}
