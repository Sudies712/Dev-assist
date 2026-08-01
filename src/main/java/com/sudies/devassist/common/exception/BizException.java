package com.sudies.devassist.common.exception;

import com.sudies.devassist.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，携带 ResultCode 业务码。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
