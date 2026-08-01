package com.sudies.devassist.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberDTO {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;

    /**
     * 项目角色：OWNER / DEVELOPER / TESTER
     */
    @NotBlank(message = "项目角色不能为空")
    private String projectRole;
}
