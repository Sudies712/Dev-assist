package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 权限类型。
 */
@Getter
public enum PermType {
    MENU("菜单"),
    BUTTON("按钮");

    private final String label;

    PermType(String label) {
        this.label = label;
    }
}
