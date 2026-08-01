package com.sudies.devassist.modules.task.vo;

import com.sudies.devassist.modules.task.entity.TaskComment;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskCommentVO extends TaskComment {

    private String userName;

    private String realName;
}
