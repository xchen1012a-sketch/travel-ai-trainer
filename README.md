# Travel AI Trainer

旅游咨询员 AI 智能实战培训系统，用于旅游服务、产品销售、景点讲解、客户沟通等教学场景的 AI 实训。项目包含前端管理与训练界面、Spring Boot 后端接口、MySQL 数据库脚本，以及可导入 Dify 的工作流模板。

## 项目定位

本项目面向旅游类专业教学与企业内训，帮助教师创建课程、班级、知识库、训练任务和 AI 场景，帮助学员通过模拟客户、旅游问答、产品变更、景点话术、旅行模拟等方式进行实战训练。

## 核心功能

- 教师端：课程管理、班级管理、资料库管理、训练任务管理、对练记录查看、Dify 场景配置。
- 学员端：加入班级、完成任务、旅游知识问答、销售对练、模拟客户沟通、景点话术训练、旅行场景模拟。
- 管理端：用户管理、系统配置、模型与密钥配置、工作流管理、调用日志与审计。
- AI 工作流：内置 7 个 Dify Workflow DSL 文件，可按场景导入 Dify 后由后端代理调用。
- 数据持久化：使用 MySQL 保存用户、课程、班级、会话、消息、知识库和工作流配置等业务数据。

## 技术栈

### 前端

- React 19
- TypeScript
- Vite
- Ant Design / Ant Design Pro Components

### 后端

- Java 21
- Spring Boot 3
- Spring Web
- Spring JDBC
- MySQL Connector/J

### AI 与数据

- Dify Workflow
- MySQL 8

## 目录结构

```text
travel-ai-trainer/
├── backend/          # Spring Boot 后端服务
├── database/         # MySQL 建表脚本、迁移脚本和演示数据
├── dify-workflows/   # Dify 工作流 DSL 导入文件
├── frontend/         # React + Vite 前端工程
└── README.md         # 项目说明文档
```

## 快速启动

### 1. 初始化数据库

请先确认本地已安装 MySQL 8，并创建或初始化 `teach_agent` 数据库。

```bash
mysql -uroot -p < database/schema.sql
```

如需导入演示数据：

```bash
mysql -uroot -p teach_agent < database/seed_demo.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

可通过环境变量覆盖数据库配置：

```bash
DB_URL="jdbc:mysql://127.0.0.1:3306/teach_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
DB_USERNAME="root"
DB_PASSWORD="123456"
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

## Dify 工作流导入

`dify-workflows/` 目录包含以下工作流：

- `01-course-qa.yml`：课程问答
- `02-simulated-customer.yml`：AI 模拟客户
- `03-ai-advisor.yml`：AI 参谋
- `04-attraction-speech.yml`：景点话术生成
- `05-product-change.yml`：旅游产品变更提醒
- `06-travel-simulation.yml`：旅行模拟
- `07-theory-knowledge.yml`：理论知识查询

在 Dify 工作室中选择导入 DSL 文件，逐个导入上述 YAML。导入后根据自己的 Dify 环境调整模型、知识库检索节点和 API Key，再在系统后台或接口中保存对应工作流配置。

## 常用接口

后端启动后可访问：

```text
GET  /api/health
POST /api/users
POST /api/courses
POST /api/agents
POST /api/classes
POST /api/class-members/join
POST /api/conversations
POST /api/messages
GET  /api/workflow-configs?teacherId=1
POST /api/workflow-configs
POST /api/workflow-configs/test
```

也可以运行后端目录中的接口测试脚本：

```bash
cd backend
bash scripts/test_api.sh
```

## 构建验证

前端：

```bash
cd frontend
npm run build
```

后端：

```bash
cd backend
mvn test
```

## 部署说明

- 前端执行 `npm run build` 后，将 `frontend/dist` 部署到 Nginx、静态资源服务或对象存储。
- 后端可通过 Maven 打包为 Jar，并使用环境变量配置数据库连接和外部服务密钥。
- 生产环境不要在代码中提交真实 API Key、数据库密码或 Dify 密钥。
- Dify API Key 建议使用加密存储、密钥管理服务或运行环境变量进行管理。

## 许可证

如需开源发布，请在仓库中补充 `LICENSE` 文件并明确授权协议。
