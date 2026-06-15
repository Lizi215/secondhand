package com.secondhand.common.exception;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // ==================== 通用错误 (1000-1999) ====================
    SUCCESS(0, "success"),
    PARAM_ERROR(1000, "参数错误"),
    UNAUTHORIZED(1001, "未登录或Token已过期"),
    FORBIDDEN(1002, "无权限访问"),
    NOT_FOUND(1003, "资源不存在"),
    SYSTEM_ERROR(1004, "系统繁忙，请稍后重试"),

    // ==================== 用户相关 (2000-2999) ====================
    USER_NOT_FOUND(2000, "用户不存在"),
    USER_PASSWORD_ERROR(2001, "密码错误"),
    USER_EXIST(2002, "用户已存在"),
    USER_MUTED(2003, "账号已被禁言"),
    USERNAME_EMPTY(2004, "用户名不能为空"),
    PASSWORD_EMPTY(2005, "密码不能为空"),
    INVALID_USERNAME(2006, "用户名格式不正确"),

    // ==================== 商品相关 (3000-3999) ====================
    PRODUCT_NOT_FOUND(3000, "商品不存在"),
    PRODUCT_NAME_EMPTY(3001, "商品名称不能为空"),
    PRODUCT_PRICE_ERROR(3002, "商品价格不合法"),
    PRODUCT_NOT_YOURS(3003, "只能操作自己的商品"),

    // ==================== 聊天相关 (4000-4999) ====================
    CHAT_CONTENT_EMPTY(4000, "消息内容不能为空"),
    CHAT_SELF(4001, "不能给自己发消息"),

    // ==================== Token 相关 (5000-5999) ====================
    TOKEN_INVALID(5000, "Token无效"),
    TOKEN_EXPIRED(5001, "Token已过期"),
    TOKEN_MISSING(5002, "缺少Token");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
