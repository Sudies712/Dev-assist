package com.sudies.devassist.modules.sprint.dto;

import lombok.Data;

@Data
public class UpdateSprintDTO {

    private String name;
    /**
     * 进行中的迭代仅允许改目标
     */
    private String goal;
}
