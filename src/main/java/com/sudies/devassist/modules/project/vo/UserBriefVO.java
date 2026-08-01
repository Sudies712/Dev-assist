package com.sudies.devassist.modules.project.vo;

import lombok.Data;

/**
 * 用户简要信息（用于项目成员候选选择，避免泄露 loginFailCount/lockUntil 等安全字段）。
 */
@Data
public class UserBriefVO {

    private Long id;
    private String username;
    private String realName;
}
