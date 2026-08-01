package com.sudies.devassist.modules.project.dto;

import lombok.Data;

@Data
public class ProjectQuery {

    private String name;
    private String status;

    private Integer page = 1;
    private Integer pageSize = 20;
}
