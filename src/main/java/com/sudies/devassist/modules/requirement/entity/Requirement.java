package com.sudies.devassist.modules.requirement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求。可并发修改（乐观锁）。状态机见 {@link com.sudies.devassist.common.enums.RequirementStatus}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement")
public class Requirement extends BaseEntity {

    private Long projectId;

    private String title;

    private String description;

    /**
     * 需求类型
     */
    private String type;

    /**
     * LOW / MEDIUM / HIGH / URGENT
     */
    private String priority;

    /**
     * PENDING_REVIEW / CONFIRMED / SCHEDULED / DEVELOPING / TESTING / DONE / CLOSED
     */
    private String status;

    /**
     * 预估工作量（故事点）
     */
    private Integer estimatedEffort;

    private Long creatorId;

    @Version
    private Integer version;
}
