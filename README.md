# Dev-assist

软件项目开发辅助管理平台：**Scrum 项目管理 + LangChain4j AI 辅助**。
面向中小型软件研发团队，覆盖项目、需求、迭代、任务、缺陷、测试、文档全流程管理，并集成知识库问答与 AI 助手。
前端基于 vue-pure-admin v7 二次开发，后端基于 Spring Boot 4.1 提供 REST 服务，AI 能力由 DeepSeek + RAG 知识库支撑。

## 技术栈

| 端     | 技术                                                                                                         |
|-------|------------------------------------------------------------------------------------------------------------|
| 后端    | Spring Boot 4.1.0 · Sa-Token 1.45.0（认证鉴权）· MyBatis-Plus 3.5.17 · springdoc-openapi · LangChain4j 1.0.0（AI） |
| AI/向量 | DeepSeek（LLM）· bge-small-zh-v1.5（本地 embedding）· Qdrant（向量库，RAG）                                            |
| 前端    | Vue 3.5 + vue-pure-admin v7（Element Plus / Pinia / ECharts）· Vite 8                                        |
| 存储    | MySQL（Docker）· Redis（Docker，会话与 Token）                                                                     |

## 环境要求

JDK 17、Maven 3.9.11、MySQL、Redis、Qdrant

## 环境变量

| 变量                 | 用途                             | 说明                                                    |
|--------------------|--------------------------------|-------------------------------------------------------|
| `DEEPSEEK_API_KEY` | DeepSeek LLM 调用密钥（AI 助手/知识库问答） | **必填**（AI 功能依赖），未设置时 AI 类接口将报错。环境变量名：DEEPSEEK_API_KEY |

## 部署

1.保证环境要求与环境变量配置完毕

2.使用`/sql/schema.sql`初始化数据库表

3.检查SpringBoot的yaml配置

4.启动

## 启动

```bash
mvn -B -ntp compile      # 编译
mvn spring-boot:run      # 启动

# Redis,需另外会话启动
redis-server

# 前端（80，浏览器访问 http://localhost，需另外会话启动）
cd dev-assist-ui && npm run dev
```

- Swagger 文档：http://localhost:8080/swagger-ui.html
- AI 功能启动前需配置密钥，见下方「环境变量」

## 功能模块

- **项目管理**：创建/编辑/归档、上下文状态流转（开始/暂停/结束/归档）、起止时间随状态自动记录、成员管理（角色/负载/候选）、详情统计摘要
- **需求管理**：需求评审、迭代排期（CONFIRMED→SCHEDULED）、7 态状态机、优先级
- **迭代管理**：迭代规划、需求排期/移出、燃尽图（ECharts）、迭代总结、线性状态机
- **任务管理**：5 态状态机（含退回填原因）、工时记录、任务评论、负责人分配
- **缺陷管理**：6 态状态机、附件上传/下载、缺陷历史、OWNER 分配、测试失败一键联动建缺陷
- **测试用例**：用例 CRUD、执行（结果+实际结果）、执行历史、失败联动建缺陷、执行状态列/详情抽屉
- **项目文档**：上传（multipart）/解析状态轮询/切片/下载，文档类型分类
- **知识库问答**：RAG 检索增强问答，回答附带引用来源
- **AI 助手**：需求分析、任务拆解、测试用例生成、缺陷分析、迭代/项目总结——生成→草稿→确认联动建数据
- **仪表盘**：项目下拉 + 6 统计卡 + 5 张 ECharts 图表
- **系统管理**（ADMIN 专属）：用户 CRUD/状态/重置密码/分配角色，角色与权限管理

## 测试账号（密码统一 admin123）

| 账号      | 角色            |
|---------|---------------|
| admin   | 系统管理员（系统管理专属） |
| owner1  | 项目负责人         |
| dev1    | 开发人员          |
| tester1 | 测试人员          |

## 目录结构

```
dev-assist/
├── src/                  # 后端（com.sudies.devassist）
│   ├── modules/          # system / project / requirement / sprint / task / bug / testcase / document / ai / statistics
│   ├── config/           # Sa-Token / Cors / MyBatisPlus / LangChain4j 等
│   └── resources/prompt/ # AI Prompt
├── dev-assist-ui/        # 前端（vue-pure-admin）
├── sql/schema.sql        # 建表脚本（23 表 + 初始数据）
└── pom.xml
```

