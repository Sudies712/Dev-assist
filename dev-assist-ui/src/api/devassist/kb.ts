import {http} from "@/utils/http";

/**
 * 知识库问答（RAG，按项目隔离检索）。返回答案 + 命中的引用来源。
 * 后端需 Qdrant 检索 + DeepSeek 生成，冷启动常 >10s，故单独加长超时（默认 10s 会中断）。
 */
export const askKnowledge = (projectId: number | string, question: string) =>
    http.request<{ answer: string; sources: { sourceName: string; snippet: string }[] }>(
        "post",
        "/kb/ask",
        {data: {projectId, question}, timeout: 60000}
    );
