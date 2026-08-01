package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 操作日志动作类型。
 */
@Getter
public enum OperationActionType {
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    STATUS_CHANGE("状态变更"),
    LOGIN("登录"),
    LOGOUT("退出");

    private final String label;

    OperationActionType(String label) {
        this.label = label;
    }
}
