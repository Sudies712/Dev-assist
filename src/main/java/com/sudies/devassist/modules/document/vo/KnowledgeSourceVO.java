package com.sudies.devassist.modules.document.vo;

import lombok.Data;

@Data
public class KnowledgeSourceVO {

    /**
     * 来源文档名
     */
    private String sourceName;

    /**
     * 命中片段
     */
    private String snippet;
}
