package com.secondhand.user.controller;

import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.model.Result;
import com.secondhand.user.dto.ChangePasswordRequest;
import com.secondhand.user.dto.UserInfoDTO;
import com.secondhand.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoDTO> getCurrentUser(HttpServletRequest request) {
        String userIdStr = request.getHeader(Constant.USER_ID_HEADER);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = Long.parseLong(userIdStr);
        return Result.success(userService.getUserById(userId));
    }

    /**
     * 根据 ID 获取指定用户信息
     */
    @GetMapping("/{id}")
    public Result<UserInfoDTO> getUserById(@PathVariable("id") Long id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 批量获取用户信息（内部服务调用）
     */
    @GetMapping("/batch")
    public Result<List<UserInfoDTO>> getUsersByIds(@RequestParam("ids") String ids) {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(userService.getUsersByIds(idList));
    }

    // ==================== 管理员接口 ====================

    /**
     * 获取所有用户列表（管理员）
     */
    @GetMapping("/admin/list")
    public Result<List<UserInfoDTO>> listAllUsers(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(userService.listAllUsers());
    }

    /**
     * 搜索用户（管理员，支持 ID 或用户名）
     */
    @GetMapping("/admin/search")
    public Result<List<UserInfoDTO>> searchUsers(@RequestParam("keyword") String keyword,
                                                  HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(userService.searchUsers(keyword));
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteUser(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAdmin(request);
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 修改用户密码（管理员）
     */
    @PutMapping("/admin/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest req,
                                       HttpServletRequest request) {
        checkAdmin(request);
        userService.changePassword(req);
        return Result.success();
    }

    /**
     * 禁言/解禁用户（管理员）
     */
    @PutMapping("/admin/mute/{id}")
    public Result<Void> toggleMute(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAdmin(request);
        userService.toggleMute(id);
        return Result.success();
    }

    // ==================== 内部辅助 ====================

    /**
     * 校验当前用户是否为管理员
     */
    private void checkAdmin(HttpServletRequest request) {
        String roleStr = request.getHeader(Constant.USER_ROLE_HEADER);
        if (roleStr == null || Integer.parseInt(roleStr) != Constant.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
