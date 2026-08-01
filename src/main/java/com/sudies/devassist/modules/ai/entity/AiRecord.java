package com.sudies.devassist.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 生成记录。表 ai_record 仅 create_by/create_time（无更新/逻辑删除），
 * 故不继承 BaseEntity；createBy/createTime 由 MetaObjectHandler 自动填充。
 */
@Data
@TableName("ai_record")
public class AiRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long sprintId;

    /**
     * REQUIREMENT/SPRINT/TASK/TEST/BUG/DOCUMENT/STATISTICS
     */
    private String module;

    /**
     * REQUIREMENT_ANALYSIS/TASK_BREAKDOWN/...
     */
    private String aiType;

    private String inputContent;

    private String outputContent;

    /**
     * UNADOPTED/PARTIAL/FULL
     */
    private String status;

    private Long creatorId;

    private LocalDateTime adoptTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
