package com.secondhand.user.dto;

import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
public class ChangePasswordRequest {

    private Long userId;
    private String newPassword;
}
