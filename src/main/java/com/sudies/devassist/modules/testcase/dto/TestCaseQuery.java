package com.sudies.devassist.modules.testcase.dto;

import lombok.Data;

@Data
public class TestCaseQuery {

    private Long projectId;
    private Long sprintId;
    private Long requirementId;
    private String priority;
    /**
     * 标题模糊搜索
     */
    private String keyword;

    private Integer page = 1;
    private Integer pageSize = 20;
}
