package com.sudies.devassist.modules.task.dto;

import lombok.Data;

@Data
public class TaskQuery {

    private Long projectId;
    private Long sprintId;
    private Long requirementId;
    private String status;
    private Long assigneeId;
    /**
     * 标题模糊搜索
     */
    private String keyword;

    private Integer page = 1;
    private Integer pageSize = 20;
}
