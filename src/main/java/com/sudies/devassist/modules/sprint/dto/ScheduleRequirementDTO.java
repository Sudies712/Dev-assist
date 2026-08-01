package com.sudies.devassist.modules.sprint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleRequirementDTO {

    @NotNull(message = "需求 id 不能为空")
    private Long requirementId;
}
