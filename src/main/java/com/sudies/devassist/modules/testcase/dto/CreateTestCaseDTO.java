package com.sudies.devassist.modules.testcase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTestCaseDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    /**
     * 归属迭代（可空）
     */
    private Long sprintId;

    private Long requirementId;

    @NotBlank(message = "用例标题不能为空")
    @Size(max = 255, message = "标题过长")
    private String title;

    private String preconditions;

    private String steps;

    private String expectedResult;

    /**
     * LOW/MEDIUM/HIGH（默认 MEDIUM）
     */
    private String priority;
}
