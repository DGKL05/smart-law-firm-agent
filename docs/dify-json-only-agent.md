# Dify JSON-only Agent 接入说明

## 最终架构

```text
Vue3 前端聊天页面
  -> POST /api/ai/chat
  -> Spring Boot 后端调用 Dify 云端 /chat-messages
  -> Dify 返回 JSON 字符串
  -> Spring Boot 解析 JSON
  -> Spring Boot 本地调用 AgentAppointmentService
  -> MySQL
```

前端不直接调用 Dify，也不保存 Dify API Key。Dify 只负责意图识别、参数抽取和普通法律咨询回复。

## 不再让 Dify 回调 Spring Boot 的原因

- 云端 Dify 无法访问本机的 `host.docker.internal:8080`。
- 本地业务由 Spring Boot 自己执行更稳定，能复用登录态、权限和事务边界。
- 不需要把本地 8080 暴露到公网。
- 新流程不需要 `SPRINGBOOT_BASE_URL`。
- 新流程不需要 `SPRINGBOOT_API_TOKEN`。

原来的 `/api/agent/appointments` 和 `/api/agent/appointments/cancel` 仍可作为本地测试接口保留，但新聊天流程不依赖云端 Dify 回调它们。

## conversation_id 机制

第一次请求前端传空字符串：

```json
{
  "query": "帮我预约黑龙江冰城律师事务所赵思远律师的法律咨询",
  "conversationId": ""
}
```

Spring Boot 调 Dify 时映射为：

```json
{
  "conversation_id": ""
}
```

Dify 返回 `conversation_id` 后，Spring Boot 原样返回给前端。前端同一页面内保存该值，下一轮继续传给 `/api/ai/chat`。这样第二轮只说“时间：23号 14:00”时，Dify 可以结合上一轮上下文补齐律所和律师。

`user` 字段必须稳定，Spring Boot 使用当前登录用户 ID：

```text
user-{userId}
```

不要使用随机 UUID，否则 Dify 上下文无法连续。

## Dify 输出 JSON 协议

Dify 的最终 `answer` 必须是 JSON 字符串，不能带 Markdown 代码块或解释文字。

```json
{
  "intent": "CREATE_APPOINTMENT",
  "appointmentTime": "2026-06-23 14:00",
  "lawFirmName": "黑龙江冰城律师事务所",
  "lawyerName": "赵思远",
  "remark": "用户通过智能法律助理预约法律咨询",
  "missingFields": [],
  "reply": "已识别到你的预约信息，正在为你创建预约。"
}
```

支持的 `intent`：

- `CREATE_APPOINTMENT`
- `CANCEL_APPOINTMENT`
- `MISSING_INFO`
- `NORMAL_CHAT`

如果 Dify 返回的 JSON 无法解析，Spring Boot 会降级为普通聊天，直接返回 Dify 原始 `answer`，不会抛 500。

## 本地配置

`src/main/resources/application-dev.yml`：

```yaml
dify:
  base-url: https://api.dify.ai/v1
  api-key: ${DIFY_API_KEY:}
  response-mode: blocking
```

Windows 启动前设置：

```bat
set DIFY_API_KEY=你的Dify应用ServiceAPIKey
mvnw.cmd spring-boot:run
```

不要把真实 API Key 写入仓库或 Vue 代码。

## 前端调用示例

```js
const res = await http.post('/api/ai/chat', {
  query: inputText,
  conversationId: conversationId.value
})

conversationId.value = res.conversationId || conversationId.value
messages.value.push({
  role: 'assistant',
  content: res.answer
})
```

新 DSL 文件位于：

```text
dify/smart_law_firm_agent_json_only.yml
```
