package com.secondhand.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.user.dto.ChangePasswordRequest;
import com.secondhand.user.dto.UserInfoDTO;
import com.secondhand.user.entity.User;
import com.secondhand.user.mapper.UserMapper;
import com.secondhand.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserInfoDTO getUserById(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserInfoDTO(user);
    }

    @Override
    public List<UserInfoDTO> listAllUsers() {
        List<User> users = baseMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .orderByAsc(User::getUserId)
        );
        return users.stream()
                .map(this::toUserInfoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        baseMapper.deleteById(userId);
        log.info("管理员删除用户，userId={}", userId);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_EMPTY);
        }

        User user = baseMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(DigestUtil.sha256Hex(request.getNewPassword().trim()));
        baseMapper.updateById(user);
        log.info("管理员修改用户密码，userId={}", request.getUserId());
    }

    @Override
    public void toggleMute(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        int newMuted = (user.getIsMuted() != null && user.getIsMuted() == Constant.USER_STATUS_MUTED)
                ? Constant.USER_STATUS_NORMAL
                : Constant.USER_STATUS_MUTED;
        user.setIsMuted(newMuted);
        baseMapper.updateById(user);
        log.info("{}用户，userId={}",
                newMuted == Constant.USER_STATUS_MUTED ? "禁言" : "解禁",
                userId);
    }

    @Override
    public List<UserInfoDTO> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = baseMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .in(User::getUserId, userIds)
        );
        return users.stream()
                .map(this::toUserInfoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserInfoDTO> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listAllUsers();
        }

        List<User> users = baseMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword.trim())
                        .or()
                        .eq(keyword.trim().matches("\\d+"), User::getUserId, Long.parseLong(keyword.trim()))
                        .orderByAsc(User::getUserId)
        );

        return users.stream()
                .map(this::toUserInfoDTO)
                .collect(Collectors.toList());
    }

    private UserInfoDTO toUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
