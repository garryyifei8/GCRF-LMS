package com.gcrf.library.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author 张三
 * @date 2025-10-11
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAILED(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 参数验证失败
     */
    VALIDATE_FAILED(400, "参数验证失败"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(5000, "业务异常"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(5001, "用户不存在"),

    /**
     * 用户名或密码错误
     */
    USER_CREDENTIALS_ERROR(5002, "用户名或密码错误"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(5003, "用户已被禁用"),

    /**
     * 用户名已存在
     */
    USER_ALREADY_EXISTS(5004, "用户名已存在"),

    /**
     * 手机号已存在
     */
    PHONE_ALREADY_EXISTS(5005, "手机号已存在"),

    /**
     * 邮箱已存在
     */
    EMAIL_ALREADY_EXISTS(5006, "邮箱已存在"),

    /**
     * 旧密码不正确
     */
    OLD_PASSWORD_INCORRECT(5007, "旧密码不正确"),

    /**
     * 新密码不能与旧密码相同
     */
    NEW_PASSWORD_SAME_AS_OLD(5008, "新密码不能与旧密码相同"),

    /**
     * Token无效
     */
    TOKEN_INVALID(5009, "Token无效"),

    /**
     * Token过期
     */
    TOKEN_EXPIRED(5010, "Token已过期"),

    /**
     * 图书不存在
     */
    BOOK_NOT_FOUND(5101, "图书不存在"),

    /**
     * 图书已借出
     */
    BOOK_BORROWED(5102, "图书已借出"),

    /**
     * 图书库存不足
     */
    BOOK_STOCK_INSUFFICIENT(5103, "图书库存不足"),

    /**
     * 读者不存在
     */
    READER_NOT_FOUND(5201, "读者不存在"),

    /**
     * 读者证已存在
     */
    READER_CARD_EXISTS(5202, "读者证已存在"),

    /**
     * 读者借阅量已达上限
     */
    READER_BORROW_LIMIT(5203, "读者借阅量已达上限"),

    /**
     * 读者有逾期图书
     */
    READER_HAS_OVERDUE(5204, "读者有逾期图书，无法借阅"),

    /**
     * 借阅记录不存在
     */
    CIRCULATION_NOT_FOUND(5301, "借阅记录不存在"),

    /**
     * 图书未借出
     */
    BOOK_NOT_BORROWED(5302, "图书未借出"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(5401, "文件上传失败"),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_FAILED(5402, "文件下载失败"),

    /**
     * 文件格式不支持
     */
    FILE_FORMAT_NOT_SUPPORTED(5403, "文件格式不支持"),

    /**
     * 数据重复
     */
    DUPLICATE_DATA(5500, "数据重复"),

    /**
     * 操作失败
     */
    OPERATION_FAILED(5501, "操作失败");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}
