package com.secondhand.client.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.secondhand.client.model.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * API 客户端（封装 Hutool HttpUtil）
 */
public class ApiClient {

    /** 后端网关地址 */
    private static final String BASE_URL = "http://127.0.0.1:8088";

    /** 当前登录用户的 Token */
    private static String token;

    /** 当前登录用户 ID */
    private static Long currentUserId;

    /** 当前用户角色 */
    private static Integer currentRole;

    /** 当前用户昵称 */
    private static String currentNickname;

    public static void setToken(String token) {
        ApiClient.token = token;
    }

    public static String getToken() {
        return token;
    }

    public static Long getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(Long currentUserId) {
        ApiClient.currentUserId = currentUserId;
    }

    public static Integer getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(Integer currentRole) {
        ApiClient.currentRole = currentRole;
    }

    public static String getCurrentNickname() {
        return currentNickname;
    }

    public static void setCurrentNickname(String currentNickname) {
        ApiClient.currentNickname = currentNickname;
    }

    /**
     * POST 请求（JSON body）
     */
    public static Result post(String path, Object body) {
        String url = BASE_URL + path;
        String jsonBody = JSONUtil.toJsonStr(body);

        HttpRequest request = HttpRequest.post(url)
                .body(jsonBody)
                .header("Content-Type", "application/json");
        addAuthHeader(request);

        try (HttpResponse response = request.execute()) {
            return parseResult(response.body());
        }
    }

    /**
     * GET 请求
     */
    public static Result get(String path) {
        return get(path, new HashMap<>());
    }

    /**
     * GET 请求（带参数）
     */
    public static Result get(String path, Map<String, Object> params) {
        String url = BASE_URL + path;
        HttpRequest request = HttpRequest.get(url)
                .form(params)
                .header("Content-Type", "application/json");
        addAuthHeader(request);

        try (HttpResponse response = request.execute()) {
            return parseResult(response.body());
        }
    }

    /**
     * PUT 请求
     */
    public static Result put(String path, Object body) {
        String url = BASE_URL + path;
        String jsonBody = JSONUtil.toJsonStr(body);

        HttpRequest request = HttpRequest.put(url)
                .body(jsonBody)
                .header("Content-Type", "application/json");
        addAuthHeader(request);

        try (HttpResponse response = request.execute()) {
            return parseResult(response.body());
        }
    }

    /**
     * DELETE 请求
     */
    public static Result delete(String path) {
        String url = BASE_URL + path;
        HttpRequest request = HttpRequest.delete(url)
                .header("Content-Type", "application/json");
        addAuthHeader(request);

        try (HttpResponse response = request.execute()) {
            return parseResult(response.body());
        }
    }

    /**
     * 添加认证头
     */
    private static void addAuthHeader(HttpRequest request) {
        if (token != null && !token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
    }

    /**
     * 解析响应结果
     */
    private static Result parseResult(String body) {
        Result result = new Result();
        try {
            JSONObject json = JSONUtil.parseObj(body);
            result.setCode(json.getInt("code"));
            result.setMsg(json.getStr("msg"));
            result.setData(json.get("data"));
        } catch (Exception e) {
            result.setCode(500);
            result.setMsg("服务器返回异常: " + (body != null ? body.substring(0, Math.min(body.length(), 100)) : "空响应"));
        }
        return result;
    }
}
