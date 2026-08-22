# AI 助理功能需求可行性分析报告

> **文档位置**：`docs/tech/AI-assistant-requirements/README.md`  
> **目标读者**：产品经理、后端/前端开发、AI 工程师  
> **状态**：可行性分析 / 已确认 Python LangChain 方案  
> **最后更新**：2026-08-15

---

## 1. 项目背景与目标

### 1.1 背景

leyoSwimming 第二批次开发完成后，系统将具备完整的用户、教练、套餐、订单、上课记录等核心业务数据。为提升游客/学员的转化效率，计划在**用户端小程序**底部 TabBar 新增一个**「AI 助理」**独立页面。

该功能起源于 [docs/figma/第二批次页面梳理.md](../../figma/第二批次页面梳理.md) §5「扩展功能」中的「游客的 AI 助理」设想。

### 1.2 目标

- 为**游客**提供基础版问答推荐：通过多轮对话收集年龄、泳姿、预算、目标等信息，推荐合适的套餐和教练。
- 为**已登录用户**提供个性化推荐：结合用户画像、已购套餐、历史订单、教练-学员档案，生成更精准的推荐。
- 支持**多轮对话、新建会话、查看历史会话**。
- 作为团队学习 **LangChain Agent** 的练手项目，接受按量付费的 LLM API 成本。

### 1.3 非目标

- 不替代现有搜索/列表功能，AI 助理是增量体验。
- MVP 阶段不推荐「实时可约时段」，避免与现有预约系统强耦合。
- 不做复杂的协同过滤或深度学习训练模型。

---

## 2. 前置工作

在启动 AI 助理功能前，必须完成以下前置工作：

### 2.1 业务数据准备

| 数据类型 | 最低要求 | 说明 |
|---------|---------|------|
| 教练数据 | ≥ 10 条有效教练 | 包含 `status=1`（已通过）、`reference_price`、`teaching_strokes`、`gender`、`age`、`teaching_years`、`rating` 等字段 |
| 套餐模板 | ≥ 5 条标准套餐 | 包含 `package_mode`、`hours`、`price`、`validity_days`、`teaching_type` 等字段 |
| 用户数据 | ≥ 20 条 mock 用户 | 包含年龄、性别、游泳基础、目标泳姿、预算区间等画像字段 |
| 订单数据 | ≥ 30 条购买记录 | 用于学习用户偏好和套餐热度 |
| 用户套餐 | ≥ 30 条 package 实例 | 用于已登录用户的「已购套餐-aware」推荐 |
| 预约记录 | ≥ 50 条 booking | 用于计算教练热度、完成率等画像指标 |

### 2.2 基础设施准备

- **LLM API 账号**：至少注册并充值一个国内可用的大模型服务（通义千问 / 文心一言 / Kimi / DeepSeek 等）。
- **Python 3.10+ 环境**：用于部署独立的 Python AI Service（FastAPI/Flask + LangChain）。
- **Redis**：已存在，用于缓存会话、推荐结果、限流。
- **MySQL**：已存在，用于存储会话历史、消息记录、推荐日志。
- **Mock 数据脚本**：需要一份可复用的 SQL/脚本，能在第二批次完成后一键生成测试数据。

### 2.3 设计稿准备

- 需在 Calicat 中补充「AI 助理」页面的设计稿。
- 需要明确的视觉规范：聊天气泡、输入框、推荐卡片、历史会话入口、加载态、空状态。

### 2.4 合规与授权

- 更新《隐私协议》/《用户须知》，说明 AI 助理会如何使用用户数据。
- 获取用户授权（尤其是已登录用户的画像数据）。

---

## 3. 需要了解的核心知识

### 3.1 Python LangChain / Agent 基础

- **Chain**：把 Prompt → LLM → Output Parser 串成流水线。
- **Agent**：让 LLM 决定调用哪个 Tool，并根据 Tool 结果继续推理。
- **@tool 装饰器**：用 Python 函数封装业务能力，LangChain 自动生成 Tool Schema 供 LLM function calling 使用。例如：

  ```python
  from langchain_core.tools import tool

  @tool
  def query_coaches(stroke: str, gender: str = None, max_price: int = None) -> list:
      """根据泳姿、性别、价格上限召回候选教练。"""
      ...
  ```

- **create_tool_calling_agent**：Python LangChain 中让 LLM 通过原生 function calling 调用 @tool 的 Agent 构造器。
- **Memory**：维护多轮对话上下文，常用 `ConversationBufferMemory` 或自定义 Redis Memory。

