package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 测试用例优先级。
 */
@Getter
public enum TestCasePriority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String label;

    TestCasePriority(String label) {
        this.label = label;
    }
}
