package com.sudies.devassist.modules.bug.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBugDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    /**
     * 归属迭代（可空）
     */
    private Long sprintId;

    private Long requirementId;
    private Long taskId;
    private Long testCaseId;

    @NotBlank(message = "Bug 标题不能为空")
    @Size(max = 255, message = "标题过长")
    private String title;

    private String description;

    /**
     * 复现步骤
     */
    private String stepsToReproduce;

    /**
     * MINOR/NORMAL/MAJOR/CRITICAL（默认 NORMAL）
     */
    private String severity;

    /**
     * LOW/MEDIUM/HIGH（默认 MEDIUM）
     */
    private String priority;

    /**
     * 修复人（可空，创建后可分配）
     */
    private Long assigneeId;
}
