package com.sudies.devassist.modules.statistics.vo;

import lombok.Data;

/**
 * 成员负载（接口契约 §6 member-load）。
 */
@Data
public class MemberLoadVO {

    private Long userId;
    private String realName;
    /**
     * 负责任务数
     */
    private Long taskCount;
    /**
     * 修复 Bug 数
     */
    private Long bugCount;
    /**
     * 已完成任务数
     */
    private Long doneCount;
}
