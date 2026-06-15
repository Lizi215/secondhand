package com.secondhand.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO（不返回密码等敏感信息）
 */
@Data
public class UserInfoDTO {

    private Long userId;
    private String username;
    private Integer role;
    private String nickname;
    private String phone;
    private Integer isMuted;
    private LocalDateTime createdAt;
}
