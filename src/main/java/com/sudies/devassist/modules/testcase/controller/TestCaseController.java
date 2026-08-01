package com.sudies.devassist.modules.testcase.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.testcase.dto.CreateTestCaseDTO;
import com.sudies.devassist.modules.testcase.dto.ExecuteTestCaseDTO;
import com.sudies.devassist.modules.testcase.dto.TestCaseQuery;
import com.sudies.devassist.modules.testcase.dto.UpdateTestCaseDTO;
import com.sudies.devassist.modules.testcase.service.TestCaseService;
import com.sudies.devassist.modules.testcase.vo.ExecuteResultVO;
import com.sudies.devassist.modules.testcase.vo.TestCaseVO;
import com.sudies.devassist.modules.testcase.vo.TestExecutionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "测试用例管理")
@RestController
@RequestMapping("/api/test-cases")
public class TestCaseController {

    @Resource
    private TestCaseService testCaseService;

    @Operation(summary = "用例列表（DataScope 按项目隔离）")
    @SaCheckPermission("testcase:read")
    @GetMapping
    public Result<PageResult<TestCaseVO>> page(TestCaseQuery q) {
        return Result.success(testCaseService.page(q));
    }

    @Operation(summary = "用例详情")
    @SaCheckPermission("testcase:read")
    @GetMapping("/{id}")
    public Result<TestCaseVO> detail(@PathVariable Long id) {
        return Result.success(testCaseService.detail(id));
    }

    @Operation(summary = "创建用例（创建人=当前用户）")
    @SaCheckPermission("testcase:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateTestCaseDTO dto) {
        return Result.success(testCaseService.create(dto));
    }

    @Operation(summary = "编辑用例（创建人或项目负责人）")
    @SaCheckPermission("testcase:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateTestCaseDTO dto) {
        testCaseService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除用例")
    @SaCheckPermission("testcase:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return Result.success();
    }

    @Operation(summary = "执行用例（FAILED + submitBug 可联动创建 Bug）")
    @SaCheckPermission("testcase:execute")
    @PostMapping("/{id}/execute")
    public Result<ExecuteResultVO> execute(@PathVariable Long id, @RequestBody @Valid ExecuteTestCaseDTO dto) {
        return Result.success(testCaseService.execute(id, dto));
    }

    @Operation(summary = "用例执行历史")
    @SaCheckPermission("testcase:read")
    @GetMapping("/{id}/executions")
    public Result<List<TestExecutionVO>> listExecutions(@PathVariable Long id) {
        return Result.success(testCaseService.listExecutions(id));
    }

    @Operation(summary = "执行历史转缺陷（FAILED 记录，幂等返回已有缺陷）")
    @SaCheckPermission("testcase:execute")
    @PostMapping("/executions/{executionId}/convert-bug")
    public Result<ExecuteResultVO> convertToBug(@PathVariable Long executionId) {
        return Result.success(testCaseService.convertToBug(executionId));
    }
}
