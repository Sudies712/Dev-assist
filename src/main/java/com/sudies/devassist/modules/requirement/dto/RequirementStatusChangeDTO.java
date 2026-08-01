package com.sudies.devassist.modules.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequirementStatusChangeDTO {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 评审结果 PASS/REJECT（仅 PENDING_REVIEW → CONFIRMED/CLOSED 时填写，写入评审记录）
     */
    private String reviewResult;

    /**
     * 评审意见
     */
    private String reviewOpinion;
}
