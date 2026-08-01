package com.sudies.devassist.modules.task.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTaskDTO {

    @Size(max = 255, message = "任务标题过长")
    private String title;

    private String description;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;

    private LocalDate deadline;

    @Positive(message = "预估工时必须大于 0")
    private BigDecimal estimatedHours;

    @Positive(message = "实际工时必须大于 0")
    private BigDecimal actualHours;
}
