import {http} from "@/utils/http";

export const getTaskList = (params?: object) =>
    http.request<any>("get", "/tasks", {params});

export const getTask = (id: number) =>
    http.request<any>("get", `/tasks/${id}`);

export const createTask = (data: object) =>
    http.request<any>("post", "/tasks", {data});

export const updateTask = (id: number, data: object) =>
    http.request<any>("put", `/tasks/${id}`, {data});

export const deleteTask = (id: number) =>
    http.request<any>("delete", `/tasks/${id}`);

/** 状态变更 {targetStatus, reason(退回必填)} */
export const changeTaskStatus = (id: number, data: object) =>
    http.request<any>("put", `/tasks/${id}/status`, {data});

/** 分配 */
export const assignTask = (id: number, data: object) =>
    http.request<any>("put", `/tasks/${id}/assign`, {data});

export const listTaskComments = (id: number) =>
    http.request<any>("get", `/tasks/${id}/comments`);

export const addTaskComment = (id: number, data: object) =>
    http.request<any>("post", `/tasks/${id}/comments`, {data});

export const listTaskWorkLogs = (id: number) =>
    http.request<any>("get", `/tasks/${id}/work-logs`);

export const addTaskWorkLog = (id: number, data: object) =>
    http.request<any>("post", `/tasks/${id}/work-logs`, {data});
