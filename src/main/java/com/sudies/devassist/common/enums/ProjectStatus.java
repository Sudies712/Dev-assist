package com.sudies.devassist.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 项目状态机。迁移图见关键流程详细设计 §2.2。
 */
@Getter
public enum ProjectStatus {
    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    PAUSED("暂停"),
    COMPLETED("已完成"),
    ARCHIVED("已归档");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    /**
     * 允许的下一状态集合（终态返回空集）
     */
    public Set<ProjectStatus> next() {
        return switch (this) {
            case NOT_STARTED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(PAUSED, COMPLETED);
            case PAUSED -> EnumSet.of(IN_PROGRESS);
            case COMPLETED -> EnumSet.of(ARCHIVED);
            case ARCHIVED -> EnumSet.noneOf(ProjectStatus.class);
        };
    }
}
