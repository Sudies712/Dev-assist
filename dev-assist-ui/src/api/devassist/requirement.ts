import {http} from "@/utils/http";

export const getRequirementList = (params?: object) =>
    http.request<any>("get", "/requirements", {params});

export const getRequirement = (id: number) =>
    http.request<any>("get", `/requirements/${id}`);

export const createRequirement = (data: object) =>
    http.request<any>("post", "/requirements", {data});

export const updateRequirement = (id: number, data: object) =>
    http.request<any>("put", `/requirements/${id}`, {data});

export const deleteRequirement = (id: number) =>
    http.request<any>("delete", `/requirements/${id}`);

/** 评审（PENDING_REVIEW → CONFIRMED/CLOSED） */
export const reviewRequirement = (id: number, data: object) =>
    http.request<any>("put", `/requirements/${id}/status`, {data});

/** 加入迭代（CONFIRMED → SCHEDULED） */
export const scheduleRequirement = (requirementId: number, sprintId: number) =>
    http.request<any>("post", `/sprints/${sprintId}/requirements`, {
        data: {requirementId}
    });

/** 移出迭代 */
export const unscheduleRequirement = (requirementId: number, sprintId: number) =>
    http.request<any>("delete", `/sprints/${sprintId}/requirements`, {
        params: {requirementId}
    });

/** 评审记录列表 */
export const getRequirementReviews = (id: number) =>
    http.request<any>("get", `/requirements/${id}/reviews`);
