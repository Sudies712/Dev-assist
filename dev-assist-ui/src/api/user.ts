import {http} from "@/utils/http";
import {getToken} from "@/utils/auth";

export type UserResult = {
    code: number;
    message: string;
    data: {
        /** 头像 */
        avatar: string;
        /** 用户名 */
        username: string;
        /** 昵称 */
        nickname: string;
        /** 当前登录用户的角色 */
        roles: Array<string>;
        /** 按钮级别权限 */
        permissions: Array<string>;
        /** `token` */
        accessToken: string;
        /** 用于调用刷新`accessToken`的接口时所需的`token` */
        refreshToken: string;
        /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
        expires: Date;
    };
};

export type RefreshTokenResult = {
    code: number;
    message: string;
    data: {
        /** `token` */
        accessToken: string;
        /** 用于调用刷新`accessToken`的接口时所需的`token` */
        refreshToken: string;
        /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
        expires: Date;
    };
};

export type UserInfo = {
    /** 头像 */
    avatar: string;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname: string;
    /** 邮箱 */
    email: string;
    /** 联系电话 */
    phone: string;
    /** 简介 */
    description: string;
};

export type UserInfoResult = {
    code: number;
    message: string;
    data: UserInfo;
};

type ResultTable = {
    code: number;
    message: string;
    data?: {
        /** 列表数据 */
        list: Array<any>;
        /** 总条目数 */
        total?: number;
        /** 每页显示条目个数 */
        pageSize?: number;
        /** 当前页数 */
        currentPage?: number;
    };
};

/** 登录：后端返回 {accessToken,expires,refreshToken,user:{username,realName,roles,permissions}}，
 *  映射为模板 setToken 所需扁平结构（http 响应拦截器已脱信封 return data） */
export const getLogin = (data?: object) => {
    return http.request<any>("post", "/login", {data}).then((res: any) => ({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        expires: Date.now() + (res.expires || 7200) * 1000,
        roles: res.user?.roles || [],
        permissions: res.user?.permissions || [],
        username: res.user?.username || "",
        nickname: res.user?.realName || res.user?.username || "",
        avatar: res.user?.avatar || ""
    }));
};

/** 刷新 token：后端返回同登录结构 */
export const refreshTokenApi = (data?: object) => {
    return http.request<any>("post", "/refresh-token", {data}).then((res: any) => ({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        expires: Date.now() + (res.expires || 7200) * 1000,
        roles: res.user?.roles || [],
        permissions: res.user?.permissions || []
    }));
};

/** 账户设置-个人信息（GET /users/profile，响应拦截器已脱信封，返回 UserVO） */
export const getMine = () => http.request<any>("get", "/users/profile");

/** 账户设置-修改个人信息 */
export const updateMine = (data: object) =>
    http.request<any>("put", "/users/profile", {data});

/** 账户设置-修改密码 */
export const changeMyPassword = (data: object) =>
    http.request<any>("put", "/users/change-password", {data});

/**
 * 账户设置-上传头像：后端 multipart（file），返回最新 UserVO（含可访问 avatar URL）。
 * 同文档上传，http(axios) 封装会把 FormData JSON 化，故用原生 fetch 直传。
 */
export async function uploadAvatar(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token?.accessToken) headers["Authorization"] = `Bearer ${token.accessToken}`;
    const base = import.meta.env.VITE_API_BASE_URL || "/api";
    const res = await fetch(`${base}/users/avatar`, {
        method: "POST",
        headers,
        body: formData
    });
    const json = await res.json();
    if (json.code !== 200) throw json;
    return json.data;
}
