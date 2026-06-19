package com.secondhand.chat.feign;

import com.secondhand.chat.dto.UserInfoDTO;
import com.secondhand.common.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 调用 user-service 获取用户信息（用于禁言检查等）
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserInfoDTO> getUserById(@PathVariable("id") Long id);
}
