package com.sudies.devassist.modules.sprint.dto;

import lombok.Data;

@Data
public class SprintQuery {

    private Long projectId;
    private String status;

    private Integer page = 1;
    private Integer pageSize = 20;
}
