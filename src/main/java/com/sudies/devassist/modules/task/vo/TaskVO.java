package com.sudies.devassist.modules.task.vo;

import com.sudies.devassist.modules.task.entity.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务展示对象：在 Task 基础上补充关联展示字段（负责人/创建人/需求标题/迭代名）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskVO extends Task {

    /**
     * 负责人姓名
     */
    private String assigneeName;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 关联需求标题
     */
    private String requirementTitle;

    /**
     * 归属迭代名
     */
    private String sprintName;
}
