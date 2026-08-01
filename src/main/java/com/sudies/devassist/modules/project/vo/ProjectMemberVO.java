package com.sudies.devassist.modules.project.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberVO {

    private Long id;
    private Long userId;
    private String realName;
    /**
     * 项目角色：OWNER / DEVELOPER / TESTER
     */
    private String projectRole;
    private LocalDateTime joinTime;
}
