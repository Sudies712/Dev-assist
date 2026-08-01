package com.sudies.devassist.modules.testcase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExecuteTestCaseDTO {

    /**
     * 执行所属迭代（可空）
     */
    private Long sprintId;

    /**
     * 实际结果
     */
    private String actualResult;

    /**
     * UNEXECUTED/PASSED/FAILED/BLOCKED/SKIPPED
     */
    @NotBlank(message = "执行结果不能为空")
    private String result;

    /**
     * FAILED 时是否联动创建 Bug
     */
    private Boolean submitBug;
}
