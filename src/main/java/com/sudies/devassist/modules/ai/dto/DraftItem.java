package com.sudies.devassist.modules.ai.dto;

import lombok.Data;

/**
 * AI 建议项（结构化输出元素 / 草稿 item）。
 * targetModule 决定草稿确认时分发到哪个业务表。
 */
@Data
public class DraftItem {

    /**
     * REQUIREMENT/TASK/TESTCASE/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY
     */
    private String targetModule;

    /**
     * 落点子类型，如任务 FRONTEND/BACKEND/DATABASE/TEST
     */
    private String targetType;

    private String title;

    private String content;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;

    /**
     * 任务草稿归属迭代（仅 TASK 落表用，前端创建草稿时填当前迭代）
     */
    private Long sprintId;
}
