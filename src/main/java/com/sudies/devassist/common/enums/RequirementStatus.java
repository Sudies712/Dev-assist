package com.sudies.devassist.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 需求状态机。迁移图见关键流程详细设计 §2.2。
 */
@Getter
public enum RequirementStatus {
    PENDING_REVIEW("待评审"),
    CONFIRMED("已确认"),
    SCHEDULED("已排期"),
    DEVELOPING("开发中"),
    TESTING("测试中"),
    DONE("已完成"),
    CLOSED("已关闭");

    private final String label;

    RequirementStatus(String label) {
        this.label = label;
    }

    public Set<RequirementStatus> next() {
        return switch (this) {
            case PENDING_REVIEW -> EnumSet.of(CONFIRMED, CLOSED);              // CLOSED = 评审不通过
            case CONFIRMED -> EnumSet.of(SCHEDULED, CLOSED);                    // SCHEDULED = 加入迭代
            case SCHEDULED -> EnumSet.of(CONFIRMED, DEVELOPING);                // CONFIRMED = 移出迭代
            case DEVELOPING -> EnumSet.of(TESTING, SCHEDULED);                  // SCHEDULED = 退回
            case TESTING -> EnumSet.of(DONE, DEVELOPING);                       // DEVELOPING = 严重问题退回
            case DONE -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.of(PENDING_REVIEW);                         // 重新打开
        };
    }
}
