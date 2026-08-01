package com.sudies.devassist.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateProjectDTO {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    private String techStack;

    private LocalDate startDate;

    private LocalDate endDate;
}
