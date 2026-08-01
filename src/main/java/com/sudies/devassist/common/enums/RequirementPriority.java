package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 需求优先级（4 级）。
 */
@Getter
public enum RequirementPriority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    URGENT("紧急");

    private final String label;

    RequirementPriority(String label) {
        this.label = label;
    }
}
