# 智教实训前端

当前目录是教学智能体平台前端工程。页面结构已按教师、学生、管理员三类工作空间拆分，不内置业务 Mock 数据；所有业务数据入口集中在 `src/services/api.ts`，等待与 Spring Boot 后端接口联调。

## 启动

```bash
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173/
```

## 已包含页面

- `/`：未登录中文官网首页
- `/login`：角色登录页
- `/teacher`：教师空间工作台
- `/teacher/agents`：智能体管理
- `/teacher/courses`：课程与实训
- `/teacher/knowledge`：知识库管理
- `/teacher/templates`：模板中心
- `/teacher/classes`：班级管理
- `/teacher/interactions`：互动记录
- `/teacher/tasks`：任务与反馈
- `/student`：学生空间首页
- `/student/classes`：我的班级
- `/student/practice`：AI 实训对话
- `/student/travel-practice`：旅游销售对练
- `/student/tasks`：任务与反馈
- `/admin`：管理概览
- `/admin/users`：用户与角色
- `/admin/system`：系统配置
- `/admin/audit`：审计日志

## 验证

```bash
npm run build
npm audit --omit=dev
```
