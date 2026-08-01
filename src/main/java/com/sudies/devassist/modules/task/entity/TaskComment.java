package com.sudies.devassist.modules.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务评论。系统退回记录（status 变更退回原因）也写入此表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_comment")
public class TaskComment extends BaseEntity {

    private Long taskId;

    private Long userId;

    private String content;
}
