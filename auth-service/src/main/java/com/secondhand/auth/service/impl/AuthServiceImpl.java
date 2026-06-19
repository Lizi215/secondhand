package com.secondhand.auth.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.auth.dto.LoginRequest;
import com.secondhand.auth.dto.LoginResponse;
import com.secondhand.auth.dto.RegisterRequest;
import com.secondhand.auth.entity.User;
import com.secondhand.auth.mapper.UserMapper;
import com.secondhand.auth.service.AuthService;
import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.USERNAME_EMPTY);
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_EMPTY);
        }

        // 查找用户
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername().trim())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验密码（SHA256 加密）
        String encryptedPwd = DigestUtil.sha256Hex(request.getPassword().trim());
        if (!user.getPassword().equals(encryptedPwd)) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 检查是否被禁言
        if (user.getIsMuted() != null && user.getIsMuted() == Constant.USER_STATUS_MUTED) {
            log.warn("被禁言用户登录，userId={}", user.getUserId());
        }

        // 生成 Token
        String token = JwtUtils.generateToken(user.getUserId(), user.getRole());
        log.info("用户登录成功，userId={}, username={}", user.getUserId(), user.getUsername());

        return new LoginResponse(user.getUserId(), token, user.getRole(), user.getNickname());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.USERNAME_EMPTY);
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_EMPTY);
        }

        username = username.trim();
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME, "用户名长度需在3~20个字符之间");
        }

        // 检查用户名是否已存在
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USER_EXIST);
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtil.sha256Hex(password));
        user.setRole(Constant.ROLE_USER);
        user.setNickname(request.getNickname() != null ? request.getNickname() : username);
        user.setPhone(request.getPhone());
        user.setIsMuted(Constant.USER_STATUS_NORMAL);

        baseMapper.insert(user);
        log.info("用户注册成功，userId={}, username={}", user.getUserId(), username);

        // 注册成功后自动登录，返回 Token
        String token = JwtUtils.generateToken(user.getUserId(), user.getRole());
        return new LoginResponse(user.getUserId(), token, user.getRole(), user.getNickname());
    }
}
