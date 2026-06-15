package com.secondhand.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    private String username;

    private String password;

    /** 0:普通用户, 1:管理员 */
    private Integer role;

    private String nickname;

    private String phone;

    /** 0:正常, 1:禁言 */
    private Integer isMuted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
