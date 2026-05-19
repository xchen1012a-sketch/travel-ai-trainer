# 数据库说明

首版数据库使用 MySQL 8，初始化脚本为 `schema.sql`。

## 建库

```bash
mysql -u root -p < database/schema.sql
```

默认数据库名：`teach_agent`

## 首版核心链路

1. 教师账号写入 `users`，角色为 `TEACHER`。
2. 教师创建课程 `courses`。
3. 教师创建智能体 `agents`，可绑定课程和 Dify Workflow。
4. 教师创建知识库 `knowledge_bases`，上传资料写入 `knowledge_files`。
5. 教师创建班级 `classes`，系统生成 `class_code`。
6. 学生账号写入 `users`，通过班级码加入 `class_members`。
7. 学生发起对话写入 `conversations` 和 `messages`。
8. Dify 返回内容也写入 `messages`，教师可按班级、学生、会话查看。

## 设计约定

- 业务删除优先使用 `deleted_at` 软删除。
- 枚举字段先贴合首版业务，后续如果状态复杂再拆成字典表。
- `dify_api_key_ref` 只保存密钥引用，不直接保存明文密钥。
- 文件真实内容不放数据库，`storage_key` 保存对象存储或本地存储路径。
