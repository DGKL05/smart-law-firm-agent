# SQL 脚本说明

请按以下顺序手动执行。Windows 命令行或 Docker 容器中导入时，必须使用 UTF-8 连接；脚本内已包含 `SET NAMES utf8mb4`，避免中文初始化数据乱码。

1. `001_create_database.sql`：创建数据库并切换到 `smart_law_firm_agent`。
2. `002_init_schema.sql`：创建系统表、业务表、权限表。
3. `003_init_data.sql`：写入角色、菜单、测试账号、律所、律师、预约、案件、咨询和通知示例数据。

Docker MySQL 导入示例：

```bash
docker exec -i smart-law-mysql mysql --default-character-set=utf8mb4 -uroot -proot < sql/001_create_database.sql
docker exec -i smart-law-mysql mysql --default-character-set=utf8mb4 -uroot -proot < sql/002_init_schema.sql
docker exec -i smart-law-mysql mysql --default-character-set=utf8mb4 -uroot -proot < sql/003_init_data.sql
```

`changes/` 用于记录后续增量变更脚本。本项目当前不会自动执行 SQL。
