package com.secondhand.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String token;
    private Integer role;
    private String nickname;
}
