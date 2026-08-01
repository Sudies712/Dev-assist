package com.sudies.devassist.modules.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作记录。表 work_log 仅有 create_by/create_time（无逻辑删除/更新审计字段），
 * 故不继承 BaseEntity；createBy/createTime 由 {@link com.sudies.devassist.framework.mybatis.MetaObjectHandlerImpl} 自动填充。
 */
@Data
@TableName("work_log")
public class WorkLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    /**
     * 归属迭代
     */
    private Long sprintId;

    /**
     * 工作内容
     */
    private String content;

    /**
     * 耗时
     */
    private BigDecimal hours;

    private LocalDate logDate;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
