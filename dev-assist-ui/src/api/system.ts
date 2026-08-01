import {http} from "@/utils/http";

// ============================== 用户管理 ==============================

/** 用户列表（分页+筛选：username/status/roleId） */
export const getUserList = (params?: object) =>
    http.request<any>("get", "/users", {params});

/** 新增用户 */
export const createUser = (data: object) =>
    http.request<void>("post", "/users", {data});

/** 编辑用户（realName/email/phone） */
export const updateUser = (id: number, data: object) =>
    http.request<void>("put", `/users/${id}`, {data});

/** 启用/禁用用户 */
export const changeUserStatus = (id: number, status: string) =>
    http.request<void>("put", `/users/${id}/status`, {params: {status}});

/** 重置密码（返回默认密码明文） */
export const resetUserPassword = (id: number) =>
    http.request<string>("put", `/users/${id}/reset-password`);

/** 给用户分配角色 */
export const assignUserRoles = (id: number, roleIds: number[]) =>
    http.request<void>("put", `/users/${id}/roles`, {data: {roleIds}});

// ============================== 角色权限 ==============================

/** 角色列表（预置 4 个系统角色） */
export const getRoleList = () => http.request<any[]>("get", "/roles");

/** 查询角色已有的权限 id 列表 */
export const getRolePermissionIds = (id: number) =>
    http.request<number[]>("get", `/roles/${id}/permissions`);

/** 分配角色权限（全量覆盖） */
export const assignRolePermissions = (id: number, permissionIds: number[]) =>
    http.request<void>("put", `/roles/${id}/permissions`, {
        data: {permissionIds}
    });
