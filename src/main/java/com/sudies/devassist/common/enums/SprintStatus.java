package com.sudies.devassist.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 迭代状态机。迁移图见关键流程详细设计 §2.2（不可回退；中止须删迭代退需求）。
 */
@Getter
public enum SprintStatus {
    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    ARCHIVED("已归档");

    private final String label;

    SprintStatus(String label) {
        this.label = label;
    }

    public Set<SprintStatus> next() {
        return switch (this) {
            case NOT_STARTED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(COMPLETED);
            case COMPLETED -> EnumSet.of(ARCHIVED);
            case ARCHIVED -> EnumSet.noneOf(SprintStatus.class);
        };
    }
}
