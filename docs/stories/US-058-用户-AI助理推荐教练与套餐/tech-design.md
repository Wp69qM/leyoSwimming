# Tech Design: US-058 用户-AI助理推荐教练与套餐

## 1. 总体架构

```
用户端小程序（Taro/React）
    │
    ▼
Java Spring Boot API Gateway（鉴权、会话管理、路由、数据脱敏）
    │  /api/ai-assistant/chat
    │  /api/ai-assistant/session/list
    │  /api/ai-assistant/session/create
    │  /api/ai-assistant/session/detail
    ▼
Python AI Service（FastAPI + LangChain）
    │
    ├── LangChain Agent
    │       ├── Prompt Template
    │       ├── @tool Tool Registry
    │       └── Conversation Memory（Redis）
    │
    └── LLM Client（通义千问 / Kimi / DeepSeek）
    │
    ▼ 内部数据接口 /api/internal/ai/*
Java Spring Boot 业务服务
    ├── Coach Service
    ├── Package Service
    ├── User Profile Service
    └── Order/Booking Service
    │
    ▼
Redis（会话缓存、限流、对话 Memory）
MySQL（会话历史、消息记录、推荐日志、业务数据）
```

---

## 2. 数据模型

### 2.1 新增表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `ai_chat_session` | 写 | 会话主表 |
| `ai_chat_message` | 写 | 消息记录表 |
| `ai_recommendation_log` | 写 | 推荐日志表 |
| `coach` | 读 | 教练公开信息 |
| `package_template` | 读 | 套餐模板信息 |
| `package` | 读 | 已登录用户已购套餐 |
| `user_profile` | 读 | 用户画像 |

#### ai_chat_session

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `session_id` | 业务 ID，唯一 |
| `user_id` | 用户 ID（游客为 null）|
| `title` | 会话标题，取首条用户消息前 20 字 |
| `status` | active / archived |
| `created_at` | 创建时间 |
| `updated_at` | 最后消息时间 |

#### ai_chat_message

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `message_id` | 业务 ID，唯一 |
| `session_id` | FK |
| `role` | user / assistant / tool |
| `content` | 文本内容 |
| `recommendations` | JSON，推荐卡片数据 |
| `tool_calls` | JSON，Tool 调用记录 |
| `created_at` | 创建时间 |

#### ai_recommendation_log

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `session_id` | 会话 ID |
| `message_id` | 消息 ID |
| `user_id` | 用户 ID（游客为 null）|
| `input` | 用户原始输入 |
| `tool_calls` | Tool 调用链 |
| `llm_response` | LLM 原始回复 |
| `final_response` | 返回前端的结构化回复 |
| `latency_ms` | 总耗时 |
| `llm_latency_ms` | LLM 调用耗时 |
| `created_at` | 创建时间 |

### 2.2 索引

```sql
CREATE INDEX idx_ai_session_user ON ai_chat_session(user_id, status, updated_at);
CREATE INDEX idx_ai_message_session ON ai_chat_message(session_id, created_at);
CREATE INDEX idx_ai_log_session ON ai_recommendation_log(session_id, created_at);
CREATE INDEX idx_ai_log_created ON ai_recommendation_log(created_at);
```

---

## 3. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。
> 内部接口以 `/api/internal/ai` 为前缀，仅允许 Python AI Service 访问。

### 3.1 对外接口：小程序 → Java API Gateway

#### 3.1.1 POST /api/ai-assistant/chat

- **鉴权**：游客可访问，已登录用户带 JWT
- **幂等**：是（`Idempotency-Key` header）
- **Request**:
  ```json
  {
    "sessionId": "sess_abc123",
    "message": "我 30 岁，想学自由泳，预算 3000 左右，推荐个教练"
  }
  ```
- **Response 200**:
  ```json
  {
    "sessionId": "sess_abc123",
    "messageId": "msg_xyz789",
    "reply": {
      "text": "根据你的情况，leyo 推荐王教练的 10 节自由泳私教课。王教练 8 年教学经验，评分 4.9，擅长成人自由泳教学。",
      "recommendations": [
        {
          "type": "coach",
          "id": 1,
          "name": "王教练",
          "avatarUrl": "https://cdn.example.com/avatar/1.jpg",
          "rating": 4.9,
          "reason": "8 年教学经验，擅长自由泳"
        },
        {
          "type": "package",
          "id": 101,
          "name": "成人自由泳 10 节私教",
          "price": 1800,
          "hours": 10,
          "reason": "单节课 180 元，符合预算"
        },
        {
          "type": "custom_package",
          "coachId": 1,
          "coachName": "王教练",
          "hours": 12,
          "pricePerHour": 200,
          "totalPrice": 2400,
          "classSize": "一对一",
          "stroke": "自由泳",
          "reason": "标准套餐没有 12 节配置，可按你需求的课时自定义"
        }
      ],
      "suggestedQuestions": ["这个教练能约什么时候？", "有没有更便宜的体验课？", "自定义套餐怎么买？"]
    }
  }
  ```
