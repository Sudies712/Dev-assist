package com.sudies.devassist.modules.testcase.vo;

import com.sudies.devassist.modules.testcase.entity.TestCase;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TestCaseVO extends TestCase {

    /**
     * 创建人姓名
     */
    private String creatorName;

    private String sprintName;

    private String requirementTitle;

    /**
     * 最近一次执行结果（UNEXECUTED/PASSED/FAILED/BLOCKED/SKIPPED），无执行记录为 null
     */
    private String lastResult;

    /**
     * 最近一次执行的实际结果描述
     */
    private String lastActualResult;

    /**
     * 最近一次执行时间
     */
    private java.time.LocalDateTime lastExecuteTime;
}
