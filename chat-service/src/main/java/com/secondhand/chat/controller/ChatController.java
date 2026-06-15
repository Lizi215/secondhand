package com.secondhand.chat.controller;

import com.secondhand.chat.dto.ChatMessageDTO;
import com.secondhand.chat.dto.SendMessageRequest;
import com.secondhand.chat.service.ChatService;
import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 聊天控制器
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<ChatMessageDTO> sendMessage(@RequestBody SendMessageRequest request,
                                              HttpServletRequest servletRequest) {
        Long fromUserId = getUserId(servletRequest);
        return Result.success(chatService.sendMessage(fromUserId, request));
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history")
    public Result<List<ChatMessageDTO>> getHistory(@RequestParam("userId") Long otherUserId,
                                                   @RequestParam(value = "productId", required = false) Long productId,
                                                   HttpServletRequest servletRequest) {
        Long myUserId = getUserId(servletRequest);
        return Result.success(chatService.getHistory(myUserId, otherUserId, productId));
    }

    /**
     * 获取询问某商品的买家 ID 列表
     */
    @GetMapping("/product-inquirers/{productId}")
    public Result<List<Long>> getProductInquirers(@PathVariable("productId") Long productId,
                                                   HttpServletRequest servletRequest) {
        Long sellerId = getUserId(servletRequest);
        return Result.success(chatService.getProductInquirerIds(productId, sellerId));
    }

    /**
     * 轮询新消息
     */
    @GetMapping("/poll")
    public Result<List<ChatMessageDTO>> pollNewMessages(
            @RequestParam(value = "lastMsgId", required = false, defaultValue = "0") Long lastMsgId,
            @RequestParam("otherUserId") Long otherUserId,
            HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(chatService.pollNewMessages(userId, otherUserId, lastMsgId));
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(Constant.USER_ID_HEADER);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.parseLong(userIdStr);
    }
}
