# 智能律所平台

智能律所平台是《软件框架技术》课程大作业基础版本，当前实现 Spring Boot + Vue3 的业务系统、后台管理、数据库脚本、Redis 缓存入口和 MongoDB 操作日志预留。当前阶段不接入 Dify、北大法宝 MCP 或真实 AI 对话。

## 技术栈

- 后端：Java 17、Spring Boot、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、MongoDB、Maven、Lombok。
- 前端：Vue3、Vite、Element Plus、Vue Router、Pinia、Axios。
- 基础设施：Docker Compose 提供 MySQL、Redis、MongoDB。

## 功能模块

- 认证权限：注册、登录、JWT 鉴权、管理员与普通用户角色控制。
- 公共展示：律所列表/详情、律师列表/详情、省份与分类筛选、首页统计。
- 用户中心：我的咨询、案件、预约、通知。
- 管理后台：用户、律所、律师、咨询、案件、预约、通知、统计、日志。
- 缓存与日志：律所省份缓存、律师分类缓存、首页统计缓存；MongoDB 操作日志记录失败不阻断主流程。

## 项目结构

```text
src/main/java/com/example/dgkl
├── common
├── config
├── security
└── module
    ├── auth
    ├── user
    ├── lawfirm
    ├── lawyer
    ├── consultation
    ├── legalcase
    ├── appointment
    ├── notification
    ├── log
    └── agent
frontend
sql
docs
```

## 环境要求

- JDK 17+
- Maven Wrapper：使用仓库内 `mvnw.cmd`
- Node.js 18+
- MySQL、Redis、MongoDB 均默认连接 `localhost`

## 数据库脚本

脚本只记录在仓库中，不会自动执行。手动执行顺序：

1. `sql/001_create_database.sql`
2. `sql/002_init_schema.sql`
3. `sql/003_init_data.sql`

测试账号：

- 管理员：`admin / 123456`
- 普通用户：`user / 123456`

## 后端启动

```bat
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

默认后端地址：`http://localhost:8080`。

## 前端启动

```bat
cd frontend
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`。

## Docker Compose

```bat
docker compose up -d
```

容器会暴露：

- MySQL：`localhost:3307`，root/root
- Redis：`localhost:6379`
- MongoDB：`localhost:27017`

## 课程设计亮点

- 后端统一响应结构和全局异常处理。
- 用户端“我的”模块按 JWT 当前用户过滤数据，避免越权读取。
- 管理端 `/api/admin/**` 统一要求 ADMIN 角色。
- MyBatis-Plus 提供分页与逻辑删除基础能力。
- Redis 缓存围绕公开热点数据设计，Redis 不可用时回退 MySQL。
- MongoDB 操作日志使用拦截器记录，MongoDB 不可用时不影响 CRUD。

## 后续 Dify/MCP Agent 计划

当前只预留 `/api/agent/appointments` 和 `/api/agent/appointments/cancel` 普通 REST 接口。后续可在不破坏现有业务模块的前提下，将 Dify 工作流或 MCP 工具接入预约、咨询和法律知识检索入口。
