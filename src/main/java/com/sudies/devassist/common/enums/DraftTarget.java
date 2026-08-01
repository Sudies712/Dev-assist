package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * AI 草稿落点模块（草稿确认时按此分发到对应业务表，见关键流程详细设计 §3）。
 */
@Getter
public enum DraftTarget {
    REQUIREMENT("需求补充"),
    TASK("任务"),
    TESTCASE("测试用例"),
    BUG("Bug 备注"),
    SPRINT_SUMMARY("迭代总结"),
    PROJECT_SUMMARY("项目总结");

    private final String label;

    DraftTarget(String label) {
        this.label = label;
    }
}
