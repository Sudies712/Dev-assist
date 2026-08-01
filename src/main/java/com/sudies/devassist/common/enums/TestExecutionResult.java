package com.sudies.devassist.common.enums;

import lombok.Getter;

/**
 * 测试执行结果（含"未执行"隐含初始态）。
 */
@Getter
public enum TestExecutionResult {
    UNEXECUTED("未执行"),
    PASSED("通过"),
    FAILED("失败"),
    BLOCKED("阻塞"),
    SKIPPED("跳过");

    private final String label;

    TestExecutionResult(String label) {
        this.label = label;
    }
}
