package com.sudies.devassist.modules.sprint.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.sprint.dto.*;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.service.SprintService;
import com.sudies.devassist.modules.sprint.vo.SprintProgressVO;
import com.sudies.devassist.modules.task.service.TaskService;
import com.sudies.devassist.modules.task.vo.BurndownPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "迭代管理")
@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    @Resource
    private SprintService sprintService;

    @Resource
    private TaskService taskService;

    @Operation(summary = "迭代列表（DataScope 按项目隔离）")
    @SaCheckPermission("sprint:read")
    @GetMapping
    public Result<PageResult<Sprint>> page(SprintQuery q) {
        return Result.success(sprintService.page(q));
    }

    @Operation(summary = "迭代详情")
    @SaCheckPermission("sprint:read")
    @GetMapping("/{id}")
    public Result<Sprint> detail(@PathVariable Long id) {
        return Result.success(sprintService.detail(id));
    }

    @Operation(summary = "创建迭代")
    @SaCheckPermission("sprint:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateSprintDTO dto) {
        return Result.success(sprintService.create(dto));
    }

    @Operation(summary = "编辑迭代（进行中仅可改目标）")
    @SaCheckPermission("sprint:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateSprintDTO dto) {
        sprintService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除迭代（仅未开始）")
    @SaCheckPermission("sprint:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sprintService.delete(id);
        return Result.success();
    }

    @Operation(summary = "迭代状态变更（不可回退）")
    @SaCheckPermission("sprint:update")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        sprintService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "保存迭代总结")
    @SaCheckPermission("sprint:summary")
    @PutMapping("/{id}/summary")
    public Result<Void> saveSummary(@PathVariable Long id, @RequestBody @Valid SprintSummaryDTO dto) {
        sprintService.saveSummary(id, dto);
        return Result.success();
    }

    @Operation(summary = "迭代进度（需求完成率）")
    @SaCheckPermission("sprint:read")
    @GetMapping("/{id}/progress")
    public Result<SprintProgressVO> progress(@PathVariable Long id) {
        return Result.success(sprintService.progress(id));
    }

    @Operation(summary = "迭代需求规划：加入需求（CONFIRMED→SCHEDULED）")
    @SaCheckPermission("sprint:plan_requirement")
    @PostMapping("/{id}/requirements")
    public Result<Void> planRequirement(@PathVariable Long id, @RequestBody @Valid ScheduleRequirementDTO dto) {
        sprintService.planRequirement(id, dto);
        return Result.success();
    }

    @Operation(summary = "迭代需求规划：移出需求（SCHEDULED→CONFIRMED）")
    @SaCheckPermission("sprint:plan_requirement")
    @DeleteMapping("/{id}/requirements")
    public Result<Void> removeRequirement(@PathVariable Long id, @RequestParam Long requirementId) {
        sprintService.removeRequirement(id, requirementId);
        return Result.success();
    }

    @Operation(summary = "迭代内需求列表")
    @SaCheckPermission("sprint:read")
    @GetMapping("/{id}/requirements")
    public Result<List<Requirement>> listRequirements(@PathVariable Long id) {
        return Result.success(sprintService.listRequirements(id));
    }

    @Operation(summary = "迭代燃尽图（剩余任务数，SRS §5.4.4）")
    @SaCheckPermission("sprint:read")
    @GetMapping("/{id}/burndown")
    public Result<List<BurndownPointVO>> burndown(@PathVariable Long id) {
        return Result.success(taskService.burndown(id));
    }
}
