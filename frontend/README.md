# 旅游咨询员 AI 智能实战培训系统前端

当前目录是旅游咨询员 AI 实战培训系统前端工程。页面结构按训练管理端、学员实训端、系统管理端拆分；业务数据入口集中在 `src/services/api.ts`，用于和 Spring Boot 后端接口联调。

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

- `/`：未登录中文首页
- `/login`：角色登录页
- `/teacher`：训练管理工作台
- `/teacher/courses`：培训项目
- `/teacher/knowledge`：资料库管理
- `/teacher/classes`：班级管理
- `/teacher/interactions`：对练记录
- `/teacher/tasks`：任务与反馈
- `/teacher/products`：旅游产品库
- `/teacher/attractions`：景点话术库
- `/teacher/customer-profiles`：客户画像库
- `/teacher/scenarios`：训练场景库
- `/teacher/workflows`：Dify 场景配置
- `/student`：学员实训首页
- `/student/classes`：我的班级
- `/student/practice`：任务对练
- `/student/tourism-qa`：旅游知识问答
- `/student/travel-practice`：旅游销售对练
- `/student/customer-practice`：客户模拟对练
- `/student/advisor`：AI 参谋
- `/student/attraction-speech`：景点话术训练
- `/student/product-changes`：产品变更提醒
- `/student/travel-simulation`：旅行模拟
- `/student/tasks`：任务与反馈
- `/admin`：管理概览
- `/admin/users`：用户与角色
- `/admin/system`：系统配置
- `/admin/workflows`：Dify 工作流管理
- `/admin/models`：模型与密钥配置
- `/admin/call-logs`：调用日志
- `/admin/jobs`：队列/异步任务
- `/admin/audit`：审计日志

## 验证

```bash
npm run build
npm audit --omit=dev
```
