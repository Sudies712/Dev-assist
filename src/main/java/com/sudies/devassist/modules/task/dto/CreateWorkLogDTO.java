package com.sudies.devassist.modules.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateWorkLogDTO {

    @NotNull(message = "工作内容不能为空")
    private String content;

    @NotNull(message = "耗时不能为空")
    @Positive(message = "耗时必须大于 0")
    private BigDecimal hours;

    @NotNull(message = "记录日期不能为空")
    private LocalDate logDate;
}
