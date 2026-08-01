package com.sudies.devassist.modules.bug.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷。状态机见 {@link com.sudies.devassist.common.enums.BugStatus}。
 * <p>reporter 为提交人（不可空）；assignee 为修复人（分配后写入）。
 * fixDescription 在状态置 PENDING_VERIFY（修复完成）时写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug")
public class Bug extends BaseEntity {

    private Long projectId;

    /**
     * 归属迭代（可空）
     */
    private Long sprintId;

    private Long requirementId;

    private Long taskId;

    private Long testCaseId;

    private String title;

    private String description;

    /**
     * 复现步骤
     */
    private String stepsToReproduce;

    /**
     * MINOR/NORMAL/MAJOR/CRITICAL
     */
    private String severity;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;

    /**
     * PENDING_CONFIRM/PENDING_FIX/FIXING/PENDING_VERIFY/CLOSED/REJECTED
     */
    private String status;

    /**
     * 修复人
     */
    private Long assigneeId;

    /**
     * 提交人（不可空）
     */
    private Long reporterId;

    /**
     * 修复说明（FIXING→PENDING_VERIFY 时写入）
     */
    private String fixDescription;

    @Version
    private Integer version;
}
