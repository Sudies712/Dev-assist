package com.sudies.devassist.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试用例。优先级见 {@link com.sudies.devassist.common.enums.TestCasePriority}。
 * creator_id 为用例创建人（不可空）。无 version（schema 无该列）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_case")
public class TestCase extends BaseEntity {

    private Long projectId;

    /**
     * 归属迭代（可空）
     */
    private Long sprintId;

    private Long requirementId;

    private String title;

    /**
     * 前置条件
     */
    private String preconditions;

    /**
     * 测试步骤
     */
    private String steps;

    /**
     * 预期结果
     */
    private String expectedResult;

    /**
     * LOW/MEDIUM/HIGH
     */
    private String priority;

    /**
     * 创建人（不可空）
     */
    private Long creatorId;
}
