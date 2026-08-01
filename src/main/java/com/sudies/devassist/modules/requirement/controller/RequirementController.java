package com.sudies.devassist.modules.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.requirement.dto.*;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.entity.RequirementReview;
import com.sudies.devassist.modules.requirement.service.RequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "需求管理")
@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    @Resource
    private RequirementService requirementService;

    @Operation(summary = "需求列表（DataScope 自动按项目隔离）")
    @SaCheckPermission("requirement:read")
    @GetMapping
    public Result<PageResult<Requirement>> page(RequirementQuery q) {
        return Result.success(requirementService.page(q));
    }

    @Operation(summary = "需求详情")
    @SaCheckPermission("requirement:read")
    @GetMapping("/{id}")
    public Result<Requirement> detail(@PathVariable Long id) {
        return Result.success(requirementService.detail(id));
    }

    @Operation(summary = "创建需求")
    @SaCheckPermission("requirement:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateRequirementDTO dto) {
        return Result.success(requirementService.create(dto));
    }

    @Operation(summary = "编辑需求")
    @SaCheckPermission("requirement:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateRequirementDTO dto) {
        requirementService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除需求（已排期/开发中不可删）")
    @SaCheckPermission("requirement:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requirementService.delete(id);
        return Result.success();
    }

    @Operation(summary = "需求状态变更（按状态机校验）")
    @SaCheckPermission("requirement:change_status")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody @Valid RequirementStatusChangeDTO dto) {
        requirementService.changeStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "需求评审记录列表")
    @SaCheckPermission("requirement:read")
    @GetMapping("/{id}/reviews")
    public Result<List<RequirementReview>> listReviews(@PathVariable Long id) {
        return Result.success(requirementService.listReviews(id));
    }

    @Operation(summary = "新增评审记录")
    @SaCheckPermission("requirement:review")
    @PostMapping("/{id}/reviews")
    public Result<Void> addReview(@PathVariable Long id, @RequestBody @Valid CreateReviewDTO dto) {
        requirementService.addReview(id, dto);
        return Result.success();
    }
}
