package com.sudies.devassist.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户。对应 schema.sql user 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String username;

    /**
     * BCrypt 哈希（不序列化到响应）
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    /**
     * 用户状态码：ENABLED / DISABLED
     */
    private String status;

    /**
     * 连续登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 锁定截止时间（空=未锁定）
     */
    private LocalDateTime lockUntil;

    private LocalDateTime lastLoginTime;
}
