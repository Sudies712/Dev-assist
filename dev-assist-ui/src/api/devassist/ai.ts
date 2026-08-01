import {http} from "@/utils/http";

/** AI 建议项 / 草稿 item（与后端 DraftItem 一致） */
export interface DraftItem {
    targetModule?: string; // REQUIREMENT/TASK/TESTCASE/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY
    targetType?: string; // 任务 FRONTEND/BACKEND/DATABASE/TEST
    title?: string;
    content?: string;
    priority?: string; // LOW/MEDIUM/HIGH
    sprintId?: number;
}

/** AI 生成记录 */
export interface AiRecord {
    id: number;
    projectId: number;
    module: string;
    aiType: string;
    inputContent?: string;
    outputContent?: string;
    status: string; // UNADOPTED/PARTIAL/FULL
    creatorId?: number;
    createTime: string;
}

/** AI 草稿 */
export interface AiDraft {
    id: number;
    aiRecordId: number;
    projectId: number;
    targetModule: string; // REQUIREMENT/TASK/TESTCASE/BUG/SPRINT_SUMMARY/PROJECT_SUMMARY
    targetType?: string;
    draftContent: string; // JSON
    status: string; // PENDING_CONFIRM/ADOPTED/DISCARDED
    confirmTime?: string;
    createTime: string;
}

/** AI 助手生成（DeepSeek，冷启动慢，单独加长超时） */
export const generateAi = (assistant: string, projectId: number | string, contextId?: number | string) =>
    http.request<{ aiRecordId: number; items: DraftItem[]; rawOutput: string }>(
        "post",
        `/ai/${assistant}`,
        {data: {projectId, contextId: contextId || undefined}, timeout: 60000}
    );

/** 创建草稿（勾选的 items → 待二次确认草稿） */
export const createDrafts = (aiRecordId: number, projectId: number | string, items: DraftItem[]) =>
    http.request<number[]>("post", "/ai/drafts", {
        data: {aiRecordId, projectId, items},
        timeout: 30000
    });

/** 确认草稿（事务落业务表：TASK/TESTCASE 自动创建） */
export const confirmDraft = (id: number) =>
    http.request<number>("put", `/ai/drafts/${id}/confirm`);

/** 丢弃草稿 */
export const discardDraft = (id: number) =>
    http.request<void>("delete", `/ai/drafts/${id}`);

/** 项目 AI 生成记录 */
export const listAiRecords = (projectId: number | string) =>
    http.request<AiRecord[]>("get", "/ai/records", {params: {projectId}});

/** AI 记录下的草稿列表 */
export const listDrafts = (recordId: number) =>
    http.request<AiDraft[]>("get", `/ai/records/${recordId}/drafts`);
