package com.secondhand.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.chat.dto.ChatMessageDTO;
import com.secondhand.chat.dto.SendMessageRequest;
import com.secondhand.chat.entity.ChatMessage;
import com.secondhand.chat.mapper.ChatMessageMapper;
import com.secondhand.chat.service.ChatService;
import com.secondhand.chat.dto.UserInfoDTO;
import com.secondhand.chat.feign.UserFeignClient;
import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    private final UserFeignClient userFeignClient;

    @Override
    public ChatMessageDTO sendMessage(Long fromUserId, SendMessageRequest request) {
        if (request.getToUserId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (fromUserId.equals(request.getToUserId())) {
            throw new BusinessException(ErrorCode.CHAT_SELF);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.CHAT_CONTENT_EMPTY);
        }

        // 检查发送者是否被禁言
        try {
            Result<UserInfoDTO> userResult = userFeignClient.getUserById(fromUserId);
            if (userResult != null && userResult.getData() != null
                    && userResult.getData().getIsMuted() != null
                    && userResult.getData().getIsMuted() == Constant.USER_STATUS_MUTED) {
                throw new BusinessException(ErrorCode.USER_MUTED);
            }
        } catch (BusinessException e) {
            if (e.getCode() == ErrorCode.USER_MUTED.getCode()) {
                throw e;
            }
            log.warn("禁言检查时调用 user-service 失败，放行消息: {}", e.getMessage());
        }

        ChatMessage msg = new ChatMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(request.getToUserId());
        msg.setProductId(request.getProductId());
        msg.setContent(request.getContent().trim());
        msg.setHasRead(Constant.MSG_UNREAD);

        baseMapper.insert(msg);
        log.info("消息发送成功，msgId={}", msg.getMsgId());

        return toDTO(msg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMessageDTO> getHistory(Long myUserId, Long otherUserId, Long productId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w
                        .eq(ChatMessage::getFromUserId, myUserId)
                        .eq(ChatMessage::getToUserId, otherUserId)
                        .or()
                        .eq(ChatMessage::getFromUserId, otherUserId)
                        .eq(ChatMessage::getToUserId, myUserId)
                )
                .orderByAsc(ChatMessage::getCreatedAt);

        // 如果指定了商品，只查该商品的聊天
        if (productId != null && productId > 0) {
            wrapper.eq(ChatMessage::getProductId, productId);
        }

        List<ChatMessage> messages = baseMapper.selectList(wrapper);

        // 将消息标记为已读
        List<Long> unreadIds = messages.stream()
                .filter(m -> m.getToUserId().equals(myUserId) && m.getHasRead() == Constant.MSG_UNREAD)
                .map(ChatMessage::getMsgId)
                .collect(Collectors.toList());
        if (!unreadIds.isEmpty()) {
            baseMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                    .in(ChatMessage::getMsgId, unreadIds)
                    .set(ChatMessage::getHasRead, Constant.MSG_READ));
        }

        return messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMessageDTO> pollNewMessages(Long userId, Long otherUserId, Long lastMsgId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w
                        .eq(ChatMessage::getFromUserId, otherUserId)
                        .eq(ChatMessage::getToUserId, userId)
                        .or()
                        .eq(ChatMessage::getFromUserId, userId)
                        .eq(ChatMessage::getToUserId, otherUserId)
                )
                .orderByAsc(ChatMessage::getCreatedAt);

        if (lastMsgId != null && lastMsgId > 0) {
            wrapper.gt(ChatMessage::getMsgId, lastMsgId);
        }

        List<ChatMessage> messages = baseMapper.selectList(wrapper);

        // 标记为已读
        if (!messages.isEmpty()) {
            List<Long> ids = messages.stream().map(ChatMessage::getMsgId).collect(Collectors.toList());
            baseMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                    .in(ChatMessage::getMsgId, ids)
                    .set(ChatMessage::getHasRead, Constant.MSG_READ));
        }

        return messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getProductInquirerIds(Long productId, Long sellerId) {
        if (productId == null) return Collections.emptyList();

        List<ChatMessage> messages = baseMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getProductId, productId)
                        .ne(ChatMessage::getFromUserId, sellerId)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );

        return messages.stream()
                .map(ChatMessage::getFromUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        BeanUtils.copyProperties(msg, dto);
        return dto;
    }
}
