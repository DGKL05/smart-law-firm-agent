# SQL 脚本说明

请按以下顺序手动执行：

1. `001_create_database.sql`：创建数据库并切换到 `smart_law_firm_agent`。
2. `002_init_schema.sql`：创建系统表、业务表、权限表。
3. `003_init_data.sql`：写入角色、菜单、测试账号、律所和律师示例数据。

`changes/` 用于记录后续增量变更脚本。本项目当前不会自动执行 SQL。
