package com.sudies.devassist.modules.bug.vo;

import com.sudies.devassist.modules.bug.entity.Bug;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bug 展示对象：在 Bug 基础上补充关联展示字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugVO extends Bug {

    /**
     * 修复人姓名
     */
    private String assigneeName;

    /**
     * 提交人姓名
     */
    private String reporterName;

    private String sprintName;

    private String requirementTitle;

    private String taskTitle;
}
