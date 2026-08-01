package com.sudies.devassist.modules.ai.vo;

import com.sudies.devassist.modules.ai.dto.DraftItem;
import lombok.Data;

import java.util.List;

@Data
public class AiResultVO {

    private Long aiRecordId;

    private List<DraftItem> items;

    /**
     * LLM 原始输出（调试/兜底）
     */
    private String rawOutput;
}
