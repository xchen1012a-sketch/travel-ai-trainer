# 后端接口

当前后端使用 Spring Boot 3 + JDBC + MySQL。

## 数据库

默认连接：

- 数据库：`teach_agent`
- 地址：`127.0.0.1:3306`
- 用户：`root`
- 密码：`123456`

初始化数据库：

```bash
mysql -uroot -p123456 < ../database/schema.sql
```

## 启动

```bash
mvn spring-boot:run
```

也可以用环境变量覆盖数据库配置：

```bash
DB_URL='jdbc:mysql://127.0.0.1:3306/teach_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=123456 \
mvn spring-boot:run
```

## 测试接口状态码

服务启动后，另开终端执行：

```bash
bash scripts/test_api.sh
```

测试会走完整链路并写入 MySQL：

1. 创建教师和学生。
2. 创建课程。
3. 创建智能体。
4. 创建班级并生成班级码。
5. 学生通过班级码加入。
6. 创建会话。
7. 写入学生消息和模拟智能体回复。

## 已实现接口

- `GET /api/health`
- `POST /api/users`
- `POST /api/courses`
- `POST /api/agents`
- `POST /api/classes`
- `POST /api/class-members/join`
- `POST /api/conversations`
- `POST /api/messages`

## 说明

- 当前接口已持久化到 MySQL。
- `POST /api/messages` 暂时返回模拟智能体回复，Dify 接入后替换为真实工作流调用。
- 状态码约定：创建类接口返回 `201`，健康检查和加入班级返回 `200`。