- **Response 500**: LLM 服务异常，前端提示重试

#### 3.1.2 POST /api/ai-assistant/session/list

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "page": 1,
    "size": 20
  }
  ```
- **Response 200**:
  ```json
  {
    "items": [
      {
        "sessionId": "sess_abc123",
        "title": "自由泳教练推荐",
        "lastMessageAt": "2026-08-15T10:00:00Z",
        "messageCount": 8
      }
    ],
    "total": 5,
    "page": 1,
    "size": 20
  }
  ```

#### 3.1.3 POST /api/ai-assistant/session/create

- **鉴权**：游客可访问，已登录用户带 JWT
- **Request**: `{}`
- **Response 200**:
  ```json
  {
    "sessionId": "sess_new123",
    "welcomeMessage": "你好！我是 leyo，你的游泳学习助手。"
  }
  ```

#### 3.1.4 POST /api/ai-assistant/session/detail

- **鉴权**：会话所有者或已登录用户本人
- **Request**:
  ```json
  {
    "sessionId": "sess_abc123"
  }
  ```
- **Response 200**:
  ```json
  {
    "sessionId": "sess_abc123",
    "messages": [
      { "role": "user", "content": "我想学自由泳", "createdAt": "2026-08-15T09:55:00Z" },
      { "role": "assistant", "content": "...", "createdAt": "2026-08-15T09:55:05Z" }
    ]
  }
  ```

### 3.2 内部接口：Python AI Service → Java 业务服务

所有内部接口通过 HTTP + 内网 Token 访问，返回数据已脱敏。

#### 3.2.1 POST /api/internal/ai/coaches/query

**Request**:
```json
{
  "stroke": "freestyle",
  "gender": "female",
  "minPrice": 100,
  "maxPrice": 300,
  "classSize": "one_on_one",
  "limit": 5
}
```

**Response 200**:
```json
{
  "data": [
    {
      "coachHash": "c_3e5b12",
      "name": "王教练",
      "avatarUrl": "https://...",
      "gender": "female",
      "rating": 4.9,
      "referencePrice": 200,
      "teachingYears": 8,
      "teachingStrokes": ["freestyle", "breaststroke"]
    }
  ]
}
```

#### 3.2.2 POST /api/internal/ai/packages/query

**Request**:
```json
{
  "stroke": "freestyle",
  "packageMode": "custom",
  "minPrice": 1000,
  "maxPrice": 3000,
  "hours": 12,
  "limit": 5
}
```

#### 3.2.3 POST /api/internal/ai/user/profile

**Request**:
```json
{
  "userHash": "u_7d8f9a"
}
```

#### 3.2.4 POST /api/internal/ai/user/packages

**Request**:
```json
{
  "userHash": "u_7d8f9a",
  "statuses": ["active", "exhausted"]
}
```

#### 3.2.5 POST /api/internal/ai/recommendations/hot

**Request**:
```json
{
  "stroke": "freestyle",
  "limit": 5
}
```

---

## 4. LangChain Agent 设计

### 4.1 Tool 注册

```python
from langchain_core.tools import tool
import httpx

INTERNAL_API_BASE = "http://leyo-backend:8080/api/internal/ai"

@tool
def query_coaches(stroke: str, gender: str = None, max_price: int = None, limit: int = 5) -> list:
    """根据目标泳姿、教练性别、价格上限召回候选教练。"""
    ...

@tool
def query_packages(stroke: str, package_mode: str = None, max_price: int = None, hours: int = None, limit: int = 5) -> list:
    """根据目标泳姿、套餐模式（standard/experience/custom）、价格上限、课时数召回候选套餐。"""
    ...

@tool
def get_user_profile(user_hash: str) -> dict:
    """获取已登录用户的画像信息，用于个性化推荐。游客场景不要调用。"""
    ...

@tool
def get_user_packages(user_hash: str) -> list:
    """获取用户已购套餐，用于避免重复推荐。游客场景不要调用。"""
    ...

@tool
def get_hot_recommendations(stroke: str = None, limit: int = 5) -> list:
    """获取热门教练或套餐，用于冷启动或游客默认推荐。"""
    ...

