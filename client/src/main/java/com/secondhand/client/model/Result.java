package com.secondhand.client.model;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;

/**
 * 统一响应体（客户端版）
 */
@Data
public class Result {

    private int code;
    private String msg;
    private Object data;

    public boolean isSuccess() {
        return code == 200;
    }

    /**
     * 获取 data 并转为指定类型
     */
    public <T> T getData(Class<T> clazz) {
        if (data == null) {
            return null;
        }
        if (data instanceof JSONObject) {
            return JSONUtil.toBean((JSONObject) data, clazz);
        }
        // data is a string or other type
        return JSONUtil.toBean(JSONUtil.toJsonStr(data), clazz);
    }
}
