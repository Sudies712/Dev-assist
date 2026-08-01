package com.sudies.devassist.modules.system.security;

import cn.dev33.satoken.stp.StpInterface;
import com.sudies.devassist.modules.system.service.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限/角色解析实现：Sa-Token 的 @SaCheckPermission / @SaCheckRole 通过本类取值。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private PermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermissionCodes(Long.valueOf(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.getRoleCodes(Long.valueOf(loginId.toString()));
    }
}
