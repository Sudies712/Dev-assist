package com.sudies.devassist.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskStatusChangeDTO {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 退回原因（仅 IN_PROGRESS→TODO 退回必填，Service 层校验）
     */
    private String reason;
}
