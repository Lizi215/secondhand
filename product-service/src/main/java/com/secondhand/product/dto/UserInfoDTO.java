package com.secondhand.product.dto;

import lombok.Data;

/**
 * 用户信息 DTO（从 user-service Feign 获取）
 */
@Data
public class UserInfoDTO {

    private Long userId;
    private String username;
    private Integer role;
    private String nickname;
    private String phone;
    private Integer isMuted;
}
