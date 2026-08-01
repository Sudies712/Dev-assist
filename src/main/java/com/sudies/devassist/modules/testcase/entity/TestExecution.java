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

    private LocalDateTime executeTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
