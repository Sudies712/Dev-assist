package com.sudies.devassist.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。对应概要设计 §3.4：业务异常、参数校验、Sa-Token 未登录/无权限、乐观锁、兜底 500。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(ResultCode.UNAUTHORIZED));
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<Result<Void>> handleNoPerm(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        return Result.error(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        return Result.error(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return Result.error(ResultCode.ERROR);
    }
}
