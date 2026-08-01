package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * AI 草稿状态。
 */
@Getter
public enum AiDraftStatus {
    PENDING_CONFIRM("待确认"),
    ADOPTED("已采纳"),
    DISCARDED("已丢弃");

    private final String label;

    AiDraftStatus(String label) {
        this.label = label;
    }
}
