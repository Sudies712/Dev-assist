package com.sudies.devassist.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    private String realName;
    private String email;
    private String phone;

    /**
     * 分配的系统角色 id 列表
     */
    private List<Long> roleIds;
}
