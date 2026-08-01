package com.sudies.devassist.modules.bug.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBugDTO {

    @Size(max = 255, message = "标题过长")
    private String title;

    private String description;

    private String stepsToReproduce;

    /**
     * MINOR/NORMAL/MAJOR/CRITICAL
     */
    private String severity;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;
}