### 3.2 Prompt Engineering

- 如何写系统 Prompt，让 LLM 只推荐数据库中存在的教练/套餐。
- 如何让 LLM 输出结构化 JSON（用于前端渲染推荐卡片）。
- 如何防止幻觉：要求 LLM 在推荐理由中引用数据来源。

### 3.3 数据脱敏

- 用户手机号、真实姓名、openId、unionId 不得进入 LLM Prompt。
- 使用 `user_hash`、`coach_hash` 等匿名标识。
- 对敏感字段做哈希或删除处理。

### 3.4 推荐系统基础

- **召回（Recall）**：从全量数据中筛选候选集合。
- **排序（Rank）**：对候选集合打分排序。
- **解释（Explanation）**：生成推荐理由。
- MVP 阶段可用规则召回 + LLM 解释，无需训练模型。

### 3.5 小程序开发

- Taro 底部 TabBar 新增入口，需在 `app.config.ts` 注册页面。
- 聊天界面实现：消息列表、输入框、推荐卡片、历史会话。
- 流式输出：如果 LLM 支持 SSE，需要在小程序中处理流式响应。

---

## 4. 数据源与画像设计

### 4.1 可用数据表

| 表名 | 用途 | 敏感字段 | 脱敏要求 |
|------|------|---------|---------|
| `user` | 用户基础画像 | `phone`、`openid`、`union_id`、`name` | 脱敏/匿名化 |
| `coach` | 教练画像 | `phone`、`openid`、`union_id` | 脱敏/匿名化 |
| `package` | 用户已购套餐 | `user_id`、`coach_id` | 使用 hash 替代 |
| `order` | 历史订单 | `user_id`、`coach_id` | 使用 hash 替代 |
| `booking` | 上课记录 | `user_id`、`coach_id` | 使用 hash 替代 |
| `coach_student_profile` | 教练视角学员切片 | `student_user_id` | 使用 hash 替代 |
| `standard_package` / `package_template` | 可售套餐模板 | 无 | 可直接使用 |

### 4.2 用户画像字段

```json
{
  "userHash": "u_abc123",
  "ageGroup": "25-34",
  "gender": "female",
  "hasSwimBasis": true,
  "knownStrokes": ["breaststroke"],
  "targetStrokes": ["freestyle"],
  "swimYears": 1,
  "isMinor": false,
  "goal": "improve_freestyle",
  "budgetRange": "2000-3000",
  "preferredGender": "female",
  "preferredClassSize": "one_on_one"
}
```

### 4.3 教练画像字段

```json
{
  "coachHash": "c_xyz789",
  "nameAlias": "王教练",
  "gender": "female",
  "ageGroup": "30-39",
  "teachingYears": 8,
  "rating": 4.9,
  "totalStudents": 128,
  "totalHours": 2560,
  "teachingStrokes": ["freestyle", "breaststroke"],
  "specialty": "adult_beginner",
  "referencePrice": 200,
  "classSizes": ["one_on_one", "one_on_two"],
  "tags": ["耐心", "女教练", "成人教学"]
}
```

### 4.4 套餐画像字段

```json
{
  "packageId": 101,
  "name": "成人自由泳 10 节私教",
  "packageMode": "standard",
  "hours": 10,
  "price": 1800,
  "pricePerHour": 180,
  "validityDays": 90,
  "teachingType": "one_on_one",
  "durationMinutes": 60,
  "targetStrokes": ["freestyle"],
  "suitableFor": ["adult", "has_basic"]
}
```

---

## 5. 三种技术方案对比

### 5.1 方案 A：纯规则推荐

| 维度 | 说明 |
|------|------|
| 实现 | 后端写死规则：年龄 → 课程类型，泳姿 → 教练标签，预算 → 价格区间 |
| LLM 作用 | 无 |
| 优点 | 零 API 费用、响应快、完全可控、无幻觉 |
| 缺点 | 对话体验差，无法解释，复杂需求难处理 |
| 成本 | 低 |
| 开发周期 | 1-2 周 |
| 适用 | 预算极低或只想验证规则逻辑 |

### 5.2 方案 B：Python LangChain Agent + @tool + 规则召回 + LLM 解释（推荐）

