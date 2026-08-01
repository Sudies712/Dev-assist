package com.sudies.devassist.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.sudies.devassist.common.result.PageResult;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.system.dto.AssignRolesDTO;
import com.sudies.devassist.modules.system.dto.CreateUserDTO;
import com.sudies.devassist.modules.system.dto.UpdateUserDTO;
import com.sudies.devassist.modules.system.dto.UserQuery;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "用户列表（分页+筛选）")
    @SaCheckPermission("user:read")
    @GetMapping("/users")
    public Result<PageResult<User>> page(UserQuery q) {
        return Result.success(userService.page(q));
    }

    @Operation(summary = "新增用户")
    @SaCheckPermission("user:create")
    @PostMapping("/users")
    public Result<Void> create(@RequestBody @Valid CreateUserDTO dto) {
        userService.create(dto);
        return Result.success();
    }

    @Operation(summary = "编辑用户")
    @SaCheckPermission("user:update")
    @PutMapping("/users/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateUserDTO dto) {
        userService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @SaCheckPermission("user:update")
    @PutMapping("/users/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        userService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "重置密码（返回默认密码）")
    @SaCheckPermission("user:reset_password")
    @PutMapping("/users/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id) {
        return Result.success(userService.resetPassword(id));
    }

    @Operation(summary = "给用户分配角色")
    @SaCheckPermission("user:update")
    @PutMapping("/users/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return Result.success();
    }
}
