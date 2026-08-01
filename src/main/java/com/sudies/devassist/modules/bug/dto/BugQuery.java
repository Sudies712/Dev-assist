package com.sudies.devassist.modules.bug.dto;

import lombok.Data;

@Data
public class BugQuery {

    private Long projectId;
    private Long sprintId;
    private String status;
    private Long assigneeId;
    private Long reporterId;
    private String severity;
    /**
     * 标题模糊搜索
     */
    private String keyword;

    private Integer page = 1;
    private Integer pageSize = 20;
}
