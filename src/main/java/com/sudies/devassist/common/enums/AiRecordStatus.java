package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * AI 记录采纳状态。
 */
@Getter
public enum AiRecordStatus {
    UNADOPTED("未采纳"),
    PARTIAL("部分采纳"),
    FULL("全部采纳");

    private final String label;

    AiRecordStatus(String label) {
        this.label = label;
    }
}
