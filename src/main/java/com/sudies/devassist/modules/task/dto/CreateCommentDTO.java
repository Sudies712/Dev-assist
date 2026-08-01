package com.sudies.devassist.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容过长")
    private String content;
}
