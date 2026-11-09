# 智能律所平台

公众可以检索律所和律师。登录用户可以预约、查看案件、提交咨询、收取通知和使用智能法律助理。律师工作台提供资料和头像维护、排班停诊、预约和咨询处理。后台按权限维护业务数据，并汇总统计和管理操作审计。

| Java 模块 | 端口 | 职责 | MySQL 数据库 |
| --- | ---: | --- | --- |
| gateway-service | 8080 | API 路由、OAuth2 令牌校验和管理操作审计采集 | — |
| auth-service | 8081 | 注册登录、个人资料、密码、账号和角色 | smart_law_auth |
| lawyer-service | 8082 | 律所和律师档案、公开检索、头像与律所照片 | smart_law_lawyer |
| appointment-service | 8083 | 预约、案件、人工咨询和预约通知投递 | smart_law_appointment |
| ai-service | 8084 | Dify 对话、会话和消息历史 | smart_law_ai |
| admin-service | 8085 | 跨服务统计、管理操作审计 | smart_law_admin |
| schedule-service | 8086 | 每周排班、临时停诊和时段占用 | smart_law_schedule |
| notification-service | 8087 | 站内通知、通知模板和阅读状态 | smart_law_notification |

七个业务库位于同一个 MySQL 容器。网关通过 `lb://服务名` 从 Nacos 发现实例，OpenFeign 按服务名调用各业务服务。预约服务对排班等内部调用配置了 1.5 秒连接超时、3 秒读取超时和 `BASIC` 日志。

登录使用 Spring Authorization Server 的授权码流程和 PKCE。授权记录和访问令牌保存在 `smart_law_auth`，网关通过内省校验令牌及当前角色。管理 API 要求管理员角色，律师工作台 API 要求律师角色。

## IDEA 开发

需要 JDK 17、Maven、Node.js 和 Docker Desktop。首次部署时在根目录生成本地凭据。如果已经有 `.env`，继续使用原文件中的值。

~~~powershell
if (-not (Test-Path .env)) {
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $keys = 'OAUTH2_INTROSPECTION_CLIENT_SECRET', 'RABBITMQ_PASSWORD', 'SBA_PASSWORD'
        $keys | ForEach-Object {
            $bytes = New-Object byte[] 32
            $random.GetBytes($bytes)
            "$_=$([Convert]::ToBase64String($bytes))"
        } | Set-Content .env -Encoding ascii
    } finally {
        $random.Dispose()
    }
}
~~~

从根目录构建监控服务并启动中间件。Sentinel 镜像在首次构建时下载官方 Dashboard JAR 并校验 SHA-256。

~~~powershell
.\mvnw.cmd -pl monitor-service -am -DskipTests package
docker compose up -d --build mysql redis mongodb nacos nacos-config-init rabbitmq seata-server sentinel-dashboard zipkin monitor-service
~~~

