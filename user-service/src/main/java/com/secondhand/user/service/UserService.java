package com.secondhand.user.service;

import com.secondhand.user.dto.ChangePasswordRequest;
import com.secondhand.user.dto.UserInfoDTO;

import java.util.List;

public interface UserService {

    /**
     * 根据 ID 获取用户信息
     */
    UserInfoDTO getUserById(Long userId);

    /**
     * 批量获取用户信息
     */
    List<UserInfoDTO> getUsersByIds(List<Long> userIds);

    /**
     * 搜索用户（管理员用，支持 ID 精确匹配或用户名模糊搜索）
     */
    List<UserInfoDTO> searchUsers(String keyword);

    /**
     * 获取所有用户列表（管理员用）
     */
    List<UserInfoDTO> listAllUsers();

    /**
     * 删除用户（管理员用）
     */
    void deleteUser(Long userId);

    /**
     * 修改用户密码（管理员用）
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * 禁言/解禁用户（管理员用）
     */
    void toggleMute(Long userId);
}
