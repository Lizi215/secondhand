package com.secondhand.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long msgId;

    /** 发送者 ID */
    private Long fromUserId;

    /** 接收者 ID */
    private Long toUserId;

    /** 关联商品 ID（可为 null） */
    private Long productId;

    /** 消息内容 */
    private String content;

    /** 0:未读, 1:已读 */
    private Integer hasRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
