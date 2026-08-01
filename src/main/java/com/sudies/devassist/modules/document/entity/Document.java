package com.sudies.devassist.modules.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目文档。parseStatus 驱动异步摄入流程（UNPARSED→PARSING→PARSED/FAILED）。
 * 解析后切片入 document_chunk + Qdrant。无 version（schema 无该列）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    private Long projectId;

    private String name;

    /**
     * REQUIREMENT/DESIGN/API/TEST/MEETING/STANDARD/SPRINT_SUMMARY/PROJECT_SUMMARY/OTHER
     */
    private String type;

    private String description;

    /**
     * 相对存储路径，如 /document/{uuid}.{ext}
     */
    private String filePath;

    private Long fileSize;

    /**
     * UNPARSED/PARSING/PARSED/FAILED
     */
    private String parseStatus;

    /**
     * 上传人（不可空）
     */
    private Long uploaderId;
}
