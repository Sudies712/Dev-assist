package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * Bug 严重程度。
 */
@Getter
public enum BugSeverity {
    MINOR("轻微"),
    NORMAL("一般"),
    MAJOR("严重"),
    CRITICAL("致命");

    private final String label;

    BugSeverity(String label) {
        this.label = label;
    }
}
