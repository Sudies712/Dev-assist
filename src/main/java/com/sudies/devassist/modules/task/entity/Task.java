package com.sudies.devassist.modules.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务。状态机见 {@link com.sudies.devassist.common.enums.TaskStatus}。
 * <p>须归属迭代（sprint_id 非空）；可关联需求（requirement_id 可空）。
 * doneTime 在状态置 DONE 时写入，作为燃尽图（剩余任务数）的统计依据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {

    private Long projectId;

    /**
     * 任务须归属迭代
     */
    private Long sprintId;

    /**
     * 关联需求（可空）
     */
    private Long requirementId;

    private String title;

    private String description;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;

    /**
     * TODO/IN_PROGRESS/READY_FOR_TEST/DONE/CLOSED
     */
    private String status;

    /**
     * 负责人
     */
    private Long assigneeId;

    private LocalDate deadline;

    /**
     * 预估工时
     */
    private BigDecimal estimatedHours;

    /**
     * 实际工时
     */
    private BigDecimal actualHours;

    /**
     * 完成时间（→DONE 时写入，燃尽图统计依据；离开 DONE 时清空）
     */
    private LocalDateTime doneTime;

    @Version
    private Integer version;
}
