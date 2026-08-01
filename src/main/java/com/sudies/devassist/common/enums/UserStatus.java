package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 用户状态。
 */
@Getter
public enum UserStatus {
    ENABLED("启用"),
    DISABLED("禁用");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }
}
