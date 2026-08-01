package com.sudies.devassist.modules.testcase.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTestCaseDTO {

    @Size(max = 255, message = "标题过长")
    private String title;

    private String preconditions;

    private String steps;

    private String expectedResult;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;
}
