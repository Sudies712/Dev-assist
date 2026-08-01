package com.sudies.devassist.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sudies.devassist.modules.system.entity.Permission;
import com.sudies.devassist.modules.system.entity.Role;
import com.sudies.devassist.modules.system.entity.RolePermission;
import com.sudies.devassist.modules.system.entity.UserRole;
import com.sudies.devassist.modules.system.mapper.PermissionMapper;
import com.sudies.devassist.modules.system.mapper.RoleMapper;
import com.sudies.devassist.modules.system.mapper.RolePermissionMapper;
import com.sudies.devassist.modules.system.mapper.UserRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 权限加载：按 userId 从 DB 解析角色码与权限码。供 Sa-Token 的 StpInterface 与登录 VO 复用。
 */
@Service
public class PermissionService {

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    /**
     * 用户的角色码列表（如 ADMIN）
     */
    public List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectByIds(roleIds).stream()
                .map(Role::getRoleCode).filter(Objects::nonNull).toList();
    }

    /**
     * 用户的权限码列表（如 user:create）
     */
    public List<String> getPermissionCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct().toList();
        if (permIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectByIds(permIds).stream()
                .map(Permission::getPermCode).filter(Objects::nonNull).distinct().toList();
    }
}
