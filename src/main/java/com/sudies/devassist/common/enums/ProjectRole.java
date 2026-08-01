package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 项目角色（项目内职能）。
 */
@Getter
public enum ProjectRole {
    OWNER("项目负责人"),
    DEVELOPER("开发人员"),
    TESTER("测试人员");

    private final String label;

    ProjectRole(String label) {
        this.label = label;
    }
}
