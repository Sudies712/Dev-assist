package com.sudies.devassist.modules.requirement.dto;

import lombok.Data;

@Data
public class RequirementQuery {

    private Long projectId;
    private String status;
    private String priority;
    private String keyword;

    private Integer page = 1;
    private Integer pageSize = 20;
}
