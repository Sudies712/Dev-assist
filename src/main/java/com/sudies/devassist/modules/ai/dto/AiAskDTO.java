package com.sudies.devassist.modules.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiAskDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    /**
     * 上下文 id（需求/迭代/Bug id，按助手语义不同）
     */
    private Long contextId;
}
