# 数据库脚本

当前微服务的脚本位于 microservices 目录。MySQL 容器首次初始化数据目录时会依次执行建库、建表、升级检查和演示数据脚本。

已有旧版微服务数据卷时，在应用启动前进入 MySQL 客户端：

~~~powershell
docker compose exec mysql mysql --default-character-set=utf8mb4 -uroot -p
~~~

按顺序执行以下命令：

~~~sql
SOURCE /docker-entrypoint-initdb.d/001_create_databases.sql;
SOURCE /docker-entrypoint-initdb.d/002_apply_service_schemas.sql;
SOURCE /docker-entrypoint-initdb.d/003_upgrade_business_services.sql;
~~~

升级脚本补充旧表字段，将旧预约库中的排班和通知复制到各自的新库，并导入已有的有效时段占用。请先备份现有数据卷。若同一个账号绑定了多个有效律师档案，需先保留正确的绑定，再执行唯一索引升级。

应用表结构脚本会在认证库创建 OAuth2 令牌相关表，在预约库、排班库创建 Seata AT 的 `undo_log`，并在 AI 库创建用于校验会话锁持有者的 `ai_chat_guard`。升级脚本会给旧版预约和排班的锁表补齐 `guard_version`。

旧表中状态为 PENDING 的预约保留在列表中并标记为预约失败。用户可在开放时段重新预约。

999_init_data.sql 用于全新数据卷的演示数据初始化。sql 目录下的旧版建库建表脚本对应历史单体数据库。
