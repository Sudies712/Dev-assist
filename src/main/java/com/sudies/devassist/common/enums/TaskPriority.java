package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 任务/Bug 优先级（3 级，二者共用）。
 */
@Getter
public enum TaskPriority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }
}
