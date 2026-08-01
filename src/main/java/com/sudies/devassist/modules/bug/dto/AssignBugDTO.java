package com.sudies.devassist.modules.bug.dto;

import lombok.Data;

@Data
public class AssignBugDTO {

    /**
     * 新修复人；为 null 表示取消分配
     */
    private Long assigneeId;
}
