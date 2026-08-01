package com.sudies.devassist.modules.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 迭代-需求关联（N:N）。记录加入/移出时间，驱动需求 SCHEDULED ↔ CONFIRMED 状态。
 */
@Data
@TableName("sprint_requirement")
public class SprintRequirement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sprintId;

    private Long requirementId;

    private Long operatorId;

    private LocalDateTime addTime;

    /**
     * 移出时间（空=当前在迭代中）
     */
    private LocalDateTime removeTime;

    private Long createBy;

    private LocalDateTime createTime;
}
