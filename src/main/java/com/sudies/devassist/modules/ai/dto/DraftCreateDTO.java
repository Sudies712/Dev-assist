package com.sudies.devassist.modules.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DraftCreateDTO {

    @NotNull(message = "aiRecordId 不能为空")
    private Long aiRecordId;

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    @NotEmpty(message = "items 不能为空")
    private List<DraftItem> items;
}
