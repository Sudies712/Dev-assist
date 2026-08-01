package com.sudies.devassist.modules.system.dto;

import lombok.Data;

@Data
public class UserQuery {

    private String username;
    private String status;
    private Long roleId;

    private Integer page = 1;
    private Integer pageSize = 20;
}
