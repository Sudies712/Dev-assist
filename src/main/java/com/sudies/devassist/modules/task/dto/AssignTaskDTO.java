package com.sudies.devassist.modules.task.dto;

import lombok.Data;

@Data
public class AssignTaskDTO {

    /**
     * 新负责人；为 null 表示取消分配
     */
    private Long assigneeId;
}
