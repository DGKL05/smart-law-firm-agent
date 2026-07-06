# 严格生产型微服务改造设计

## 背景

当前项目是一个单体 Spring Boot 应用，前端是单 Vue 应用，Docker 仅包含 MySQL、Redis、MongoDB。后端已经按业务包划分出认证、律所、律师、预约、AI、后台、通知、日志等模块，但部署单元仍是一个应用。新的目标是把项目升级为可写进简历的准生产微服务项目，并新增独立律师端。

## 目标

- 后端改造成 Maven 多模块微服务项目，每个业务服务可独立构建、独立容器运行。
- 使用统一网关作为外部 API 入口，前端只访问网关。
- 使用服务注册与配置中心支撑服务发现和集中配置。
- 数据按服务划分独立 database/schema，默认使用一个 MySQL 容器承载多个 schema，后续可平滑拆成独立 MySQL 实例。
- Redis 作为分布式缓存，MongoDB 存储操作日志、AI 交互日志等文档型数据。
- 新增律师端角色与页面：律师管理本人资料、可预约时间、案件占用、预约列表、咨询回复。
- Docker Compose 一键启动前端、网关、各微服务、MySQL、Redis、MongoDB、注册配置中心。

## 非目标

- 本阶段不引入 Kubernetes。
- 本阶段不做支付、短信、电子签章等扩展能力。
- 本阶段不做复杂分布式事务中间件，跨服务写入通过接口编排和幂等约束控制。

## 服务拆分

### gateway-service

职责：
- 统一暴露 `/api/**`。
- 路由到后端服务。
- 处理 CORS。
- 承担基础限流和健康检查聚合。

### auth-service

职责：
- 登录、注册、JWT 签发与解析。
- 用户、角色、权限、律师账号绑定。
- 提供内部用户身份查询接口。

数据边界：
- `auth_db.sys_user`
- `auth_db.sys_role`
- `auth_db.sys_user_role`
- `auth_db.sys_menu`
- `auth_db.sys_role_menu`

### lawyer-service

职责：
- 律所、律师公开检索。
- 律所详情、律师详情。
- 律师个人资料维护。
- 律师与律所关联。

数据边界：
- `lawyer_db.law_firm`
- `lawyer_db.lawyer`
- `lawyer_db.lawyer_profile_audit`（预留）

### appointment-service

职责：
- 用户预约、取消预约。
- 律师端查看预约、确认或取消。
- 律师默认周一至周五可预约。
- 案件占用或已预约时段不可再次预约。
- 通知生成。

数据边界：
- `appointment_db.appointment`
- `appointment_db.lawyer_schedule`
- `appointment_db.legal_case`
- `appointment_db.consultation`
- `appointment_db.notification`

### ai-service

职责：
- Dify 工作流调用。
- 法律咨询对话。
- 聊天会话与消息历史。
- 智能助理触发预约/取消预约时调用 appointment-service。

数据边界：
- `ai_db.ai_chat_session`
- `ai_db.ai_chat_message`
- MongoDB：`agent_chat_log`

### admin-service

职责：
- 管理后台聚合接口。
- 统计首页。
- 管理视图编排。
- 操作日志查询。

说明：
- 后台 CRUD 能力优先通过各业务服务内部管理接口完成，admin-service 负责聚合和统一后台视图。

## 前端设计

前端仍使用一套 Vue 应用，但按角色拆入口：
- 公众端：主页、律所、律师、详情页、全局智能法律助理。
- 用户端：我的预约、我的咨询、我的案件、通知、聊天历史。
- 律师端：工作台、我的资料、可预约时间、预约管理、案件占用、咨询回复。
- 管理端：统计、用户、律所、律师、预约、案件、咨询、通知、日志。

路由守卫：
- `USER` 可访问用户端。
- `LAWYER` 可访问律师端。
- `ADMIN` 可访问管理端。

## 鉴权与服务调用

- 前端登录后保存 JWT。
- 网关透传 `Authorization`。
- 各服务使用统一 JWT 配置完成本地鉴权。
- 服务间调用通过 OpenFeign 或 WebClient，内部接口透传用户 ID、角色、请求 ID。
- 后续可在网关层增加签名或 mTLS，本阶段不做。

## 数据迁移

SQL 必须完整记录在 `sql/` 下：
- `sql/microservices/001_create_databases.sql`
- `sql/microservices/auth/001_schema.sql`
- `sql/microservices/lawyer/001_schema.sql`
- `sql/microservices/appointment/001_schema.sql`
- `sql/microservices/ai/001_schema.sql`
- `sql/microservices/admin/001_schema.sql`
- `sql/microservices/999_init_data.sql`

默认 Docker 使用一个 MySQL 容器创建多个 database：
- `smart_law_auth`
- `smart_law_lawyer`
- `smart_law_appointment`
- `smart_law_ai`
- `smart_law_admin`

## Docker 编排

Compose 服务：
- `smart-law-mysql`
- `smart-law-redis`
- `smart-law-mongodb`
- `smart-law-nacos`
- `smart-law-gateway`
- `smart-law-auth-service`
- `smart-law-lawyer-service`
- `smart-law-appointment-service`
- `smart-law-ai-service`
- `smart-law-admin-service`
- `smart-law-frontend`

## 错误处理

- 各服务统一返回 `ApiResponse`。
- 网关返回统一 401、403、404、503。
- 服务间调用失败时返回可读错误，不直接暴露下游堆栈。
- AI 调用保留较长超时，但前端显示加载状态和失败重试入口。

## 验证标准

- `mvn test` 通过。
- `npm run build` 通过。
- `docker compose config` 通过。
- `docker compose up -d --build` 后所有容器健康。
- 通过网关可访问登录、律所列表、律师列表、预约接口、AI 对话接口。
- 律师账号可登录律师端并查看自己的预约与可预约时间。