| 维度 | 说明 |
|------|------|
| 实现 | 独立 Python AI Service（FastAPI + LangChain）+ `@tool` 注册业务工具 + LLM 原生 function calling + Java 后端提供数据 API |
| LLM 作用 | 理解用户需求、选择 Tool、解释推荐结果、多轮对话 |
| 优点 | Python LangChain 生态最成熟；真实 Agent 练手项目；LLM 通过 `@tool` 自主决定调用；可控性强、对话体验好 |
| 缺点 | 需要维护一个 Python 服务；Java 与 Python 之间需定义内部 API 契约 |
| 成本 | 中（按 Token 计费） |
| 开发周期 | 3-4 周 |
| 适用 | **MVP 首推** |

### 5.3 方案 C：RAG + 向量数据库 + 实时画像

| 维度 | 说明 |
|------|------|
| 实现 | 教练/套餐/用户画像向量化 → 向量检索召回 → LLM 排序解释 |
| LLM 作用 | 语义理解、排序、解释 |
| 优点 | 语义理解能力强，能处理复杂模糊需求 |
| 缺点 | 架构最重、需要向量库、Embedding 模型、数据清洗 |
| 成本 | 高 |
| 开发周期 | 6-8 周 |
| 适用 | 方案 B 跑通后的第二阶段 |

### 5.4 方案对比矩阵

| 维度 | 方案 A | 方案 B（推荐） | 方案 C |
|------|--------|---------------|--------|
| 实现复杂度 | 低 | 中 | 高 |
| 对话体验 | 差 | 好 | 很好 |
| 推荐可控性 | 强 | 强 | 中等 |
| LLM API 成本 | 无 | 中 | 高 |
| 学习价值 | 低 | 高 | 很高 |
| 数据隐私风险 | 低 | 中 | 中 |
| 维护成本 | 低 | 中 | 高 |
| MVP 适用度 | ★★ | ★★★★★ | ★★★ |

---

## 6. 推荐方案详细设计（方案 B）

### 6.1 总体架构

```
用户端小程序（Taro/React）
    │
    ▼
Java Spring Boot API Gateway（鉴权、会话管理、路由）
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

### 6.2 LangChain Agent 设计

#### 6.2.1 Agent 类型

使用 **Python LangChain 的 `create_tool_calling_agent`**，让 LLM 通过原生 function calling 自主决定调用哪个 `@tool`。

核心组件：

```python
from langchain import hub
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

# 1. 定义 Tools（@tool 装饰器会自动生成 schema）
@tool
def query_coaches(stroke: str, gender: str = None, max_price: int = None) -> list:
    """根据泳姿、教练性别、价格上限召回候选教练。"""
    ...

tools = [query_coaches, query_packages, get_user_profile, get_hot_recommendations]

# 2. 选择支持 function calling 的 LLM
llm = ChatOpenAI(model="qwen-plus", base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")

# 3. 构造 Agent
prompt = hub.pull("hwchase17/openai-tools-agent")
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)
```

> LLM 必须支持 `function calling` / `tool_calls`。国内模型中，通义千问（Qwen）、Kimi、DeepSeek、文心一言均支持。

#### 6.2.2 Tool 设计

| Tool 名称 | 输入 | 输出 | 用途 |
|-----------|------|------|------|
| `get_user_profile` | `userHash` | 用户画像 JSON | 已登录用户个性化推荐 |
| `get_user_packages` | `userHash` | 已购套餐列表 | 避免重复推荐、推荐续课 |
| `query_coaches` | `stroke`, `gender`, `minPrice`, `maxPrice`, `classSize` | 候选教练列表 | 召回教练 |
| `query_packages` | `stroke`, `packageMode`, `minPrice`, `maxPrice`, `hours` | 候选套餐列表 | 召回套餐 |
| `get_hot_recommendations` | `stroke`, `limit` | 热门教练/套餐 | 冷启动/游客默认推荐 |

#### 6.2.3 Tool 实现示例

以 `query_coaches` 为例，Python AI Service 中的 Tool 会调用 Java 后端的内部数据接口：

```python
from langchain_core.tools import tool
import httpx

INTERNAL_API_BASE = "http://leyo-backend:8080/api/internal/ai"

@tool
def query_coaches(stroke: str, gender: str = None, max_price: int = None, limit: int = 5) -> list:
    """
    根据目标泳姿、教练性别、价格上限召回候选教练。
    当用户想要找教练、推荐教练、或询问某位教练是否合适时调用。
    """
    params = {
        "stroke": stroke,
        "gender": gender,
        "maxPrice": max_price,
        "limit": limit
    }
    response = httpx.post(f"{INTERNAL_API_BASE}/coaches/query", json=params)
    response.raise_for_status()
    return response.json()["data"]
