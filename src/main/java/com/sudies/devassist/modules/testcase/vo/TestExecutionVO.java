package com.sudies.devassist.modules.testcase.vo;

import com.sudies.devassist.modules.testcase.entity.TestExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TestExecutionVO extends TestExecution {

    /**
     * 执行人姓名
     */
    private String executorName;
}
