import {devassist} from "@/router/enums";

export default {
    path: "/devassist",
    redirect: "/devassist/dashboard",
    meta: {
        icon: "ri/dashboard-line",
        title: "DevAssist",
        rank: devassist
    },
    children: [
        {
            path: "/devassist/dashboard",
            name: "DevAssistDashboard",
            component: () => import("@/views/devassist/dashboard/index.vue"),
            meta: {
                title: "Scrum 控制台",
                keepAlive: true
            }
        },
        {
            path: "/devassist/project",
            name: "DevAssistProject",
            component: () => import("@/views/devassist/project/index.vue"),
            meta: {
                title: "项目管理",
                icon: "ri/project-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/requirement",
            name: "DevAssistRequirement",
            component: () => import("@/views/devassist/requirement/index.vue"),
            meta: {
                title: "需求管理",
                icon: "ri/file-list-3-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/sprint",
            name: "DevAssistSprint",
            component: () => import("@/views/devassist/sprint/index.vue"),
            meta: {
                title: "迭代管理",
                icon: "ri/repeat-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/task",
            name: "DevAssistTask",
            component: () => import("@/views/devassist/task/index.vue"),
            meta: {
                title: "任务管理",
                icon: "ri/task-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/bug",
            name: "DevAssistBug",
            component: () => import("@/views/devassist/bug/index.vue"),
            meta: {
                title: "缺陷管理",
                icon: "ri/bug-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/testcase",
            name: "DevAssistTestCase",
            component: () => import("@/views/devassist/testcase/index.vue"),
            meta: {
                title: "测试用例",
                icon: "ri/flask-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/document",
            name: "DevAssistDocument",
            component: () => import("@/views/devassist/document/index.vue"),
            meta: {
                title: "项目文档",
                icon: "ri/file-upload-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/kb",
            name: "DevAssistKb",
            component: () => import("@/views/devassist/kb/chat/index.vue"),
            meta: {
                title: "知识库",
                icon: "ri/chat-3-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        },
        {
            path: "/devassist/ai",
            name: "DevAssistAi",
            component: () => import("@/views/devassist/ai/index.vue"),
            meta: {
                title: "AI 助手",
                icon: "ri/magic-line",
                roles: ["OWNER", "DEVELOPER", "TESTER"]
            }
        }
    ]
} satisfies RouteConfigsTable;
