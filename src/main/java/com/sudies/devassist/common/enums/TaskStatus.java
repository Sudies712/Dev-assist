package com.sudies.devassist.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 任务状态机。迁移图见关键流程详细设计 §2.2。
 * 注：测试人员仅在 READY_FOR_TEST 流转（→ DONE / IN_PROGRESS），与 SRS §12.1 权限一致。
 */
@Getter
public enum TaskStatus {
    TODO("待处理"),
    IN_PROGRESS("进行中"),
    READY_FOR_TEST("待测试"),
    DONE("已完成"),
    CLOSED("已关闭");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public Set<TaskStatus> next() {
        return switch (this) {
            case TODO -> EnumSet.of(IN_PROGRESS, CLOSED);                      // CLOSED = 取消
            case IN_PROGRESS -> EnumSet.of(READY_FOR_TEST, TODO, CLOSED);      // TODO = 退回，须填原因
            case READY_FOR_TEST -> EnumSet.of(DONE, IN_PROGRESS);              // IN_PROGRESS = 测试不通过退回
            case DONE -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(TaskStatus.class);                  // 终态
        };
    }
}
