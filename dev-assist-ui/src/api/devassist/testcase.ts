import {http} from "@/utils/http";

/** 用例列表（DataScope 按项目隔离） */
export const getTestCaseList = (params?: object) =>
    http.request<any>("get", "/test-cases", {params});

/** 用例详情 */
export const getTestCase = (id: number) =>
    http.request<any>("get", `/test-cases/${id}`);

/** 创建用例 */
export const createTestCase = (data: object) =>
    http.request<number>("post", "/test-cases", {data});

/** 编辑用例 */
export const updateTestCase = (id: number, data: object) =>
    http.request<void>("put", `/test-cases/${id}`, {data});

/** 删除用例 */
export const deleteTestCase = (id: number) =>
    http.request<void>("delete", `/test-cases/${id}`);

/**
 * 执行用例：FAILED + submitBug=true 时后端联动创建 Bug（标题「【用例失败】xxx」）。
 * 返回 { executionId, bugId }，bugId 非空表示已联动建缺陷。
 */
export const executeTestCase = (id: number, data: object) =>
    http.request<{ executionId: number; bugId: number | null }>(
        "post",
        `/test-cases/${id}/execute`,
        {data}
    );

/** 用例执行历史（按执行时间倒序） */
export const listExecutions = (id: number) =>
    http.request<any[]>("get", `/test-cases/${id}/executions`);

/**
 * 执行历史转缺陷（FAILED 记录，幂等：已有 bug_id 返回原缺陷）
 * 返回 { executionId, bugId }
 */
export const convertExecutionToBug = (executionId: number) =>
    http.request<{ executionId: number; bugId: number | null }>(
        "post",
        `/test-cases/executions/${executionId}/convert-bug`
    );
