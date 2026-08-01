package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 系统角色码（预置 4 个，不可新增）。
 */
@Getter
public enum RoleCode {
    ADMIN("系统管理员"),
    OWNER("项目负责人"),
    DEVELOPER("开发人员"),
    TESTER("测试人员");

    private final String label;

    RoleCode(String label) {
        this.label = label;
    }
}