tools = [query_coaches, query_packages, get_user_profile, get_user_packages, get_hot_recommendations]
```

### 4.2 Agent 构造

```python
from langchain import hub
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="qwen-plus",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    temperature=0.3,
)

prompt = hub.pull("hwchase17/openai-tools-agent")
prompt.messages[0].prompt.template = """你是 leyo，一位专业的游泳学习助手。你的任务是根据用户的需求，推荐合适的游泳教练和课程套餐。

注意：
1. 如果用户未提供关键信息（年龄、目标泳姿、预算、游泳基础），可以主动询问。
2. 推荐时必须调用 query_coaches 或 query_packages 工具获取真实数据，不能编造。
3. 已登录用户可以调用 get_user_profile 和 get_user_packages 做个性化推荐。
4. 游客用户只能调用 query_coaches、query_packages、get_hot_recommendations。
5. 回复要简洁、友好，突出推荐理由。

{input}"""

agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)
```

### 4.3 调用流程

```
用户输入
  → AgentExecutor
    → LLM 决定 Tool + 参数
    → 调用 Tool（访问 Java 内部接口）
    → 返回 Tool 结果给 LLM
    → LLM 生成最终回复
  → 解析回复为结构化数据
  → 保存会话消息和推荐日志
  → 返回前端
```

---

## 5. 会话与 Memory

### 5.1 Redis Memory

- Key: `ai:session:{session_id}:messages`
- Value: 最近 N 条消息（如 20 条），JSON 序列化
- TTL: 7 天
- 用于维护多轮对话上下文

### 5.2 持久化

- 每条用户消息和 leyo 回复实时写入 `ai_chat_message`
- 会话标题在首条用户消息后异步更新到 `ai_chat_session`

---

## 6. 数据脱敏

| 字段 | 处理方式 |
|------|---------|
| `user.id` | 哈希为 `userHash` |
| `coach.id` | 哈希为 `coachHash` |
| `phone` | 不进入 Prompt |
| `openid` / `union_id` | 不进入 Prompt |
| `user.name` | 不进入 Prompt |
| `coach.name` | 可进入 Prompt 和前端展示 |
| `coach.avatarUrl` | 可保留 |
| `coach.rating` / `teachingYears` / `referencePrice` | 可保留 |

---

## 7. 缓存与限流

| 层 | Key | TTL | 说明 |
|----|-----|-----|------|
| Redis | `ai:session:{id}:messages` | 7 天 | 会话上下文 |
| Redis | `ai:rate:user:{id}` | 1 分钟 | 单用户每分钟最多 30 次调用 |
| Redis | `ai:rate:ip:{ip}` | 1 分钟 | 单 IP 每分钟最多 60 次调用 |
| Java 本地缓存 | hot_recommendations | 10 分钟 | 热门推荐数据 |

---

## 8. 性能

| 指标 | 目标 |
|------|------|
| `/api/ai-assistant/chat` P99 | < 5s（含 LLM 调用）|
| `/api/internal/ai/*` P99 | < 100ms |
| `/api/ai-assistant/session/list` P99 | < 100ms |

---

## 9. 安全

- 游客可访问 `/api/ai-assistant/chat` 和 `/api/ai-assistant/session/create`
- 已登录用户才能访问 `/api/ai-assistant/session/list` 和 `/api/ai-assistant/session/detail`
- 内部接口 `/api/internal/ai/**` 仅限 Python AI Service 内网 IP + Token 访问
- 用户输入需做敏感词过滤和长度限制（最多 500 字）
- LLM Prompt 中不暴露用户敏感字段
- 推荐日志不记录敏感输入原文

---

## 10. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录态与用户画像 |
| US-019 | 依赖 | 套餐浏览数据与页面跳转 |
| US-020 | 依赖 | 套餐购买数据 |
| US-045 | 依赖 | 套餐模板配置 |
| US-043 | 依赖 | 套餐数据维护 |

---

## 11. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 游客推荐教练 | `test_guest_coach_recommendation` |
| 已登录用户推荐套餐 | `test_logged_in_package_recommendation` |
| 避免重复推荐已购套餐 | `test_no_duplicate_package_recommendation` |
| 自定义套餐推荐 | `test_custom_package_recommendation` |
| 新建会话 | `test_create_new_session` |
| 查看历史会话 | `test_list_history_sessions` |
| LLM 超时降级 | `test_llm_timeout_fallback` |
| 敏感输入过滤 | `test_sensitive_input_blocked` |
| 内部接口鉴权 | `test_internal_api_unauthorized` |
