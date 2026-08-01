package com.sudies.devassist.modules.requirement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 需求评审记录。
 */
@Data
@TableName("requirement_review")
public class RequirementReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long requirementId;

    private Long reviewerId;

    /**
     * 评审意见
     */
    private String opinion;

    /**
     * PASS / REJECT
     */
    private String result;

    private LocalDateTime reviewTime;

    private Long createBy;

    private LocalDateTime createTime;
}
