import {http} from "@/utils/http";
import {getToken} from "@/utils/auth";
import {message} from "@/utils/message";

/** 文档列表（DataScope 按项目隔离） */
export const getDocumentList = (params?: object) =>
    http.request<any>("get", "/documents", {params});

/** 文档详情 */
export const getDocument = (id: number) =>
    http.request<any>("get", `/documents/${id}`);

/** 编辑文档（类型/描述） */
export const updateDocument = (id: number, data: object) =>
    http.request<void>("put", `/documents/${id}`, {data});

/** 删除文档 */
export const deleteDocument = (id: number) =>
    http.request<void>("delete", `/documents/${id}`);

/** 重新解析文档 */
export const reparseDocument = (id: number) =>
    http.request<void>("post", `/documents/${id}/reparse`);

/** 文档切片列表（解析结果） */
export const listChunks = (id: number) =>
    http.request<any[]>("get", `/documents/${id}/chunks`);

/**
 * 上传文档：后端为 multipart + 表单参数（file + projectId + type + description）。
 * 同缺陷附件，项目的 http(axios) 封装会把 FormData JSON 化，故用原生 fetch 直传。
 */
export async function uploadDocument(
    file: File,
    projectId: number | string,
    type: string,
    description: string
) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("projectId", String(projectId));
    formData.append("type", type);
    formData.append("description", description);
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token?.accessToken) headers["Authorization"] = `Bearer ${token.accessToken}`;
    const base = import.meta.env.VITE_API_BASE_URL || "/api";
    const res = await fetch(`${base}/documents`, {method: "POST", headers, body: formData});
    const json = await res.json();
    if (json.code !== 200) {
        message(json.message || "上传失败", {type: "error"});
        throw json;
    }
    return json.data as number;
}

/** 下载文档：content-disposition 存在即为文件，否则按错误 JSON 提示。 */
export async function downloadDocument(id: number, fileName: string) {
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token?.accessToken) headers["Authorization"] = `Bearer ${token.accessToken}`;
    const base = import.meta.env.VITE_API_BASE_URL || "/api";
    const res = await fetch(`${base}/documents/${id}/download`, {headers});
    const cd = res.headers.get("content-disposition") || "";
    if (!res.ok || !cd) {
        let msg = "下载失败";
        try {
            msg = (await res.json())?.message || msg;
        } catch {
            /* ignore */
        }
        message(msg, {type: "error"});
        return;
    }
    const blob = await res.blob();
    let name = fileName;
    const m = cd.match(/filename\*=UTF-8''([^;]+)/);
    if (m) name = decodeURIComponent(m[1]);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = name || "document";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}
