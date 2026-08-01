package com.sudies.devassist.modules.bug.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BugStatusChangeDTO {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 修复说明（FIXING→PENDING_VERIFY 必填，写入 fix_description）
     */
    private String fixDescription;

    /**
     * 验证失败原因（PENDING_VERIFY→PENDING_FIX 必填）
     */
    private String failReason;

    /**
     * 拒绝原因（→REJECTED 必填）
     */
    private String rejectReason;
}
