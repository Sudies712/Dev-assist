package com.sudies.devassist.modules.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSprintDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    @NotBlank(message = "迭代名称不能为空")
    private String name;

    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
}
