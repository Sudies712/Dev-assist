package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 文档类型。
 */
@Getter
public enum DocumentType {
    REQUIREMENT("需求文档"),
    DESIGN("设计文档"),
    API("接口文档"),
    TEST("测试文档"),
    MEETING("会议纪要"),
    STANDARD("开发规范"),
    SPRINT_SUMMARY("迭代总结"),
    PROJECT_SUMMARY("项目总结"),
    OTHER("其他");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }
}
