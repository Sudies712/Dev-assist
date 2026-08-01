package com.sudies.devassist.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目成员（决定用户在哪些项目可见）。DataScope 据此收敛 project_id。
 */
@Data
@TableName("project_member")
public class ProjectMember implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long userId;

    /**
     * 项目角色：OWNER / DEVELOPER / TESTER
     */
    private String projectRole;

    private LocalDateTime joinTime;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
