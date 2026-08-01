package com.sudies.devassist.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改成员项目角色（SRS §5.2.2 设置成员角色）。
 */
@Data
public class UpdateMemberRoleDTO {

    /**
     * 项目角色：OWNER / DEVELOPER / TESTER
     */
    @NotBlank(message = "项目角色不能为空")
    private String projectRole;
}
