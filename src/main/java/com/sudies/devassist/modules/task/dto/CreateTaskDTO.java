package com.sudies.devassist.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTaskDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    @NotNull(message = "迭代 id 不能为空，任务须归属迭代")
    private Long sprintId;

    /**
     * 关联需求（可空）
     */
    private Long requirementId;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 255, message = "任务标题过长")
    private String title;

    private String description;

    /**
     * LOW/MEDIUM/HIGH（默认 MEDIUM）
     */
    private String priority;

    /**
     * 负责人（可空，创建后可分配）
     */
    private Long assigneeId;

    private LocalDate deadline;

    @Positive(message = "预估工时必须大于 0")
    private BigDecimal estimatedHours;
}
