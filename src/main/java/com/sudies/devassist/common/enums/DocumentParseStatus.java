package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 文档解析状态。
 */
@Getter
public enum DocumentParseStatus {
    UNPARSED("未解析"),
    PARSING("解析中"),
    PARSED("已完成"),
    FAILED("失败");

    private final String label;

    DocumentParseStatus(String label) {
        this.label = label;
    }
}
