package com.sudies.devassist.modules.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SprintSummaryDTO {

    @NotBlank(message = "总结内容不能为空")
    private String summary;
}
