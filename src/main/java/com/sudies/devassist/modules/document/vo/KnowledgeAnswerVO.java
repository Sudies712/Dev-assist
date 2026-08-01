package com.sudies.devassist.modules.document.vo;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeAnswerVO {

    /**
     * LLM 生成答案；命中为空时为"未找到相关文档"提示
     */
    private String answer;

    /**
     * 命中的引用来源
     */
    private List<KnowledgeSourceVO> sources;
}
