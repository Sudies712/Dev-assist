package com.sudies.devassist.modules.requirement.dto;

import lombok.Data;

@Data
public class UpdateRequirementDTO {

    private String title;
    private String description;
    private String type;
    private String priority;
    private Integer estimatedEffort;
}
