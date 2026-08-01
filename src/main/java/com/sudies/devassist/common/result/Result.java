package com.sudies.devassist.common.result;

import lombok.Data;

/**
 * 统一响应：{@code { code, message, data }}。
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result() {
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(ResultCode rc) {
        return error(rc.getCode(), rc.getMessage());
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
