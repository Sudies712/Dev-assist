import {http} from "@/utils/http";

/** 项目统计聚合 */
export const getProjectStatistics = (projectId: number | string) =>
    http.request<any>("get", `/projects/${projectId}/statistics`);

/** 项目成员负载 */
export const getMemberLoad = (projectId: number | string) =>
    http.request<any[]>("get", `/projects/${projectId}/member-load`);
