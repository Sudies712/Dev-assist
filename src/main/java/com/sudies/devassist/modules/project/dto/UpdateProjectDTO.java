package com.sudies.devassist.modules.project.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProjectDTO {

    private String name;
    private String description;
    private String techStack;
    private LocalDate startDate;
    private LocalDate endDate;
}
