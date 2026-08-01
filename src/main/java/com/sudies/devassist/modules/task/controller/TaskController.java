package com.sudies.devassist.modules.task.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.task.dto.*;
import com.sudies.devassist.modules.task.service.TaskService;
import com.sudies.devassist.modules.task.vo.TaskCommentVO;
import com.sudies.devassist.modules.task.vo.TaskVO;
import com.sudies.devassist.modules.task.vo.WorkLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Resource
    private TaskService taskService;

    @Operation(summary = "任务列表（DataScope 按项目隔离）")
    @SaCheckPermission("task:read")
    @GetMapping
    public Result<PageResult<TaskVO>> page(TaskQuery q) {
        return Result.success(taskService.page(q));
    }

    @Operation(summary = "任务详情")
    @SaCheckPermission("task:read")
    @GetMapping("/{id}")
    public Result<TaskVO> detail(@PathVariable Long id) {
        return Result.success(taskService.detail(id));
    }

    @Operation(summary = "创建任务")
    @SaCheckPermission("task:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateTaskDTO dto) {
        return Result.success(taskService.create(dto));
    }

    @Operation(summary = "编辑任务（开发仅自己负责的；负责人任意）")
    @SaCheckPermission("task:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateTaskDTO dto) {
        taskService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除任务（仅待处理/已关闭）")
    @SaCheckPermission("task:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }

    @Operation(summary = "任务状态变更（开发退回须填 reason；测试人员可处理待测试任务）")
    @SaCheckPermission(value = {"task:change_status", "task:advance_test"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody @Valid TaskStatusChangeDTO dto) {
        taskService.changeStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "分配负责人（仅项目负责人）")
    @SaCheckPermission("task:assign")
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody @Valid AssignTaskDTO dto) {
        taskService.assign(id, dto);
        return Result.success();
    }

    @Operation(summary = "发表任务评论")
    @SaCheckPermission("task:read")
    @PostMapping("/{id}/comments")
    public Result<Long> addComment(@PathVariable Long id, @RequestBody @Valid CreateCommentDTO dto) {
        return Result.success(taskService.addComment(id, dto));
    }

    @Operation(summary = "任务评论列表")
    @SaCheckPermission("task:read")
    @GetMapping("/{id}/comments")
    public Result<List<TaskCommentVO>> listComments(@PathVariable Long id) {
        return Result.success(taskService.listComments(id));
    }

    @Operation(summary = "添加工作记录（开发记录自己负责任务的工时）")
    @SaCheckPermission("task:update")
    @PostMapping("/{id}/work-logs")
    public Result<Long> addWorkLog(@PathVariable Long id, @RequestBody @Valid CreateWorkLogDTO dto) {
        return Result.success(taskService.addWorkLog(id, dto));
    }

    @Operation(summary = "工作记录列表")
    @SaCheckPermission("task:read")
    @GetMapping("/{id}/work-logs")
    public Result<List<WorkLogVO>> listWorkLogs(@PathVariable Long id) {
        return Result.success(taskService.listWorkLogs(id));
    }
}
