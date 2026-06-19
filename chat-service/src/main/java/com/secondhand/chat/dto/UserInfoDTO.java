package com.secondhand.chat.dto;

import lombok.Data;

/**
 * 用户信息 DTO（Feign 调用 user-service 返回）
 */
@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private Integer role;
    private String nickname;
    private String phone;
    private Integer isMuted;
    private java.time.LocalDateTime createdAt;
}
