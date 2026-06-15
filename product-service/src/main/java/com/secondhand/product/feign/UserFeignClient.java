package com.secondhand.product.feign;

import com.secondhand.common.model.Result;
import com.secondhand.product.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 调用 user-service 获取用户信息
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserInfoDTO> getUserById(@PathVariable("id") Long id);
}
