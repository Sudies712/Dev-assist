import {system} from "@/router/enums";

export default {
    path: "/system",
    redirect: "/system/user",
    meta: {
        icon: "ri/settings-3-line",
        title: "系统管理",
        rank: system,
        roles: ["ADMIN"]
    },
    children: [
        {
            path: "/system/user",
            name: "SystemUser",
            component: () => import("@/views/system/user/index.vue"),
            meta: {
                title: "用户管理",
                icon: "ri/user-settings-line",
                roles: ["ADMIN"]
            }
        },
        {
            path: "/system/role",
            name: "SystemRole",
            component: () => import("@/views/system/role/index.vue"),
            meta: {
                title: "角色管理",
                icon: "ri/shield-user-line",
                roles: ["ADMIN"]
            }
        }
    ]
} satisfies RouteConfigsTable;
