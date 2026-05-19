# Dify 工作流导入包

这个目录包含旅游销售培训平台的 6 条 Dify Workflow DSL 文件。设计原则是先保证可导入、可调用、可由后端代理接入；知识库不在 YAML 中强绑定，避免因为目标 Dify 环境没有相同 Dataset ID 导致导入失败。

## 文件清单

- `01-course-qa.yml`：课程问答工作流
- `02-simulated-customer.yml`：AI 模拟客户工作流
- `03-ai-advisor.yml`：AI 参谋工作流
- `04-attraction-speech.yml`：景点标签与话术生成工作流
- `05-product-change.yml`：旅游产品变更提醒工作流
- `06-travel-simulation.yml`：轻量数字孪生旅行模拟工作流
- `07-theory-knowledge.yml`：理论知识查询工作流

## 导入方式

在 Dify 工作室中选择“导入 DSL 文件”，逐个导入上述 YAML 文件。导入后需要根据你自己的 Dify 环境检查两项：

1. 模型节点默认使用 `gpt-4o-mini`，如你的 Dify 未配置 OpenAI Provider，请在导入后替换为可用模型。
2. 当前版本通过输入变量接收 `retrieved_context`、`product_context`、`course_context` 等资料文本。后续可以在 Dify 编排页中增加 Knowledge Retrieval 节点，把结果接入对应提示词。

## 推荐后端调用策略

- 学生对话类：`01`、`02`、`03`、`06`、`07` 走流式调用，前端用 SSE 或 WebSocket 展示增量结果。
- 预生成类：`04` 可在教师配置景点资料后后台批量生成。
- 后台异步类：`05` 在产品数据更新后由队列触发，不建议放在学生实时对话链路里。

## 稳定性约定

这些工作流都采用单轮 Workflow 形式。多轮上下文由平台后端整理为 `conversation_history` 或 `simulation_state` 后传入 Dify，业务权限、任务状态、训练记录、重试和超时控制仍由 Spring Boot 后端负责。
