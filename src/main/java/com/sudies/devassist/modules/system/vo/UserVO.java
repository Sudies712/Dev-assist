package com.sudies.devassist.modules.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private String status;
    /**
     * 角色码列表（如 ADMIN）
     */
    private List<String> roles;
    /**
     * 权限码列表（如 user:create）
     */
    private List<String> permissions;
}
