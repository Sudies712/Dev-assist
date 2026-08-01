package com.sudies.devassist.modules.sprint.vo;

import lombok.Data;

@Data
public class SprintProgressVO {

    private long requirementTotal;
    private long requirementDone;
    /**
     * 需求完成率 0~1
     */
    private double requirementDoneRate;

    /**
     * 任务统计（task 模块就绪后填充）
     */
    private long taskTotal;
    private long taskDone;
}
