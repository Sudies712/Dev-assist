package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * AI 功能类型（7 个助手）。
 */
@Getter
public enum AiType {
    REQUIREMENT_ANALYSIS("需求分析"),
    TASK_BREAKDOWN("任务拆解"),
    TEST_CASE_GENERATION("测试用例生成"),
    BUG_ANALYSIS("Bug 分析"),
    KNOWLEDGE_QA("知识库问答"),
    SPRINT_SUMMARY("迭代总结"),
    PROJECT_SUMMARY("项目总结");

    private final String label;

    AiType(String label) {
        this.label = label;
    }
}
