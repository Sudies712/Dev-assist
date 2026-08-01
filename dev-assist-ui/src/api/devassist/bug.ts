import {http} from "@/utils/http";
import {getToken} from "@/utils/auth";
import {message} from "@/utils/message";

/** 缺陷列表（DataScope 按项目隔离） */
export const getBugList = (params?: object) =>
    http.request<any>("get", "/bugs", {params});

/** 缺陷详情 */
export const getBug = (id: number) =>
    http.request<any>("get", `/bugs/${id}`);

/** 提交缺陷 */
export const createBug = (data: object) =>
    http.request<number>("post", "/bugs", {data});

/** 编辑缺陷 */
export const updateBug = (id: number, data: object) =>
    http.request<void>("put", `/bugs/${id}`, {data});

/** 删除缺陷（仅待确认/已拒绝） */
export const deleteBug = (id: number) =>
    http.request<void>("delete", `/bugs/${id}`);

/** 缺陷状态变更（状态机 + 3 处条件必填） */
export const changeBugStatus = (id: number, data: object) =>
    http.request<void>("put", `/bugs/${id}/status`, {data});

/** 分配修复人（仅项目负责人） */
export const assignBug = (id: number, assigneeId: number | null) =>
    http.request<void>("put", `/bugs/${id}/assign`, {
        data: {assigneeId}
    });

/** 附件列表 */
export const listAttachments = (bugId: number) =>
    http.request<any[]>("get", `/bugs/${bugId}/attachments`);

/**
 * 上传缺陷附件（multipart，后端字段名 file）。
 * 注意：项目的 http(axios) 封装默认 Content-Type=application/json，会把 FormData
 * 当普通对象 JSON 序列化成 {"file":{}}，故文件上传改用原生 fetch 直传——浏览器对
 * FormData body 自动设置 multipart/form-data; boundary=...，后端可正确解析。
 */
export async function uploadAttachment(bugId: number, file: File) {
    const formData = new FormData();
    formData.append("file", file);
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token?.accessToken) headers["Authorization"] = `Bearer ${token.accessToken}`;
    const base = import.meta.env.VITE_API_BASE_URL || "/api";
    const res = await fetch(`${base}/bugs/${bugId}/attachments`, {
        method: "POST",
        headers,
        body: formData
    });
    const json = await res.json();
    if (json.code !== 200) {
        message(json.message || "上传失败", {type: "error"});
        throw json;
    }
    return json.data as number;
}

/**
 * 下载附件：后端返回二进制流（成功带 Content-Disposition: attachment；异常以 JSON 体返回）。
 * 注意：后端对 .txt 等文件 Content-Type 会推断成 application/json，不能用 content-type
 * 判成败，改用「是否存在 content-disposition」区分文件/错误。用原生 fetch 以便读取响应头。
 */
export async function downloadAttachment(
    bugId: number,
    attachId: number,
    fileName: string
) {
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token?.accessToken) headers["Authorization"] = `Bearer ${token.accessToken}`;
    const base = import.meta.env.VITE_API_BASE_URL || "/api";
    const res = await fetch(
        `${base}/bugs/${bugId}/attachments/${attachId}/download`,
        {headers}
    );
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
    a.download = name || "attachment";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}
