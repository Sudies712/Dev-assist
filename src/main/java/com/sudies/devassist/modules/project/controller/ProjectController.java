package com.sudies.devassist.modules.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.project.dto.AddMemberDTO;
import com.sudies.devassist.modules.project.dto.CreateProjectDTO;
import com.sudies.devassist.modules.project.dto.ProjectQuery;
import com.sudies.devassist.modules.project.dto.UpdateProjectDTO;
import com.sudies.devassist.modules.project.service.ProjectService;
import com.sudies.devassist.modules.project.vo.ProjectMemberVO;
import com.sudies.devassist.modules.project.vo.ProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目管理")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Resource
    private ProjectService projectService;

    @Operation(summary = "项目列表（按当前用户可见过滤）")
    @SaCheckPermission("project:read")
    @GetMapping
    public Result<PageResult<ProjectVO>> page(ProjectQuery q) {
        return Result.success(projectService.page(q));
    }

    @Operation(summary = "项目详情")
    @SaCheckPermission("project:read")
    @GetMapping("/{id}")
    public Result<ProjectVO> detail(@PathVariable Long id) {
        return Result.success(projectService.detail(id));
    }

    @Operation(summary = "创建项目（创建人成为项目负责人）")
    @SaCheckPermission("project:create")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateProjectDTO dto) {
        return Result.success(projectService.create(dto));
    }

    @Operation(summary = "编辑项目")
    @SaCheckPermission("project:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateProjectDTO dto) {
        projectService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "项目状态变更（按状态机校验）")
    @SaCheckPermission("project:change_status")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        projectService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "项目成员列表")
    @SaCheckPermission("project:read")
    @GetMapping("/{id}/members")
    public Result<List<ProjectMemberVO>> listMembers(@PathVariable Long id) {
        return Result.success(projectService.listMembers(id));
    }

    @Operation(summary = "添加项目成员")
    @SaCheckPermission("project:member_manage")
    @PostMapping("/{id}/members")
    public Result<Void> addMember(@PathVariable Long id, @RequestBody @Valid AddMemberDTO dto) {
        projectService.addMember(id, dto);
        return Result.success();
    }

    @Operation(summary = "移除项目成员")
    @SaCheckPermission("project:member_manage")
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return Result.success();
    }

    @Operation(summary = "可添加的候选成员（系统用户中非本项目成员）")
    @SaCheckPermission("project:member_manage")
    @GetMapping("/{id}/members/candidates")
    public Result<List<com.sudies.devassist.modules.project.vo.UserBriefVO>> candidates(@PathVariable Long id) {
        return Result.success(projectService.listMemberCandidates(id));
    }

    @Operation(summary = "修改成员项目角色")
    @SaCheckPermission("project:member_manage")
    @PutMapping("/{id}/members/{userId}/role")
    public Result<Void> changeMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                         @RequestBody @Valid com.sudies.devassist.modules.project.dto.UpdateMemberRoleDTO dto) {
        projectService.changeMemberRole(id, userId, dto.getProjectRole());
        return Result.success();
    }
}