```

> 注意：Python Service 不直接访问 MySQL，所有业务数据查询都通过 Java 内部 API 完成，保证业务规则和数据脱敏逻辑统一收口在 Java 后端。

#### 6.2.4 推荐流程

```
1. 用户发送消息
2. Agent 判断意图：
   - 收集信息（年龄/泳姿/预算/目标）
   - 需要推荐 → 进入步骤 3
   - 闲聊/其它 → 直接回复
3. Agent 检查是否已有足够信息：
   - 不足 → 继续追问
   - 充足 → 调用 query_coaches / query_packages 召回候选
4. Agent 拿到候选后，调用 LLM 生成推荐理由和最终推荐卡片
5. 返回结构化 JSON 给前端：
   {
     "replyText": "...",
     "recommendations": [
       { "type": "coach", "id": 1, "reason": "..." },
       { "type": "package", "id": 101, "reason": "..." }
     ],
     "suggestedQuestions": ["...", "..."]
   }
```

### 6.3 两阶段推荐策略

为控制成本和幻觉，采用**规则召回 + LLM 排序解释**：

1. **召回阶段**：由业务规则/SQL 从 MySQL 查询 Top-K 候选。
2. **解释阶段**：把候选结果 + 用户画像脱敏后传给 LLM，生成推荐理由和自然语言回复。

### 6.4 多轮对话与 Memory

- 使用 Redis 存储会话上下文，Key：`ai:session:{userHash}:{sessionId}`。
- 每次请求带上 `sessionId`，后端读取最近 N 轮对话。
- 对历史消息做摘要，避免 Prompt 过长。

### 6.5 新建会话与历史会话

- 新建会话：生成新 `sessionId`，清空上下文。
- 历史会话：从 MySQL 查询用户的会话列表，展示最近 20 条。
- 会话标题：让 LLM 根据前两条消息自动生成摘要标题。

---

## 7. API 设计

遵循项目 [api-convention.md](../api-convention.md)：统一使用 POST，参数通过 JSON body 传递。

### 7.1 对外接口：小程序 → Java API Gateway

#### 7.1.1 POST /api/ai-assistant/chat

- **鉴权**：游客可访问（可选 `Authorization`），已登录用户带 JWT。
- **幂等**：是（`Idempotency-Key`）。
- **处理流程**：Java Gateway 鉴权 → 转发给 Python AI Service → 返回结果给小程序。

**Request Body**

```json
{
  "sessionId": "sess_abc123",
  "message": "我 30 岁，想学自由泳，预算 3000 左右，推荐个教练",
  "idempotencyKey": "uuid"
}
```

**Response 200**

```json
{
  "sessionId": "sess_abc123",
  "messageId": "msg_xyz789",
  "reply": {
    "text": "根据你的情况，推荐王教练的 10 节自由泳私教课。王教练 8 年教学经验，评分 4.9，擅长成人自由泳教学。",
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
      }
    ],
    "suggestedQuestions": ["这个教练能约什么时候？", "有没有更便宜的体验课？"]
  }
}
```

#### 7.1.2 POST /api/ai-assistant/session/list

- **鉴权**：是（已登录用户）。

**Request Body**

```json
{
  "sessionId": "sess_abc123",
  "message": "我 30 岁，想学自由泳，预算 3000 左右，推荐个教练",
  "idempotencyKey": "uuid"
}
```

**Response 200**

```json
{
  "sessionId": "sess_abc123",
  "messageId": "msg_xyz789",
  "reply": {
    "text": "根据你的情况，推荐王教练的 10 节自由泳私教课。王教练 8 年教学经验，评分 4.9，擅长成人自由泳教学。",
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
      }
    ],
    "suggestedQuestions": ["这个教练能约什么时候？", "有没有更便宜的体验课？"]
  }
}
```

#### 7.1.3 POST /api/ai-assistant/session/create

- **鉴权**：游客可访问，已登录用户带 JWT。

**Request Body**

```json
{}
```

**Response 200**

```json
{
  "sessionId": "sess_new123",
  "welcomeMessage": "你好，我是你的游泳学习助手，可以帮你推荐合适的教练和套餐。请问你今年多大，有没有游泳基础？"
}
```

#### 7.1.4 POST /api/ai-assistant/session/detail

- **鉴权**：会话所有者或已登录用户本人。

**Request Body**

```json
{
  "sessionId": "sess_abc123"
}
```

**Response 200**

```json
{
  "sessionId": "sess_abc123",
  "messages": [
    { "role": "user", "content": "我想学自由泳", "createdAt": "2026-08-15T09:55:00Z" },
    { "role": "assistant", "content": "好的，请问你今年多大？", "createdAt": "2026-08-15T09:55:05Z" }
  ]
}
```

### 7.2 内部接口：Python AI Service → Java 业务服务

以下接口仅供 Python AI Service 内部调用，不对外暴露，由 Java Gateway 通过 IP 白名单 / 内网 Token 限制访问。

#### 7.2.1 POST /api/internal/ai/coaches/query

- **用途**：召回候选教练。
- **调用方**：Python Tool `query_coaches`。

**Request Body**

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

**Response 200**

```json
{
  "data": [
    {
      "coachHash": "c_3e5b12",
      "nameAlias": "王教练",
      "gender": "female",
      "rating": 4.9,
      "referencePrice": 200,
      "teachingStrokes": ["freestyle", "breaststroke"]
    }
  ]
}
```

#### 7.2.2 POST /api/internal/ai/packages/query

- **用途**：召回候选套餐。
- **调用方**：Python Tool `query_packages`。

**Request Body**

```json
{
  "stroke": "freestyle",
  "packageMode": "standard",
  "minPrice": 1000,
  "maxPrice": 3000,
  "hours": 10,
  "limit": 5
}
```

#### 7.2.3 POST /api/internal/ai/user/profile

- **用途**：获取已登录用户脱敏画像。
- **调用方**：Python Tool `get_user_profile`。

**Request Body**

```json
{
  "userHash": "u_7d8f9a"
}
```

#### 7.2.4 POST /api/internal/ai/user/packages

- **用途**：获取用户已购套餐（用于避免重复推荐）。
- **调用方**：Python Tool `get_user_packages`。

**Request Body**

```json
{
  "userHash": "u_7d8f9a",
  "statuses": ["active", "exhausted"]
}
```

#### 7.2.5 POST /api/internal/ai/recommendations/hot

- **用途**：获取热门教练/套餐（冷启动场景）。
- **调用方**：Python Tool `get_hot_recommendations`。

**Request Body**

```json
{
  "stroke": "freestyle",
  "limit": 5
}
```

---

## 8. 数据脱敏与合规

### 8.1 脱敏策略

| 字段 | 处理方式 | 示例 |
|------|---------|------|
| `user.id` | 哈希为 `userHash` | `u_7d8f9a` |
| `coach.id` | 哈希为 `coachHash` | `c_3e5b12` |
| `phone` | 不进入 Prompt | - |
| `openid` / `union_id` | 不进入 Prompt | - |
| `user.name` | 不进入 Prompt；前端展示可用昵称或「您」 | - |
| `coach.name` | **可进入 Prompt 和前端展示**（教练公开信息，否则无法完成推荐） | 王教练 |
| `coach.avatarUrl` | 可保留（公开信息） | - |
| `coach.rating` / `teachingYears` / `referencePrice` | 可保留（公开信息） | - |

### 8.2 数据使用边界

- 游客：仅使用对话中主动输入的信息。
- 已登录用户：在获得授权后，可使用脱敏后的画像、已购套餐、历史订单。
- 不得将用户数据用于模型训练或持久化到 LLM 服务商（需在 LLM 服务协议中确认）。

### 8.3 日志与审计

- 记录每次推荐的输入、召回结果、LLM 输出，便于排查问题。
- 日志中不记录敏感字段。

---

## 9. 成本估算（方案 B）

### 9.1 成本构成

| 项目 | 估算 | 说明 |
|------|------|------|
| LLM API | ￥0.005-0.02 / 次对话 | 按国内模型低价档估算 |
| Redis | 已存在 | 复用现有实例 |
| MySQL | 已存在 | 新增 2-3 张表 |
| 开发人力 | 1 后端 + 1 前端，3-4 周 | 含 Prompt 调优 |
| 向量库 | MVP 不需要 | 方案 C 再考虑 |

### 9.2 月度成本估算

假设日活 100 人，每人 5 次对话：

```
100 人 × 5 次 × 30 天 = 15,000 次/月
15,000 × ￥0.01 = ￥150 / 月
```

实际成本随模型选择和 Prompt 长度浮动。

---

## 10. 风险与挑战

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| LLM 幻觉 | 推荐不存在的教练/套餐 | 强制 LLM 只从 Tool 返回结果中选择；输出结构化 JSON |
| 推荐不准确 | 用户体验差 | 规则召回保证候选质量；A/B 测试和人工评估 |
| 数据隐私 | 敏感信息泄露 | 脱敏后进入 Prompt；最小权限原则 |
| 成本超支 | LLM 调用费用高 | 限制单会话轮数；缓存热门推荐；使用低价模型 |
| 延迟高 | 用户体验差 | 异步调用 LLM；流式输出；缓存 |
| 小程序审核 | AI 功能需合规 | 提前确认微信对 AI 推荐的审核要求 |

---

## 11. 分阶段实施建议

### 第一阶段：MVP（4 周）

- 实现底部 TabBar「AI 助理」入口页面。
- 实现基础对话、新建会话、历史会话。
- 搭建独立 Python AI Service（FastAPI + LangChain），接入 1 个国内 LLM 服务。
- 用 `@tool` 注册 3-4 个核心 Tool：`query_coaches`、`query_packages`、`get_user_profile`、`get_hot_recommendations`。
- Java 后端提供对应的内部数据接口 `/api/internal/ai/*`，统一做数据查询和脱敏。
- 支持游客基础推荐和已登录用户个性化推荐。
- 数据脱敏与推荐日志。

### 第二阶段：增强体验（2-3 周）

- 引入 Embedding 做语义检索（教练/套餐描述向量化）。
- 支持更复杂的自然语言理解，例如「我想找耐心一点的女教练」。
- 推荐卡片支持直接跳转教练详情页/套餐详情页。
- 支持流式输出。

### 第三阶段：实时与闭环（3-4 周）

- 接入 `schedule_slot` 实时可约时段。
- 推荐结果支持「现在可约」的教练。
- 根据用户点击、购买行为做反馈优化。

---

## 12. 验收标准

- [ ] 游客可通过 3 轮以内对话获得至少 1 个教练 + 1 个套餐推荐。
- [ ] 已登录用户可获得结合画像的个性化推荐。
- [ ] 推荐结果必须来自真实数据库，不得 hallucinate。
- [ ] 支持多轮对话、新建会话、查看历史会话。
- [ ] 用户敏感数据不脱敏不得进入 LLM Prompt。
- [ ] P99 响应延迟 < 3s（不含网络）。
- [ ] 单元测试覆盖核心 Tool 和脱敏逻辑。

---

## 13. 前置工作清单（开发前必须完成）

- [ ] 第二批次核心 US 开发完成（US-001 / US-005 / US-017 / US-019 / US-020 / US-021 / US-025 / US-026 / US-037 / US-043 / US-045 / US-046 等）。
- [ ] 编写并执行 mock 数据脚本，覆盖用户、教练、套餐、订单、package、booking。
- [ ] 确认 LLM 服务商并充值/配置 API Key。
- [ ] Calicat 完成「AI 助理」页面设计稿。
- [ ] 更新隐私协议与用户须知，获取用户授权。
- [ ] 搭建 Python 3.10+ 环境，确认 FastAPI/Flask + LangChain 依赖版本。
- [ ] 确定 Python AI Service 与 Java 后端之间的内网通信方式（HTTP + Token / gRPC）。

---

## 14. 结论

- **可行性**：高。当前技术栈（Spring Boot + Redis + MySQL）完全支持，新增独立 Python AI Service 即可接入 LangChain，且用户接受 LLM API 付费。
- **推荐方案**：**方案 B：Python LangChain Agent + `@tool` + 规则召回 + LLM 解释**。
- **核心价值**：在可控成本内实现一个真实的 Python LangChain Agent 练手项目，LLM 通过原生 function calling 自主决定调用业务 Tool，同时提升游客转化和学员体验。
- **下一步**：评审本报告 → 创建用户故事 US-0XX → 编写 Python AI Service 的 tech-design / test-plan / page-spec → 进入开发。

---

## 15. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-08-15 | AI | 初版：背景、前置工作、三种方案、推荐方案设计、API、成本、风险、分阶段实施 |
| v0.2 | 2026-08-15 | AI | 明确使用 Python 版 LangChain + `@tool` + function calling；更新架构图为 Java Gateway + Python AI Service 双服务；补充内部接口 `/api/internal/ai/*`；更新前置工作与结论 |
| v0.3 | 2026-08-15 | AI | 修正脱敏策略：教练公开信息（姓名、头像、评分、价格等）可进入 Prompt 和前端展示；新增可交互 HTML 原型 `prototype.html` |