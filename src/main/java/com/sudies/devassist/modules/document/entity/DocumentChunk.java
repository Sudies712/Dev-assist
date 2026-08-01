package com.sudies.devassist.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档片段。表 document_chunk 仅 create_time（无逻辑删除/更新审计），
 * 故不继承 BaseEntity；createTime 由 MetaObjectHandler 自动填充。
 * vector_id 为 Qdrant 点 ID，摄入向量化后回写。
 */
@Data
@TableName("document_chunk")
public class DocumentChunk implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long projectId;

    /**
     * 片段文本
     */
    private String content;

    /**
     * Qdrant 点 ID
     */
    private String vectorId;

    private Integer chunkIndex;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
