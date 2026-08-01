package com.sudies.devassist.modules.task.vo;

import com.sudies.devassist.modules.task.entity.WorkLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkLogVO extends WorkLog {

    private String userName;

    private String realName;
}
