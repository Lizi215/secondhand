package com.secondhand.user.feign;

import com.secondhand.common.model.Result;
import com.secondhand.user.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端（供其他微服务调用）
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    /**
     * 根据 ID 获取用户信息
     */
    @GetMapping("/{id}")
    Result<UserInfoDTO> getUserById(@PathVariable("id") Long id);
}
