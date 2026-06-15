package com.secondhand.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息 DTO
 */
@Data
public class ChatMessageDTO {

    private Long msgId;
    private Long fromUserId;
    private Long toUserId;
    private Long productId;
    private String content;
    private Integer hasRead;
    private LocalDateTime createdAt;
}
