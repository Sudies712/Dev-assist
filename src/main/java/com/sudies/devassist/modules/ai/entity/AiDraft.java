package com.sudies.devassist.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 草稿（二次确认）。表 ai_draft 有 update_time（ON UPDATE 自动）但无逻辑删除，
 * 故不继承 BaseEntity；createBy/createTime 由 MetaObjectHandler 自动填充，updateTime 由 DB 维护。
 */
@Data
@TableName("ai_draft")
public class AiDraft implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long aiRecordId;

    private Long projectId;

    /**
     * REQUIREMENT/TASK/TESTCASE/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY
     */
    private String targetModule;

    /**
     * 落点子类型（如任务 FRONTEND/BACKEND）
     */
    private String targetType;

    /**
     * 草稿内容（JSON，confirm 时反序列化为对应业务 DTO）
     */
    private String draftContent;

    /**
     * PENDING_CONFIRM/ADOPTED/DISCARDED
     */
    private String status;

    private Long creatorId;

    private LocalDateTime confirmTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
