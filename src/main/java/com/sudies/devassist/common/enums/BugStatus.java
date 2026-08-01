package com.sudies.devassist.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bug 状态机。迁移图见关键流程详细设计 §2.2。
 */
@Getter
public enum BugStatus {
    PENDING_CONFIRM("待确认"),
    PENDING_FIX("待修复"),
    FIXING("修复中"),
    PENDING_VERIFY("待验证"),
    CLOSED("已关闭"),
    REJECTED("拒绝修复");

    private final String label;

    BugStatus(String label) {
        this.label = label;
    }

    public Set<BugStatus> next() {
        return switch (this) {
            case PENDING_CONFIRM -> EnumSet.of(PENDING_FIX, REJECTED);         // REJECTED 须填原因
            case PENDING_FIX -> EnumSet.of(FIXING);
            case FIXING -> EnumSet.of(PENDING_VERIFY);                        // 须填修复说明
            case PENDING_VERIFY -> EnumSet.of(CLOSED, PENDING_FIX);            // PENDING_FIX = 验证失败
            case CLOSED -> EnumSet.of(PENDING_CONFIRM);                       // 测试人员重新打开
            case REJECTED -> EnumSet.of(PENDING_CONFIRM);                     // 测试人员重新激活
        };
    }
}