如果 Docker 无法直接下载 Sentinel，可先用 `curl.exe -fL` 将 [官方 Dashboard JAR](https://github.com/alibaba/Sentinel/releases/download/1.8.8/sentinel-dashboard-1.8.8.jar) 保存到 `docker/sentinel/sentinel-dashboard.jar`，在 `.env` 中加入 `SENTINEL_DASHBOARD_SOURCE=sentinel-dashboard.jar` 后重试。

已有旧 MySQL 数据卷时，先按 [数据库升级说明](sql/README.md) 补齐表结构。在 IDEA 中导入根 Maven 工程，选择 JDK 17 并同步 Maven，然后运行 `01-全部后端`。运行配置会从项目根目录读取 `.env`。随后在 `frontend` 目录执行以下命令。

~~~powershell
npm ci
npm run dev
~~~

浏览器访问 [前端](http://localhost:5173)，开发代理把 API 请求转发给 8080 端口的网关。升级 OAuth2 后需要重新登录。

| Docker 组件 | 本机端口或入口 | 项目用途 |
| --- | --- | --- |
| MySQL | 3307 | 七个独立业务库及 OAuth2 授权与令牌 |
| Redis | 6379 | 缓存律师和律所基础资料，为 AI 会话的消息生成提供分布式锁 |
| MongoDB | 27017 | 预留文档存储，AI 会话和消息当前存于 MySQL |
| Nacos | 8848、9848，[控制台](http://localhost:8848/nacos/) | 注册发现、共享及专属配置、Sentinel 规则 |
| RabbitMQ | 5672，[管理界面](http://localhost:15672) | 预约和咨询回复通知分队列异步处理，突发消息在队列中等待有界消费 |
| Seata | 8091，[控制台](http://localhost:7091) | 协调预约记录与排班占用的 AT 事务 |
| Sentinel | [Dashboard](http://localhost:8070) | 律师详情流控和熔断，AI 助手调用 Dify 的并发限制、用户限流和失败熔断 |
| Zipkin | [链路查询](http://localhost:9411) | 收集 Micrometer Tracing 的 HTTP 和 Feign 链路 |
| Spring Boot Admin | [监控界面](http://localhost:8090) | 展示各 Java 服务健康状态和指标 |

RabbitMQ 和 Spring Boot Admin 默认用户名为 `smartlaw`，密码分别是 `.env` 里的 `RABBITMQ_PASSWORD` 和 `SBA_PASSWORD`。本机 Sentinel 和 Seata 控制台使用各自镜像的默认账号 `sentinel` 和 `seata`，密码与用户名相同。

## Compose 部署

需要把前后端也放进 Docker 时，使用以下命令。IDEA 与 Docker 的应用使用不同的 Nacos 服务发现分组，但共用业务库，切换时应先停止当前运行的应用实例。

~~~powershell
.\mvnw.cmd -DskipTests package
docker compose up -d --build
~~~

MySQL 首次启动会自动创建七个业务库。复用旧版 MySQL 数据卷时，先按 [SQL 升级说明](sql/README.md) 执行增量脚本。

| 演示角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 普通用户 | user | 123456 |
| 律师 | lawyer | 123456 |

律师在「我的资料」上传本人头像，管理员在「律所管理」上传律所照片。图片支持 JPEG 和 PNG，单张上限 2 MB。文件与资料关联保存在律所律师库。

删除律师档案前，先停用该律师并处理在途预约、案件和咨询。停用后，律师仍可在工作台处理已有记录。

AI 对话使用环境变量 DIFY_API_KEY，可通过 DIFY_BASE_URL 指定 Dify API 地址。IDEA 启动时在 AI 服务运行配置中设置，Compose 部署时从宿主机环境传入。

AI 服务通过 Sentinel 保护 Dify 调用。每个 AI 实例最多同时调用 8 次，同一账号每 10 秒最多发起 3 次。上游超时等异常计入熔断统计，达到阈值后会提示稍后重试。Redis 为同一会话提供 120 秒租约，锁被占用时返回 `409`。保存回答前会再次检查锁的持有者。

预约成功、预约取消和律师首次回复咨询都会异步生成站内通知。预约和咨询分别使用 `smart-law.notifications.appointment` 与 `smart-law.notifications.consultation` 队列。每个通知服务实例为每个队列使用一个消费者，最多预取 8 条。可在 RabbitMQ 管理界面观察 `Ready` 和 `Unacked` 消息数量。

## Nacos 配置中心

默认使用 `public` 命名空间和 `SMART_LAW_DEV` 配置分组。应用启动时先读取 `smart-law-shared.yml`，再读取与 `spring.application.name` 对应的服务 YAML。共享配置管理 Feign 默认超时、Spring Boot Admin 注册和 Zipkin 上报。专属配置管理端口、数据库地址、网关路由等参数。

配置模板在 [config/nacos](config/nacos)。Compose 的 `nacos-config-init` 在 Nacos 就绪后补发缺失配置。需要将仓库模板同步到已有环境时，先备份控制台中的自定义值，再执行以下命令。

~~~powershell
docker compose run --rm --no-deps -e NACOS_CONFIG_APPLY_CHANGES=true nacos-config-init
~~~

共享和专属配置在 Java 服务重启时生效。律师服务和 AI 服务分别监听各自的 Sentinel JSON 规则，可在 Nacos 中动态更新。密码和令牌密钥从本地 `.env` 或运行环境读取。独立环境可通过 `NACOS_CONFIG_NAMESPACE` 指定已创建的命名空间 ID，通过 `NACOS_CONFIG_GROUP` 指定发布和读取所用的配置分组。

## 构建验证

~~~powershell
.\mvnw.cmd test
docker compose config --quiet
~~~

前端在 `frontend` 目录执行 `npm run build`。Spring Cloud Sleuth 支持到 Spring Boot 2.x，本项目的 Spring Boot 3.3 使用 Micrometer Tracing 向 Zipkin 上报链路。 [Spring Cloud Sleuth 官方说明](https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/README.html)

打包后可用以下脚本验证 Seata。脚本创建临时预约库和排班库，验证预约、取消及失败回滚，然后清理测试进程和测试库。需要本机的 MySQL 和 Seata 容器已启动。

~~~powershell
python appointment-service/src/test/scripts/verify_seata_at.py --zipkin-endpoint http://127.0.0.1:9411/api/v2/spans
~~~
