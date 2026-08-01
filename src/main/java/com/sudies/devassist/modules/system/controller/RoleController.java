package com.sudies.devassist.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.system.dto.AssignPermissionsDTO;
import com.sudies.devassist.modules.system.entity.Role;
import com.sudies.devassist.modules.system.entity.RolePermission;
import com.sudies.devassist.modules.system.mapper.RoleMapper;
import com.sudies.devassist.modules.system.mapper.RolePermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色权限")
@RestController
@RequestMapping("/api")
public class RoleController {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Operation(summary = "角色列表（预置 4 个系统角色）")
    @SaCheckPermission("role:read")
    @GetMapping("/roles")
    public Result<List<Role>> list() {
        return Result.success(roleMapper.selectList(
                Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId)));
    }

    @Operation(summary = "查询角色已有的权限 id 列表")
    @SaCheckPermission("role:read")
    @GetMapping("/roles/{id}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable Long id) {
        List<Long> pids = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id))
                .stream().map(RolePermission::getPermissionId).toList();
        return Result.success(pids);
    }

    @Operation(summary = "分配角色权限（全量覆盖）")
    @SaCheckPermission("role:assign_permission")
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody AssignPermissionsDTO dto) {
        rolePermissionMapper.delete(
                Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id));
        if (dto.getPermissionIds() != null) {
            for (Long pid : dto.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
        return Result.success();
    }
}
