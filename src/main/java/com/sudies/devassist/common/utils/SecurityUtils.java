package com.sudies.devassist.common.utils;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前登录用户工具（基于 Sa-Token）。供业务层获取当前用户 id、角色、权限。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 当前用户 id（未登录抛 NotLoginException）
     */
    public static long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 当前用户 id（未登录返回 null；异步线程无 Sa-Token 上下文时也返回 null，不抛异常）
     */
    public static Long currentUserIdOrNull() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception e) {
            // 异步线程（@Async）无 HTTP/Sa-Token 上下文，安全降级为 null
            return null;
        }
    }

    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    public static boolean hasPermission(String perm) {
        return StpUtil.hasPermission(perm);
    }
}
