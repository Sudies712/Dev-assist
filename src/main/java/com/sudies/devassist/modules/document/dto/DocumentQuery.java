package com.sudies.devassist.modules.document.dto;

import lombok.Data;

@Data
public class DocumentQuery {

    private Long projectId;
    private String type;
    /**
     * UNPARSED/PARSING/PARSED/FAILED
     */
    private String parseStatus;
    /**
     * 名称模糊搜索
     */
    private String keyword;

    private Integer page = 1;
    private Integer pageSize = 20;
}
