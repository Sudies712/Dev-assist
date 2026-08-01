package com.sudies.devassist.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色（预置 4 个系统角色，build_in=1 不可删）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends BaseEntity {

    private String roleName;

    /**
     * 角色码：ADMIN / OWNER / DEVELOPER / TESTER
     */
    private String roleCode;

    private String description;

    private String status;

    private Integer buildIn;
}
