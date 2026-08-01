package com.sudies.devassist.framework.mybatis;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：主键 + 审计字段（createTime/updateTime/createBy/updateBy）
 * + 乐观锁（version）+ 逻辑删除（isDeleted）。对应 schema.sql 通用字段约定。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
