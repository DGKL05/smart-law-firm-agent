# 智能律所平台

智能律所平台已经升级为微服务项目，包含公众端、用户端、律师端、管理端和全局智能法律助理。后端不再保留单体 Spring Boot 入口，所有业务能力由独立微服务通过网关提供。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Cloud Gateway、OpenFeign、Nacos、Spring Security、JWT、MyBatis-Plus
- 数据：MySQL 8、Redis 7、MongoDB 7
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios
- 部署：Docker Compose

## 微服务模块

```text
smart-law-common        共享响应、异常、JWT、安全、MyBatis 配置
gateway-service         API 网关，统一暴露 /api/**
auth-service            登录、注册、用户、角色、JWT
lawyer-service          律所、律师、律师端资料、公开检索
appointment-service     预约、取消预约、律师排班、案件占用
ai-service              Dify 调用、法律咨询对话、聊天历史
admin-service           管理后台聚合接口
frontend                Vue 前端，包含公众端、用户端、律师端、管理端
sql/microservices       微服务数据库脚本
```

## 数据库划分

Docker 默认使用一个 MySQL 容器承载多个 schema：

- `smart_law_auth`
- `smart_law_lawyer`
- `smart_law_appointment`
- `smart_law_ai`
- `smart_law_admin`

SQL 脚本全部记录在 `sql/microservices`，MySQL 容器首次初始化时会自动执行。

## 启动方式

推荐直接使用 Docker：

```bat
docker compose up -d --build
```

访问地址：

- 前端：http://localhost:5173
- API 网关：http://localhost:8080
- Nacos：http://localhost:8848/nacos
- MySQL：localhost:3307，root/root
- Redis：localhost:6379
- MongoDB：localhost:27017

测试账号：

- 管理员：`admin / 123456`
- 用户：`user / 123456`
- 律师：`lawyer / 123456`

如需启用云端 Dify，请在启动前设置：

```bat
set DIFY_API_KEY=你的Dify应用ServiceAPIKey
docker compose up -d --build
```

## 本地构建

后端：

```bat
mvnw.cmd test
mvnw.cmd -DskipTests package
```

前端：

```bat
cd frontend
npm install
npm run build
```

Docker 配置检查：

```bat
docker compose config
```

## 关键能力

- 网关统一入口，前端只访问 `/api/**`。
- 认证服务签发 JWT，各业务服务本地校验角色。
- 律师端独立入口 `/lawyer-center`，支持工作台、资料、排班、预约、咨询页面。
- 预约服务默认生成周一至周五可预约时段，已预约或案件占用时段返回不可预约。
- AI 服务独立保存聊天会话与消息，按用户隔离历史记录。
- 律所律师服务和预约服务通过 OpenFeign 协作，避免跨库直接访问。
