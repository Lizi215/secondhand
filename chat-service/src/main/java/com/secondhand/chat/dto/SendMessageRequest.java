package com.secondhand.chat.dto;

import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
public class SendMessageRequest {

    /** 接收者 ID */
    private Long toUserId;

    /** 关联商品 ID（可选） */
    private Long productId;

    /** 消息内容 */
    private String content;
}
