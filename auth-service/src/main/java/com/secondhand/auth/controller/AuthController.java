package com.secondhand.auth.controller;

import com.secondhand.auth.dto.LoginRequest;
import com.secondhand.auth.dto.LoginResponse;
import com.secondhand.auth.dto.RegisterRequest;
import com.secondhand.auth.service.AuthService;
import com.secondhand.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success(response);
    }
}
