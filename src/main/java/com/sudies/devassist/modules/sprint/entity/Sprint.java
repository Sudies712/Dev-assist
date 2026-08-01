package com.sudies.devassist.modules.sprint.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 迭代。状态机见 {@link com.sudies.devassist.common.enums.SprintStatus}（不可回退）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sprint")
public class Sprint extends BaseEntity {

    private Long projectId;

    private String name;

    /**
     * 迭代目标
     */
    private String goal;

    /**
     * NOT_STARTED / IN_PROGRESS / COMPLETED / ARCHIVED
     */
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate actualEndDate;

    /**
     * 迭代总结（TEXT）
     */
    private String summary;

    @Version
    private Integer version;
}
