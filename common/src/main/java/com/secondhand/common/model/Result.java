package com.secondhand.common.model;

import cn.hutool.http.HttpStatus;
import com.secondhand.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 REST 响应体
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    private Result() {
    }

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ==================== 成功 ====================

    public static <T> Result<T> success() {
        return new Result<>(HttpStatus.HTTP_OK, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(HttpStatus.HTTP_OK, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(HttpStatus.HTTP_OK, msg, data);
    }

    // ==================== 失败 ====================

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(HttpStatus.HTTP_INTERNAL_ERROR, msg, null);
    }
}
