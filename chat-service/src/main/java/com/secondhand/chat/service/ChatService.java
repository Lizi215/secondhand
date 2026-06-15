package com.secondhand.chat.service;

import com.secondhand.chat.dto.ChatMessageDTO;
import com.secondhand.chat.dto.SendMessageRequest;

import java.util.List;

public interface ChatService {

    /**
     * 发送消息
     */
    ChatMessageDTO sendMessage(Long fromUserId, SendMessageRequest request);

    /**
     * 获取与某用户的聊天历史（按时间升序）
     */
    List<ChatMessageDTO> getHistory(Long myUserId, Long otherUserId, Long productId);

    /**
     * 轮询新消息（返回大于 lastMsgId 的、与指定用户聊天的消息）
     */
    List<ChatMessageDTO> pollNewMessages(Long userId, Long otherUserId, Long lastMsgId);

    /**
     * 获取询问某商品的买家 ID 列表（排除卖家自己）
     */
    List<Long> getProductInquirerIds(Long productId, Long sellerId);
}
