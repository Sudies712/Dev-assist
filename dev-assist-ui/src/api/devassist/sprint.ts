import {http} from "@/utils/http";

export const getSprintList = (params?: object) =>
    http.request<any>("get", "/sprints", {params});

export const getSprint = (id: number) =>
    http.request<any>("get", `/sprints/${id}`);

export const createSprint = (data: object) =>
    http.request<any>("post", "/sprints", {data});

export const updateSprint = (id: number, data: object) =>
    http.request<any>("put", `/sprints/${id}`, {data});

export const deleteSprint = (id: number) =>
    http.request<any>("delete", `/sprints/${id}`);

export const changeSprintStatus = (id: number, status: string) =>
    http.request<any>("put", `/sprints/${id}/status`, {params: {status}});

/** 迭代进度 */
export const getSprintProgress = (id: number) =>
    http.request<any>("get", `/sprints/${id}/progress`);

/** 迭代燃尽图 */
export const getSprintBurndown = (id: number) =>
    http.request<any>("get", `/sprints/${id}/burndown`);
