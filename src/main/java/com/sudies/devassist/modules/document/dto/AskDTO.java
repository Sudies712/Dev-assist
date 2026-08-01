package com.sudies.devassist.modules.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AskDTO {

    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    @NotBlank(message = "问题不能为空")
    private String question;
}
