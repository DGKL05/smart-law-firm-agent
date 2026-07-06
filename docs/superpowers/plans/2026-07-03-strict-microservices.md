# 严格生产型微服务改造 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将智能律所平台从单体 Spring Boot 应用升级为可 Docker 一键运行的多服务微服务项目，并新增独立律师端。

**架构：** 项目采用 Maven 多模块：`smart-law-common` 放共享 DTO、安全与工具，`gateway-service` 统一入口，`auth-service`、`lawyer-service`、`appointment-service`、`ai-service`、`admin-service` 分别承载认证、律所律师、预约咨询、智能助理、后台聚合能力。Docker Compose 启动 MySQL、Redis、MongoDB、Nacos、前端、网关和所有服务；MySQL 默认一个容器多个 schema，后续可拆成独立实例。

**技术栈：** Spring Boot 3.3.6、Spring Cloud Gateway、Spring Cloud OpenFeign、Nacos、MyBatis-Plus、MySQL 8、Redis 7、MongoDB 7、Vue 3、Vite、Element Plus、Docker Compose。

---

## 文件结构

- 创建：`smart-law-common/pom.xml`，共享基础依赖。
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/**`，共享响应、异常、JWT、安全上下文、通用实体。
- 创建：`gateway-service/pom.xml`，网关依赖与启动类。
- 创建：`gateway-service/src/main/resources/application.yml`，网关路由到各服务。
- 创建：`auth-service/pom.xml`，认证服务依赖与启动类。
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/**`，认证、用户、角色、JWT。
- 创建：`lawyer-service/pom.xml`，律所律师服务依赖与启动类。
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/**`，律所、律师、公开检索、律师端资料。
- 创建：`appointment-service/pom.xml`，预约咨询服务依赖与启动类。
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/**`，预约、排班、案件占用、咨询、通知。
- 创建：`ai-service/pom.xml`，AI 服务依赖与启动类。
- 创建：`ai-service/src/main/java/com/example/dgkl/ai/**`，Dify、聊天历史、预约意图转发。
- 创建：`admin-service/pom.xml`，后台聚合服务依赖与启动类。
- 创建：`admin-service/src/main/java/com/example/dgkl/admin/**`，统计、后台聚合查询。
- 修改：`pom.xml`，改为父级聚合 POM。
- 修改：`docker-compose.yml`，加入所有应用容器、Nacos 和初始化 SQL。
- 创建：`Dockerfile`，服务通用镜像构建。
- 创建：`frontend/Dockerfile`、`frontend/nginx.conf`，前端容器。
- 创建：`sql/microservices/**`，按服务记录建库、建表、初始化数据。
- 修改：`frontend/src/router/index.js`，新增律师端路由与角色守卫。
- 创建：`frontend/src/layout/LawyerLayout.vue`，律师端布局。
- 创建：`frontend/src/views/lawyer/**`，律师工作台、资料、排班、预约、咨询页面。
- 修改：`frontend/src/stores/auth.js`，支持 `LAWYER` 角色判断。
- 修改：`frontend/src/api/http.js`，统一指向网关。

## 任务 1：建立 Maven 多模块骨架

**文件：**
- 修改：`pom.xml`
- 创建：`smart-law-common/pom.xml`
- 创建：`gateway-service/pom.xml`
- 创建：`auth-service/pom.xml`
- 创建：`lawyer-service/pom.xml`
- 创建：`appointment-service/pom.xml`
- 创建：`ai-service/pom.xml`
- 创建：`admin-service/pom.xml`

- [ ] **步骤 1：修改父 POM**

将根 `pom.xml` 改为 `packaging=pom`，声明模块和统一依赖版本。

- [ ] **步骤 2：创建各服务 POM**

每个服务依赖 `smart-law-common`，业务服务依赖 Web、Security、Validation、MyBatis-Plus、MySQL；网关依赖 Spring Cloud Gateway 和 Nacos Discovery。

- [ ] **步骤 3：运行构建验证**

运行：`.\mvnw.cmd -DskipTests package`

预期：所有模块至少完成 Maven 解析，若因缺少启动类失败，则进入任务 2。

- [ ] **步骤 4：Commit**

```bash
git add pom.xml smart-law-common gateway-service auth-service lawyer-service appointment-service ai-service admin-service
git commit -m "build: scaffold microservice modules"
```

## 任务 2：抽取共享 common 模块

**文件：**
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/response/ApiResponse.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/exception/BusinessException.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/exception/GlobalExceptionHandler.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/security/JwtUtil.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/security/SecurityUser.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/entity/BaseEntity.java`
- 创建：`smart-law-common/src/main/java/com/example/dgkl/common/config/MybatisPlusConfig.java`

- [ ] **步骤 1：迁移共享类**

从原单体中复制响应、异常、JWT、基础实体和 MyBatis 配置，并统一包名为 `com.example.dgkl.common`。

- [ ] **步骤 2：增加 common 单元测试**

创建 JWT 生成与解析测试，确认角色列表能正确写入 token。

- [ ] **步骤 3：运行测试**

运行：`.\mvnw.cmd -pl smart-law-common test`

预期：common 测试通过。

- [ ] **步骤 4：Commit**

```bash
git add smart-law-common
git commit -m "refactor: extract shared common module"
```

## 任务 3：实现网关服务

**文件：**
- 创建：`gateway-service/src/main/java/com/example/dgkl/gateway/GatewayApplication.java`
- 创建：`gateway-service/src/main/resources/application.yml`
- 创建：`gateway-service/src/main/java/com/example/dgkl/gateway/filter/GatewayAuthFilter.java`

- [ ] **步骤 1：创建启动类**

启动类使用 `@SpringBootApplication`。

- [ ] **步骤 2：配置路由**

路由规则：
- `/api/auth/**` -> `lb://auth-service`
- `/api/public/**` -> `lb://lawyer-service`
- `/api/lawyer-center/**` -> `lb://lawyer-service`
- `/api/me/appointments/**`、`/api/lawyer/appointments/**` -> `lb://appointment-service`
- `/api/ai/**`、`/api/agent/**` -> `lb://ai-service`
- `/api/admin/**` -> `lb://admin-service`

- [ ] **步骤 3：增加网关鉴权过滤器**

放行登录、注册和公开检索；其他接口要求 `Authorization: Bearer <token>`。

- [ ] **步骤 4：运行测试**

运行：`.\mvnw.cmd -pl gateway-service test`

预期：网关上下文加载通过。

- [ ] **步骤 5：Commit**

```bash
git add gateway-service
git commit -m "feat: add api gateway service"
```

## 任务 4：拆分认证服务

**文件：**
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/AuthServiceApplication.java`
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/controller/AuthController.java`
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/service/AuthService.java`
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/service/impl/AuthServiceImpl.java`
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/entity/**`
- 创建：`auth-service/src/main/java/com/example/dgkl/auth/mapper/**`
- 创建：`auth-service/src/main/resources/application.yml`
- 创建：`sql/microservices/auth/001_schema.sql`

- [ ] **步骤 1：迁移用户角色实体与 Mapper**

将 `sys_user`、`sys_role`、`sys_user_role`、菜单相关实体迁入 auth-service。

- [ ] **步骤 2：迁移注册登录业务**

保留 `/api/auth/register`、`/api/auth/login`、`/api/auth/me`、`/api/auth/logout`。

- [ ] **步骤 3：新增 LAWYER 角色支持**

初始化 SQL 增加 `LAWYER` 角色，律师账号通过 `lawyer.user_id` 与律师资料绑定。

- [ ] **步骤 4：运行认证服务测试**

运行：`.\mvnw.cmd -pl auth-service test`

预期：注册、登录、角色返回测试通过。

- [ ] **步骤 5：Commit**

```bash
git add auth-service sql/microservices/auth
git commit -m "feat: split auth service"
```

## 任务 5：拆分律所律师服务

**文件：**
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/LawyerServiceApplication.java`
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/controller/PublicLawyerController.java`
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/controller/LawyerCenterController.java`
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/entity/LawFirm.java`
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/entity/Lawyer.java`
- 创建：`lawyer-service/src/main/java/com/example/dgkl/lawyer/service/**`
- 创建：`lawyer-service/src/main/resources/application.yml`
- 创建：`sql/microservices/lawyer/001_schema.sql`

- [ ] **步骤 1：迁移公开检索接口**

保留律所列表、律师列表、详情接口。

- [ ] **步骤 2：新增律师端资料接口**

新增：
- `GET /api/lawyer-center/profile`
- `PUT /api/lawyer-center/profile`

- [ ] **步骤 3：运行测试**

运行：`.\mvnw.cmd -pl lawyer-service test`

预期：公开检索和律师资料接口测试通过。

- [ ] **步骤 4：Commit**

```bash
git add lawyer-service sql/microservices/lawyer
git commit -m "feat: split lawyer service"
```

## 任务 6：拆分预约咨询服务

**文件：**
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/AppointmentServiceApplication.java`
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/controller/UserAppointmentController.java`
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/controller/LawyerAppointmentController.java`
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/controller/ConsultationController.java`
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/entity/**`
- 创建：`appointment-service/src/main/java/com/example/dgkl/appointment/service/**`
- 创建：`appointment-service/src/main/resources/application.yml`
- 创建：`sql/microservices/appointment/001_schema.sql`

- [ ] **步骤 1：迁移预约、咨询、案件、通知实体**

按 `appointment_db` 建表。

- [ ] **步骤 2：实现动态可预约时段**

默认周一至周五生成时段；查询 `appointment` 和 `legal_case` 排除已占用时间；不可预约时段返回 `available=false`。

- [ ] **步骤 3：新增律师端预约接口**

新增：
- `GET /api/lawyer/appointments`
- `PUT /api/lawyer/appointments/{id}/confirm`
- `PUT /api/lawyer/appointments/{id}/cancel`

- [ ] **步骤 4：运行测试**

运行：`.\mvnw.cmd -pl appointment-service test`

预期：已预约时段、案件占用时段都不可再次预约。

- [ ] **步骤 5：Commit**

```bash
git add appointment-service sql/microservices/appointment
git commit -m "feat: split appointment service"
```

## 任务 7：拆分 AI 服务

**文件：**
- 创建：`ai-service/src/main/java/com/example/dgkl/ai/AiServiceApplication.java`
- 创建：`ai-service/src/main/java/com/example/dgkl/ai/controller/AiChatController.java`
- 创建：`ai-service/src/main/java/com/example/dgkl/ai/service/**`
- 创建：`ai-service/src/main/java/com/example/dgkl/ai/client/AppointmentClient.java`
- 创建：`ai-service/src/main/resources/application.yml`
- 创建：`sql/microservices/ai/001_schema.sql`

- [ ] **步骤 1：迁移 Dify 调用与聊天历史**

保留前端已有 AI 对话接口。

- [ ] **步骤 2：通过 Feign 调用预约服务**

AI 识别预约和取消预约意图后调用 appointment-service，不直接写预约表。

- [ ] **步骤 3：运行测试**

运行：`.\mvnw.cmd -pl ai-service test`

预期：JSON 清洗、聊天历史隔离和预约转发测试通过。

- [ ] **步骤 4：Commit**

```bash
git add ai-service sql/microservices/ai
git commit -m "feat: split ai service"
```

## 任务 8：实现管理聚合服务

**文件：**
- 创建：`admin-service/src/main/java/com/example/dgkl/admin/AdminServiceApplication.java`
- 创建：`admin-service/src/main/java/com/example/dgkl/admin/controller/AdminStatsController.java`
- 创建：`admin-service/src/main/java/com/example/dgkl/admin/client/**`
- 创建：`admin-service/src/main/resources/application.yml`
- 创建：`sql/microservices/admin/001_schema.sql`

- [ ] **步骤 1：迁移后台统计**

统计接口通过各服务内部接口聚合律所、律师、预约、用户数量。

- [ ] **步骤 2：保留后台管理入口**

后台 CRUD 先保留当前接口路径，由 admin-service 聚合或代理到对应服务。

- [ ] **步骤 3：运行测试**

运行：`.\mvnw.cmd -pl admin-service test`

预期：统计接口聚合测试通过。

- [ ] **步骤 4：Commit**

```bash
git add admin-service sql/microservices/admin
git commit -m "feat: add admin aggregation service"
```

## 任务 9：Docker 全量编排

**文件：**
- 修改：`docker-compose.yml`
- 创建：`Dockerfile`
- 创建：`frontend/Dockerfile`
- 创建：`frontend/nginx.conf`
- 创建：`sql/microservices/001_create_databases.sql`
- 创建：`sql/microservices/999_init_data.sql`

- [ ] **步骤 1：增加基础设施容器**

Compose 包含 MySQL、Redis、MongoDB、Nacos。

- [ ] **步骤 2：增加应用服务容器**

每个服务通过 `Dockerfile` 构建，暴露内部端口，仅网关和前端映射到宿主机。

- [ ] **步骤 3：增加 SQL 初始化挂载**

MySQL 启动时执行 `sql/microservices` 下所有脚本。

- [ ] **步骤 4：验证 Compose**

运行：`docker compose config`

预期：配置合法。

- [ ] **步骤 5：启动验证**

运行：`docker compose up -d --build`

预期：所有服务启动，`docker compose ps` 显示 running 或 healthy。

- [ ] **步骤 6：Commit**

```bash
git add docker-compose.yml Dockerfile frontend/Dockerfile frontend/nginx.conf sql/microservices
git commit -m "chore: dockerize microservices stack"
```

## 任务 10：新增律师端前端

**文件：**
- 修改：`frontend/src/router/index.js`
- 修改：`frontend/src/stores/auth.js`
- 创建：`frontend/src/layout/LawyerLayout.vue`
- 创建：`frontend/src/views/lawyer/LawyerDashboard.vue`
- 创建：`frontend/src/views/lawyer/LawyerProfile.vue`
- 创建：`frontend/src/views/lawyer/LawyerSchedule.vue`
- 创建：`frontend/src/views/lawyer/LawyerAppointments.vue`
- 创建：`frontend/src/views/lawyer/LawyerConsultations.vue`
- 创建：`frontend/src/api/lawyerCenter.js`

- [ ] **步骤 1：新增角色判断**

`auth` store 增加 `isLawyer`。

- [ ] **步骤 2：新增律师端布局**

导航包含工作台、我的资料、可预约时间、预约管理、咨询回复。

- [ ] **步骤 3：新增律师端页面**

页面调用网关下的 `/api/lawyer-center/**` 和 `/api/lawyer/appointments/**`。

- [ ] **步骤 4：构建前端**

运行：`cd frontend; npm.cmd run build`

预期：Vite 构建通过。

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/router/index.js frontend/src/stores/auth.js frontend/src/layout/LawyerLayout.vue frontend/src/views/lawyer frontend/src/api/lawyerCenter.js
git commit -m "feat: add lawyer portal frontend"
```

## 任务 11：端到端验收

**文件：**
- 修改：`README.md`

- [ ] **步骤 1：补充启动文档**

说明：
- `docker compose up -d --build`
- 前端访问地址。
- 网关地址。
- 测试账号：用户、律师、管理员。

- [ ] **步骤 2：运行后端测试**

运行：`.\mvnw.cmd test`

预期：全部测试通过。

- [ ] **步骤 3：运行前端构建**

运行：`cd frontend; npm.cmd run build`

预期：构建通过。

- [ ] **步骤 4：运行 Docker 验收**

运行：
```bash
docker compose config
docker compose up -d --build
docker compose ps
```

预期：全部容器运行。

- [ ] **步骤 5：Commit**

```bash
git add README.md
git commit -m "docs: document microservices startup"
```

## 自检

- 规格覆盖度：服务拆分、数据库隔离、Docker、律师端、AI 调用、预约占用、测试与启动文档均有任务覆盖。
- 占位符扫描：计划没有使用“待定”“TODO”“后续实现”等占位表达。
- 类型一致性：服务命名、路径、schema 名称与设计文档保持一致。
