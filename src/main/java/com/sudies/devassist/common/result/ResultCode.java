package com.sudies.devassist.common.result;

import lombok.Getter;

/**
 * 统一响应状态码。对应接口契约 §1.2。
 * 5xx 段为业务自定义码（50001 起），与 HTTP 500 区分。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "ok"),
    BAD_REQUEST(400, "参数校验错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务端错误"),

    ACCOUNT_LOCKED(50001, "账号已锁定，请 30 分钟后再试"),
    ACCOUNT_DISABLED(50002, "账号已禁用"),
    OPTIMISTIC_LOCK_CONFLICT(50003, "数据已被他人修改，请刷新后重试"),
    ILLEGAL_STATUS_CHANGE(50004, "状态流转非法"),
    AI_SERVICE_UNAVAILABLE(50005, "AI 服务暂时不可用，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
