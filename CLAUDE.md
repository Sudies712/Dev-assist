# dev-assist 项目指南

> 本文件是**索引 + 工作规则**。项目详情见 `README.md`，技术细则见 `doc/05-开发技术要点与踩坑.md`，按需打开对应文档，不要依赖本文件承载细节。

## 项目

软件项目开发辅助管理平台：Scrum 项目管理 + LangChain4j AI 辅助。
后端在本目录（`com.sudies.devassist`），前端在 `dev-assist-ui/`（vue-pure-admin v7）。

## 环境索引

- **MySQL**：Docker 容器 `mysql`（9.7.1），:3306，root / **123456**，库 `dev_assist`
- **Redis**：Docker 容器 `redis`（7-alpine），:6379，密码 **123456**
- **Qdrant**：Docker 容器 `qdrant`（AI 切片向量库）
- **Maven localRepository**：`C:\Others\Environments\Apache\apache-maven-3.9.11\repository`（settings.xml 已配置）
- JDK 17 / Maven 3.9.11（路径见全局 CLAUDE.md）
- MySQL 客户端：宿主无，用 `docker exec -i mysql mysql -u root -p123456 ...`

## 启动

```bash
export JAVA_HOME="/c/Others/Environments/Java/jdk-17"
export PATH="/c/Others/Environments/Apache/apache-maven-3.9.11/bin:$JAVA_HOME/bin:$PATH"
mvn -B -ntp compile      # 编译（~90 文件，0 warning）
mvn spring-boot:run      # 启动 8080
```

- 前端 dev server：`dev-assist-ui` 下 `npm run dev`，端口 **80**
- Swagger：http://localhost:8080/swagger-ui.html ；账号：admin / admin123

## 文档索引（按需打开，细则不在此文件）

| 文档                    | 内容                                    |
|-----------------------|---------------------------------------|
| `README.md`           | 项目详情：技术栈、功能、环境要求、启动、账号、版本历史           |
| `doc/01-需求分析文档.md`    | 需求规格（V2.1）：用户角色、业务流程、功能需求、权限矩阵、接口清单   |
| `doc/02-概要设计文档.md`    | 概要设计（V1.1）：总体架构、后端/AI/前端架构、数据库概要、安全设计 |
| `doc/03-接口字段契约.md`    | 接口字段契约：请求/响应字段定义                      |
| `doc/04-关键流程详细设计.md`  | 关键流程：认证授权与数据隔离、状态机、AI 草稿二次确认、RAG 管道   |
| `doc/05-开发技术要点与踩坑.md` | 技术栈适配要点、架构决策细则、前端踩坑                   |

## 工作规则

- **启动/验证用工具跑数据，不靠记忆**：环境、依赖、版本变更先用命令确认（如 `mvn help:evaluate`、`git log`），再下结论
- **改后端** → `mvn compile` + 重启 + curl 验证；**改前端** → `vite build` 验证 + chrome-devtools 验证；完成必须验证任务闭环。
- **删除/覆盖前先查看目标**：内容与描述不符或非预期时，先确认再动手
- **验证登录复用同一 token**：反复 curl 登录会覆盖 Redis 中按 userId 存的 refreshToken（is-share:false），导致浏览器旧
  refreshToken 续期 401（详见 doc/05 §三.7）
- **未授权不动手**：用户未明确说"开始做"前，不执行任何修改性操作；需用户确定的事（范围/方案/取舍）先让用户确定再动手；对不明确的行动，仅查询、指出问题、提问，不擅自推进
