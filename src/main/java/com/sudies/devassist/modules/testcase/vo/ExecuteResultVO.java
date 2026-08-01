package com.sudies.devassist.modules.testcase.vo;

import lombok.Data;

/**
 * 用例执行结果：执行记录 id + 联动创建的 Bug id（submitBug=false 或非 FAILED 时为 null）。
 */
@Data
public class ExecuteResultVO {

    private Long executionId;

    /**
     * 联动创建的缺陷 id；null 表示未创建
     */
    private Long bugId;
}
