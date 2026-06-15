package com.secondhand.common.constant;

/**
 * 系统常量
 */
public interface Constant {

    // ==================== Token ====================
    /** JWT 密钥 */
    String JWT_SECRET = "SecondHandTradingSecretKey2024";
    /** JWT 过期时间（毫秒）：7天 */
    long JWT_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;
    /** 请求头中 Token 的 key */
    String AUTH_HEADER = "Authorization";
    /** Token 前缀 */
    String TOKEN_PREFIX = "Bearer ";
    /** 请求头中用户 ID 的 key（Gateway 解析后放入） */
    String USER_ID_HEADER = "X-User-Id";
    /** 请求头中用户角色的 key */
    String USER_ROLE_HEADER = "X-User-Role";

    // ==================== 用户角色 ====================
    /** 普通用户 */
    int ROLE_USER = 0;
    /** 管理员 */
    int ROLE_ADMIN = 1;

    // ==================== 商品状态 ====================
    /** 下架 */
    int PRODUCT_STATUS_OFF = 0;
    /** 上架 */
    int PRODUCT_STATUS_ON = 1;

    // ==================== 用户状态 ====================
    /** 正常 */
    int USER_STATUS_NORMAL = 0;
    /** 禁言 */
    int USER_STATUS_MUTED = 1;

    // ==================== 聊天消息 ====================
    /** 未读 */
    int MSG_UNREAD = 0;
    /** 已读 */
    int MSG_READ = 1;
}
