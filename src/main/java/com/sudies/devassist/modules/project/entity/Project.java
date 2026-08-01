package com.sudies.devassist.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 项目。可并发修改（含乐观锁 version）。状态机见 {@link com.sudies.devassist.common.enums.ProjectStatus}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    private String name;

    private String description;

    private String techStack;

    /**
     * 项目状态：NOT_STARTED / IN_PROGRESS / PAUSED / COMPLETED / ARCHIVED
     */
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 创建人（=项目负责人）
     */
    private Long creatorId;

    @Version
    private Integer version;
}
