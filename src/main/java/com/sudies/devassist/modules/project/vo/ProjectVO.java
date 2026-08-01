package com.sudies.devassist.modules.project.vo;

import com.sudies.devassist.modules.project.entity.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目视图对象：在实体基础上补充创建人姓名（详情/列表展示用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectVO extends Project {

    /**
     * 创建人姓名（realName 优先，其次 username）
     */
    private String creatorName;
}
