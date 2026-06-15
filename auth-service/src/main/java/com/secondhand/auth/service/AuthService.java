package com.secondhand.auth.service;

import com.secondhand.auth.dto.LoginRequest;
import com.secondhand.auth.dto.LoginResponse;
import com.secondhand.auth.dto.RegisterRequest;

public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     */
    LoginResponse register(RegisterRequest request);
}
