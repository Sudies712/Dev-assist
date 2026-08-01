package com.sudies.devassist.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试执行记录。表 test_execution 仅有 create_by/create_time/execute_time（无逻辑删除/更新审计），
 * 故不继承 BaseEntity；createBy/createTime 由 MetaObjectHandler 自动填充，executeTime 手动设置。
 */
@Data
@TableName("test_execution")
public class TestExecution implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long testCaseId;

    /**
     * 用例名称快照（执行时复制，历史反映当时用例内容）
     */
    private String title;

    /**
     * 前置条件快照
     */
    private String preconditions;

    /**
     * 测试步骤快照
     */
    private String steps;

    /**
     * 预期结果快照
     */
    private String expectedResult;

    /**
     * 执行人（不可空）
     */
    private Long executorId;

    private Long sprintId;

    /**
     * 实际结果
     */
    private String actualResult;

    /**
     * UNEXECUTED/PASSED/FAILED/BLOCKED/SKIPPED
     */
    private String result;

    /**
     * 联动缺陷 id（空=未转缺陷）
     */
    private Long bugId;

    private LocalDateTime executeTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
