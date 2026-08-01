package com.sudies.devassist.modules.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRequirementDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    @NotBlank(message = "需求标题不能为空")
    private String title;

    private String description;
    private String type;
    /**
     * LOW / MEDIUM / HIGH / URGENT
     */
    private String priority;
    /**
     * 故事点
     */
    private Integer estimatedEffort;
}
