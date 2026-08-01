package com.sudies.devassist.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 是否勾选"X 天免登录"：true 时 refreshToken 签发 loginDay 天，否则会话级（与 accessToken 同寿命）
     */
    private Boolean remember;

    /**
     * 免登录天数（remember=true 时生效，默认 7）
     */
    private Integer loginDay;
}
