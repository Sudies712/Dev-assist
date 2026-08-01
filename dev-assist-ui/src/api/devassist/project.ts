import {http} from "@/utils/http";

/** 项目列表（分页） */
export const getProjectList = (params?: object) =>
    http.request<any>("get", "/projects", {params});

/** 项目详情 */
export const getProject = (id: number) =>
    http.request<any>("get", `/projects/${id}`);

/** 创建项目 */
export const createProject = (data: object) =>
    http.request<any>("post", "/projects", {data});

/** 编辑项目 */
export const updateProject = (id: number, data: object) =>
    http.request<any>("put", `/projects/${id}`, {data});

/** 删除项目 */
export const deleteProject = (id: number) =>
    http.request<any>("delete", `/projects/${id}`);

/** 项目状态变更 */
export const changeProjectStatus = (id: number, status: string) =>
    http.request<any>("put", `/projects/${id}/status`, {params: {status}});

/** 项目成员列表 */
export const getProjectMembers = (id: number) =>
    http.request<any>("get", `/projects/${id}/members`);

/** 添加成员 */
export const addProjectMember = (id: number, data: object) =>
    http.request<any>("post", `/projects/${id}/members`, {data});

/** 移除成员 */
export const removeProjectMember = (id: number, userId: number) =>
    http.request<any>("delete", `/projects/${id}/members/${userId}`);

/** 可添加的候选成员（系统启用用户中非本项目成员） */
export const getMemberCandidates = (id: number) =>
    http.request<any[]>("get", `/projects/${id}/members/candidates`);

/** 修改成员项目角色 */
export const changeMemberRole = (
    id: number,
    userId: number,
    projectRole: string
) =>
    http.request<void>("put", `/projects/${id}/members/${userId}/role`, {
        data: {projectRole}
    });
