# US-058 AI 助理功能开发总计划

> **目标**：完成用户端「leyo AI 助理」端到端交付，支持游客/登录用户的多轮对话推荐（教练、标准/体验套餐、自定义套餐）、新建会话、历史会话、快捷标签与底部固定输入。
>
> **架构**：Taro 小程序 → Java Spring Boot API Gateway（鉴权 / 会话持久化 / 路由） → Python FastAPI + LangChain AI Service → Java 内部数据接口 → MySQL / Redis。
>
> **技术栈**：Taro 3 + React 18 + TypeScript + SCSS，Spring Boot 3 + MyBatis-Plus，Python 3.11 + FastAPI + LangChain，Redis，MySQL 8。
>
> **对应文档**：
> - 业务需求：[docs/stories/US-058-用户-AI助理推荐教练与套餐/user-story.md](../stories/US-058-用户-AI助理推荐教练与套餐/user-story.md)
> - 技术设计：[docs/stories/US-058-用户-AI助理推荐教练与套餐/tech-design.md](../stories/US-058-用户-AI助理推荐教练与套餐/tech-design.md)
> - 页面规格：[docs/figma/page-spec/U-AI-assistant-page.md](../figma/page-spec/U-AI-assistant-page.md)
> - 可行性分析：[docs/tech/AI-assistant-requirements/README.md](./AI-assistant-requirements/README.md)

---

## 0. 当前已就绪资产

| 资产 | 路径 | 状态 |
|------|------|------|
| Page-spec 已补充 Calicat 设计稿链接 | [U-AI-assistant-page.md §9](../figma/page-spec/U-AI-assistant-page.md) | ✅ |
| Calicat 关键 Frame 截图 | `miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-*.png` | ✅ |
| Design Tokens | `miniapp-user/src/assets/calicat/tokens/U-ai-assistant.json` | ✅ |
| SCSS 变量覆盖 | `miniapp-user/src/styles/calicat-overrides.scss` | ✅ |
| Python AI Service 骨架 | `ai-service/app/main.py`、`chat_service.py`、tools、tests | ✅ |
| Java 后端 AI 相关接口 | 暂无 | ❌ |
| 用户端 AI 助理页面 | 暂无 | ❌ |

---

## 1. 总体调用链

```text
┌─────────────────────────────────────┐
│  用户端小程序（Taro）                │
│  /pages/ai-assistant/index           │
└──────────────┬──────────────────────┘
               │ POST /api/ai-assistant/*  （可选 JWT）
               ▼
┌─────────────────────────────────────┐
│  Java API Gateway                    │
│  - 鉴权（游客 / 登录用户）            │
│  - user_id → user_hash               │
│  - 会话持久化（ai_chat_session/msg）  │
│  - 幂等 / 限流 / 日志                 │
│  - 透传至 Python AI Service           │
└──────────────┬──────────────────────┘
               │ POST http://leyo-ai-service:8000/api/ai-assistant/*
               │ Header: X-Internal-Token
               ▼
┌─────────────────────────────────────┐
│  Python AI Service（FastAPI）        │
│  - LangChain Agent + @tool           │
│  - Redis 会话 Memory                 │
│  - LLM 调用 / 结构化回复              │
└──────────────┬──────────────────────┘
               │ POST /api/internal/ai/*
               │ Header: X-Internal-Token
               ▼
┌─────────────────────────────────────┐
│  Java 业务服务                       │
│  - coaches/query                     │
│  - packages/query（standard/experience/custom）
│  - user/profile、user/packages       │
│  - recommendations/hot               │
└─────────────────────────────────────┘
```

---

## 2. 接口契约（必须先冻结，再并行开发）

### 2.1 小程序 ↔ Java Gateway

统一返回 `ApiResponse<T>` 结构：`{ code: 0, message: "ok", data: T }`。

#### 2.1.1 发送消息

```http
POST /api/ai-assistant/chat
Authorization: Bearer {jwt}  // 游客可省略
Idempotency-Key: {uuid}      // 可选，重复提交返回缓存结果
Content-Type: application/json
```

Request：

```json
{
  "sessionId": "sess_abc123",
  "message": "推荐一个自由泳教练"
}
```

Response `data`：

```json
{
  "sessionId": "sess_abc123",
  "messageId": "msg_xyz789",
  "reply": {
    "text": "根据你的需求，leyo 推荐以下教练：",
    "recommendations": [
      {
        "type": "coach",
        "id": 1,
        "name": "王教练",
        "avatarUrl": "https://cdn.example.com/avatar/1.jpg",
        "rating": 4.9,
        "teachingYears": 8,
        "referencePrice": 200,
        "strokes": ["自由泳", "蛙泳"],
        "reason": "8 年教学经验，擅长成人自由泳"
      },
      {
        "type": "package",
        "id": 101,
        "name": "成人自由泳 10 节私教",
        "price": 1800,
        "hours": 10,
        "classSize": "一对一",
        "validityDays": 90,
        "strokes": ["自由泳"],
        "reason": "单节课 180 元，性价比高"
      },
      {
        "type": "custom_package",
        "coachId": 1,
        "coachName": "王教练",
        "hours": 12,
        "pricePerHour": 200,
        "totalPrice": 2400,
        "classSize": "一对一",
        "strokes": ["自由泳"],
        "reason": "标准套餐没有 12 节配置，可按你需求自定义"
      }
    ],
    "suggestedQuestions": ["这个教练能约什么时候？", "有没有更便宜的体验课？"]
  }
}
```

#### 2.1.2 新建会话

```http
POST /api/ai-assistant/session/create
Authorization: Bearer {jwt}  // 游客可省略
```

Request：`{}`

Response `data`：

```json
{
  "sessionId": "sess_new123",
  "welcomeMessage": "你好！我是 leyo，你的游泳学习助手。"
}
```

#### 2.1.3 历史会话列表

```http
POST /api/ai-assistant/session/list
Authorization: Bearer {jwt}  // 必须登录
```

Request：

```json
{
  "page": 1,
  "size": 20
}
```

Response `data`：

```json
{
  "items": [
    {
      "sessionId": "sess_abc123",
      "title": "自由泳教练推荐",
      "lastMessageAt": "2026-08-15T10:00:00+08:00",
      "messageCount": 8
    }
  ],
  "total": 5,
  "page": 1,
  "size": 20
}
```

#### 2.1.4 会话详情

```http
POST /api/ai-assistant/session/detail
Authorization: Bearer {jwt}  // 必须登录
```

Request：

```json
{
  "sessionId": "sess_abc123"
}
```

Response `data`：

```json
{
  "sessionId": "sess_abc123",
  "messages": [
    { "role": "user", "content": "我想学自由泳", "createdAt": "2026-08-15T09:55:00+08:00" },
    { "role": "assistant", "content": "好的，leyo 给你推荐...", "createdAt": "2026-08-15T09:55:05+08:00" }
  ]
}
```

#### 2.1.5 鉴权规则

| 接口 | 游客 | 登录用户 | 说明 |
|------|:----:|:--------:|------|
| `/api/ai-assistant/chat` | ✅ | ✅ | 游客不传 JWT；Gateway 生成 `user_hash=null` |
| `/api/ai-assistant/session/create` | ✅ | ✅ | 同上 |
| `/api/ai-assistant/session/list` | ❌ | ✅ | 返回当前登录用户的历史会话 |
| `/api/ai-assistant/session/detail` | ❌ | ✅ | 校验会话属于当前用户 |

### 2.2 Java Gateway ↔ Python AI Service

Python 服务地址：`http://leyo-ai-service:8000`。

| 用途 | 路径 | 方法 | Header |
|------|------|------|--------|
| 聊天 | `/api/ai-assistant/chat` | POST | `X-Internal-Token: {INTERNAL_API_TOKEN}` |
| 新建会话 | `/api/ai-assistant/session/create` | POST | 同上 |
| 历史列表 | `/api/ai-assistant/session/list` | POST | 同上 |
| 会话详情 | `/api/ai-assistant/session/detail` | POST | 同上 |

Gateway 在转发 `chat` 时，从 JWT 提取 `user_id`，按 `user_hash = "u_" + sha256Hex("leyo-ai:" + user_id)` 生成，并加入 body：

```json
{
  "session_id": "sess_abc123",
  "message": "推荐一个自由泳教练",
  "user_hash": "u_7d8f9a"
}
```

游客不传 `user_hash`。

### 2.3 Python AI Service ↔ Java 内部数据接口

前缀 `/api/internal/ai`，仅允许 `X-Internal-Token` 访问。

#### 2.3.1 召回教练

```http
POST /api/internal/ai/coaches/query
```

```json
{
  "stroke": "freestyle",
  "gender": "female",
  "minPrice": 100,
  "maxPrice": 300,
  "classSize": "一对一",
  "limit": 5
}
```

Response：

```json
{
  "data": [
    {
      "coach_hash": "c_3e5b12",
      "coach_id": 1,
      "name": "王教练",
      "avatar_url": "https://...",
      "gender": "female",
      "rating": 4.9,
      "reference_price": 200,
      "teaching_years": 8,
      "teaching_strokes": ["freestyle", "breaststroke"]
    }
  ]
}
```

#### 2.3.2 召回套餐

```http
POST /api/internal/ai/packages/query
```

```json
{
  "stroke": "freestyle",
  "packageMode": "standard",
  "minPrice": 1000,
  "maxPrice": 3000,
  "hours": 12,
  "limit": 5
}
```

- `packageMode` 枚举：`standard`、`experience`、`custom`。
- `custom` 时返回可接自定义课的教练列表，每个条目带 `coach_id`、`reference_price`、`teaching_strokes`，前端/LLM 用 `hours × reference_price` 计算总价。

Response（standard）：

```json
{
  "data": [
    {
      "package_hash": "p_10ab01",
      "package_id": 101,
      "package_mode": "standard",
      "name": "成人自由泳 10 节私教",
      "hours": 10,
      "price": 1800,
      "price_per_hour": 180,
      "validity_days": 90,
      "class_size": "一对一",
      "strokes": ["freestyle"]
    }
  ]
}
```

Response（custom）：

```json
{
  "data": [
    {
      "type": "custom_package",
      "coach_hash": "c_3e5b12",
      "coach_id": 1,
      "name": "王教练",
      "avatar_url": "https://...",
      "reference_price": 200,
      "class_size": "一对一",
      "teaching_strokes": ["freestyle"]
    }
  ]
}
```

#### 2.3.3 用户画像

```http
POST /api/internal/ai/user/profile
```

```json
{
  "userHash": "u_7d8f9a"
}
```

Response `data`：

```json
{
  "user_hash": "u_7d8f9a",
  "age": 30,
  "target_stroke": "freestyle",
  "swimming_level": "beginner",
  "budget": 3000,
  "is_minor": false
}
```

#### 2.3.4 已购套餐

```http
POST /api/internal/ai/user/packages
```

```json
{
  "userHash": "u_7d8f9a",
  "statuses": ["active", "exhausted"]
}
```

Response `data`：

```json
{
  "data": [
    {
      "package_hash": "p_user_01",
      "package_id": 1001,
      "name": "成人自由泳 10 节私教",
      "status": "active",
      "hours": 10,
      "remaining_hours": 6,
      "strokes": ["freestyle"]
    }
  ]
}
```

#### 2.3.5 热门推荐

```http
POST /api/internal/ai/recommendations/hot
```

```json
{
  "stroke": "freestyle",
  "limit": 5
}
```

Response `data`：混合教练与套餐条目，字段同上。

---

## 3. 数据库变更（Java 后端）

新增 Flyway 脚本，例如 `backend/src/main/resources/db/migration/V20260818__add_ai_chat_tables.sql`：

```sql
CREATE TABLE ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务会话ID',
    user_id BIGINT COMMENT '登录用户ID，游客为NULL',
    title VARCHAR(64) COMMENT '会话标题，取首条用户消息前20字',
    status TINYINT DEFAULT 0 COMMENT '0 active 1 archived',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_user (user_id, status, updated_at)
) COMMENT='AI助理会话';

CREATE TABLE ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务消息ID',
    session_id VARCHAR(64) NOT NULL COMMENT '业务会话ID',
    role VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool',
    content TEXT COMMENT '文本内容',
    recommendations JSON COMMENT '推荐卡片JSON',
    tool_calls JSON COMMENT 'Tool调用记录',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_session (session_id, created_at)
) COMMENT='AI助理消息';

CREATE TABLE ai_recommendation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    input TEXT NOT NULL,
    tool_calls JSON,
    llm_response TEXT,
    final_response JSON,
    latency_ms INT,
    llm_latency_ms INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_session (session_id, created_at),
    INDEX idx_log_created (created_at)
) COMMENT='AI推荐日志';
```

---

## 4. 任务分解

### A. 前端（用户端小程序）

#### A1. 页面注册与 TabBar

- **创建**：`miniapp-user/src/pages/ai-assistant/index.tsx`、`index.scss`、`index.config.ts`
- **修改**：`miniapp-user/src/app.config.ts`
  - pages 数组追加 `pages/ai-assistant/index`
  - tabBar.list 追加 `{ pagePath: 'pages/ai-assistant/index', text: 'leyo' }`
- **实现**：`pages/ai-assistant/index.config.ts` 设置 `navigationStyle: 'custom'`
- **验收**：`npm run dev:weapp` 后 TabBar 出现 leyo，点击进入页面不报错。

#### A2. API 层

- **创建**：`miniapp-user/src/api/ai-assistant.ts`

```typescript
import { request } from './request';

export interface ChatMessageRequest {
  sessionId: string;
  message: string;
}

export interface RecommendationItem {
  type: 'coach' | 'package' | 'custom_package';
  id?: number;
  coachId?: number;
  name: string;
  coachName?: string;
  avatarUrl?: string;
  rating?: number;
  teachingYears?: number;
  referencePrice?: number;
  price?: number;
  hours?: number;
  pricePerHour?: number;
  totalPrice?: number;
  classSize?: string;
  validityDays?: number;
  strokes?: string[];
  reason: string;
}

export interface ChatReply {
  text: string;
  recommendations: RecommendationItem[];
  suggestedQuestions: string[];
}

export interface ChatResponse {
  sessionId: string;
  messageId: string;
  reply: ChatReply;
}

export function chat(data: ChatMessageRequest) {
  return request<ChatResponse>({
    url: '/ai-assistant/chat',
    method: 'POST',
    data,
    needToken: false, // 游客也可访问
  });
}

export function createSession() {
  return request<{ sessionId: string; welcomeMessage: string }>({
    url: '/ai-assistant/session/create',
    method: 'POST',
    data: {},
    needToken: false,
  });
}

export function listSessions(page = 1, size = 20) {
  return request<{ items: HistorySession[]; total: number; page: number; size: number }>({
    url: '/ai-assistant/session/list',
    method: 'POST',
    data: { page, size },
    needToken: true,
  });
}

export function getSessionDetail(sessionId: string) {
  return request<{ sessionId: string; messages: ChatMessage[] }>({
    url: '/ai-assistant/session/detail',
    method: 'POST',
    data: { sessionId },
    needToken: true,
  });
}
```

- **创建类型文件**：`miniapp-user/src/types/ai-assistant.ts`
- **验收**：`npm run lint` 通过，类型无报错。

#### A3. 页面结构与组件拆分

按 Calicat 图层自上而下实现，禁止从 page-spec 推断颜色/尺寸。

```text
miniapp-user/src/pages/ai-assistant/
├── index.tsx              # 页面主组件：状态管理、生命周期
├── index.scss             # 页面级样式，仅布局与层级
├── index.config.ts        # 自定义导航栏
├── components/
│   ├── AiHeader.tsx       # 顶部栏：新会话、标题、历史
│   ├── AiHeader.scss
│   ├── AiChatArea.tsx     # 聊天滚动区
│   ├── AiChatArea.scss
│   ├── AiWelcome.tsx      # leyo 头像 + 欢迎语
│   ├── AiQuickTags.tsx    # 快捷标签
│   ├── AiMessageBubble.tsx# 用户/leyo 气泡
│   ├── AiLoadingIndicator.tsx # 输入中动画
│   ├── AiRecommendationCard.tsx # 教练/套餐/自定义卡片
│   ├── AiSuggestedQuestions.tsx
│   ├── AiInputBar.tsx     # 底部固定输入框 + 发送
│   ├── AiInputBar.scss
│   └── AiHistoryDrawer.tsx# 历史会话抽屉
```

- **验收**：
  - 空状态展示欢迎语 + 快捷标签。
  - 发送消息后展示用户气泡 + loading + leyo 回复。
  - 推荐卡片按 type 渲染，点击跳转。
  - 历史抽屉从右侧滑出，点击加载会话。
  - 底部输入区 `position: fixed` 或 flex 固定，不随聊天滚动。

#### A4. 跳转与参数映射

| 卡片类型 | 跳转目标 | 参数 |
|----------|----------|------|
| coach | `/pages/coach/detail/index` | `?id={coachId}` |
| package | `/pages/package/detail/index` | `?templateId={id}` |
| custom_package | `/pages/package/detail/index` | `?mode=custom&coachId={coachId}&hours={hours}&classSize={classSize}&stroke={stroke}` |

- **验收**：每个跳转携带正确参数，目标页正常渲染。

#### A5. 四态与错误处理

| 状态 | 表现 |
|------|------|
| 空状态 | 欢迎语 + 快捷标签 |
| 加载中 | leyo 输入中指示器 + 推荐卡片占位 |
| 错误状态 | 「leyo 暂时走神了，请重试」+ 重试按钮 |
| 成功状态 | 完整展示回复文本、推荐卡片、建议追问 |

- **验收**：断网/500 时展示错误状态；重试重新发送同一条消息。

#### A6. 前端单元测试

- **创建**：`miniapp-user/src/pages/ai-assistant/index.test.tsx`
- 覆盖：快捷标签点击发送、推荐卡片渲染、历史抽屉开关、空输入禁用发送按钮。
- **命令**：`cd miniapp-user && npm test -- ai-assistant`
- **验收**：测试通过，覆盖率 ≥ 80%。

---

### B. Java Gateway / 内部数据接口

#### B1. 数据库实体与 Mapper

- **创建实体**：
  - `backend/src/main/java/com/leyoswimming/entity/AiChatSession.java`
  - `backend/src/main/java/com/leyoswimming/entity/AiChatMessage.java`
  - `backend/src/main/java/com/leyoswimming/entity/AiRecommendationLog.java`
- **创建 Mapper**：
  - `backend/src/main/java/com/leyoswimming/repository/AiChatSessionMapper.java`
  - `backend/src/main/java/com/leyoswimming/repository/AiChatMessageMapper.java`
  - `backend/src/main/java/com/leyoswimming/repository/AiRecommendationLogMapper.java`
- **验收**：`mvnw test` 中 MyBatis 映射无报错。

#### B2. 内部数据接口（仅 Python 可访问）

- **创建 Controller**：`backend/src/main/java/com/leyoswimming/controller/internal/InternalAiController.java`
- 路径 `/api/internal/ai`，方法：
  - `POST /coaches/query`
  - `POST /packages/query`
  - `POST /user/profile`
  - `POST /user/packages`
  - `POST /recommendations/hot`
- 复用现有 `UserCoachService`、`UserPackageTemplateService`、`UserProfileService` 查询，结果做匿名化（`coach_hash`、`package_hash`、`user_hash`）。
- **验收**：未带 `X-Internal-Token` 返回 401；带正确 Token 返回 JSON。

#### B3. 内部接口鉴权过滤器

- **创建**：`backend/src/main/java/com/leyoswimming/security/InternalAuthFilter.java`
- 拦截 `/api/internal/ai/**`，校验 `X-Internal-Token` 与 `leyo.internal.api-token` 配置一致。
- **修改**：`SecurityConfig.java` 添加该 filter，并 permitAll `/api/internal/ai/**`（由 filter 自行鉴权）。
- **验收**：错误 Token 返回 401；正确 Token 放行。

#### B4. AI Gateway 控制器

- **创建**：`backend/src/main/java/com/leyoswimming/controller/user/UserAiAssistantController.java`
- 路径 `/api/ai-assistant/*`。
- 职责：
  - 从 JWT 提取 `user_id`，生成 `user_hash`。
  - 调用 Python AI Service（RestClient / WebClient）。
  - 保存会话/消息到 `ai_chat_session`、`ai_chat_message`。
  - 记录 `ai_recommendation_log`。
  - 处理幂等 `Idempotency-Key`（Redis / DB）。
- **创建 DTO**：
  - `AiChatRequest`、`AiChatResponse`、`AiSessionCreateResponse`、`AiSessionListRequest`、`AiSessionListResponse`、`AiSessionDetailRequest`、`AiSessionDetailResponse`
- **创建 Service**：`AiAssistantGatewayService`
- **验收**：
  - 游客调用 `/chat` 返回成功。
  - 登录用户历史列表只返回自己的会话。
  - 会话详情校验所有权。

#### B5. Python 服务调用配置

- **修改**：`backend/src/main/resources/application-dev.yml`

```yaml
leyo:
  ai-service:
    base-url: http://leyo-ai-service:8000
    internal-api-token: ${INTERNAL_API_TOKEN:dev-internal-token}
```

- **创建工具类**：`backend/src/main/java/com/leyoswimming/util/AiUserHashUtil.java`

```java
public final class AiUserHashUtil {
  public static String hash(Long userId) {
    if (userId == null) return null;
    return "u_" + DigestUtils.sha256Hex("leyo-ai:" + userId).substring(0, 16);
  }
}
```

- **验收**：配置加载正常，hash 输出稳定。

#### B6. 安全与限流

- `SecurityConfig` 放行 `/api/ai-assistant/chat`、`/api/ai-assistant/session/create` 给匿名用户。
- `/api/ai-assistant/session/list`、`/api/ai-assistant/session/detail` 要求 `ROLE_USER`。
- 在 Gateway Service 内基于 Redis 实现单用户 30 次/分钟、单 IP 60 次/分钟限流。
- **验收**：超出频率返回 `TOO_MANY_REQUESTS`。

#### B7. Java 测试

- **创建**：
  - `InternalAiControllerTest`
  - `UserAiAssistantControllerTest`
  - `AiAssistantGatewayServiceTest`
- **命令**：`cd backend && ./mvnw test -Dtest=InternalAiControllerTest,UserAiAssistantControllerTest`
- **验收**：测试通过。

---

### C. Python AI Service

#### C1. 扩展推荐卡片 Schema

- **修改**：`ai-service/app/models/schemas.py`

```python
from typing import Literal

class RecommendationItem(BaseModel):
    type: Literal["coach", "package", "custom_package"]
    id: int | None = None
    name: str
    reason: str
    avatar_url: str | None = None
    rating: float | None = None
    teaching_years: int | None = None
    reference_price: int | None = None
    price: int | None = None
    hours: int | None = None
    price_per_hour: int | None = None
    total_price: int | None = None
    class_size: str | None = None
    validity_days: int | None = None
    strokes: list[str] | None = None
    coach_id: int | None = None
    coach_name: str | None = None
```

- **验收**：`cd ai-service && pytest tests/test_chat_service_unit.py` 通过。

#### C2. 完善 Tool 与自定义套餐

- **修改**：`ai-service/app/tools/recommendation_tools.py`
  - `query_packages` 增加 `package_mode="custom"` 的透传，不再把 `custom` 识别为 unknown。
  - 可新增 `query_custom_packages` Tool，语义更清晰。
- **修改**：`ai-service/app/services/chat_service.py` 的 `_extract_recommendations`
  - 识别 `type == "custom_package"` 或 `package_mode == "custom"`。
  - 计算 `total_price = hours × price_per_hour`。
  - 生成 `RecommendationItem(type="custom_package", ...)`。
- **验收**：
  - `pytest tests/test_tools.py`
  - 自定义套餐场景返回正确 `total_price` 与 `coach_id`。

#### C3. Prompt 与系统提示调整

- 在 `SYSTEM_PROMPT` 中明确：
  - 当用户需求在标准/体验套餐中无匹配时，主动推荐 `custom_package`。
  - 自定义套餐必须给出 `coach_id`、`hours`、`class_size`。
- **验收**：运行本地 chat 测试，LLM 回复包含自定义套餐卡片。

#### C4. 会话历史持久化对接

- 当前 Python `list_sessions` 返回空列表；实际历史由 Java Gateway 维护 DB，Python 不暴露历史列表给前端。
- 保持 `session/create`、`session/detail`、`chat` 供 Gateway 调用即可。

#### C5. Python 测试

- **命令**：`cd ai-service && pytest`
- **验收**：全部通过（当前已有 41 个用例）。

---

### D. 集成与 DevOps

#### D1. Nginx / 网关路由

当前 Nginx 已把所有 `/api/` 路由到 `leyo-backend:8080`。Java Gateway 部署在 backend 内，无需额外 Nginx 改动。

如需让 Python 服务可被外部直接访问（调试用），可在 `deploy/nginx.conf` 增加：

```nginx
location /api/ai-assistant/ {
    proxy_pass http://leyo-ai-service:8000;
    ...
}
```

但生产/联调推荐只走 Java Gateway。

#### D2. 环境变量与容器

- **修改**：`deploy/.env.example` 和 `deploy/.env`
  - `INTERNAL_API_TOKEN=dev-internal-token`
  - `LLM_API_KEY=`（本地开发可留空，启用 mock）
  - `ENABLE_MOCK_DATA=true`（Java 内部接口未就绪时）；接口就绪后改为 `false`。
- **验证**：`docker compose up -d` 后 backend 与 ai-service 互相 `ping` 通。

#### D3. 端到端冒烟

```bash
# 1. 创建会话
curl -X POST http://localhost/api/ai-assistant/session/create \
  -H "Content-Type: application/json" -d '{}'

# 2. 发送消息
curl -X POST http://localhost/api/ai-assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"sess_xxx","message":"推荐自由泳教练"}'

# 3. 登录后历史列表
curl -X POST http://localhost/api/ai-assistant/session/list \
  -H "Authorization: Bearer {jwt}" \
  -H "Content-Type: application/json" \
  -d '{"page":1,"size":20}'
```

- **验收**：返回 200，结构符合契约。

---

## 5. 前后端并行开发方案

**核心原则：先冻结接口契约（本计划 §2），三方再并行开发。**

```text
Day 0  接口契约评审 & 冻结
       ├─ 前端确认字段与跳转参数
       ├─ Java 确认内部数据查询逻辑
       └─ Python 确认 Tool 输入输出

Day 1  并行启动
       ├─ 前端：页面骨架 + Mock 数据 + 组件拆分
       ├─ Java：Flyway 表 + 内部接口 + Gateway 透传
       └─ Python：Schema 扩展 + custom_package 支持 + 测试

Day 2  并行推进
       ├─ 前端：推荐卡片、历史抽屉、输入区、跳转
       ├─ Java：Gateway 持久化、鉴权、限流、测试
       └─ Python：Prompt 调优、LLM 集成测试

Day 3  联调
       ├─ Java ↔ Python 内部接口打通
       ├─ 前端 ↔ Java Gateway 联调
       └─ 修复契约偏差

Day 4  验证 & 合入
       ├─ 单元 / 集成 / E2E 测试
       ├─ visual-review 与 code-reviewer
       └─ 文档归档
```

**互不阻塞的方法：**

- 前端使用本地 Mock（`miniapp-user/src/mocks/ai-assistant.ts`）模拟 `/api/ai-assistant/*` 响应，Mock 结构严格按 §2.1。
- Java 开发内部接口时，启用 Python `ENABLE_MOCK_DATA=true`，无需等真实 Java 内部接口完成即可验证 Gateway 透传。
- Python 扩展 custom_package 时，使用 `java_client.py` 的 Mock 数据，无需等 Java 接口。

---

## 6. 验证方法

### 6.1 自动化测试

| 层级 | 命令 | 通过标准 |
|------|------|----------|
| Java 单元/集成 | `cd backend && ./mvnw test` | 全部通过 |
| Python 单元/集成 | `cd ai-service && pytest` | 全部通过 |
| 前端单元 | `cd miniapp-user && npm test` | 全部通过 |

### 6.2 接口验证

使用 `curl` 或 Postman 执行 §4.3 与 §4.4 的冒烟脚本，覆盖：

- 游客聊天（无 JWT）
- 登录用户聊天（有 JWT，生成 user_hash）
- 历史会话 CRUD
- 内部接口 Token 鉴权失败/成功
- 自定义套餐推荐

### 6.3 视觉还原验证

1. 导出 Calicat 截图：`miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-*.png`。
2. 微信开发者工具运行页面，截取实现图。
3. 并排对比，输出差异清单。
4. 调用 `visual-review` Agent 检查：
   - 颜色 HEX 误差 ≤1%
   - 间距 ≤2px
   - 字体字号一致
   - 元素位置/尺寸一致
   - 静态资源来自 Calicat

### 6.4 端到端场景验证

按 US-058 验收标准执行：

1. 游客点击快捷标签「推荐自由泳教练」→ 返回教练卡片。
2. 已登录用户输入预算/泳姿 → 返回套餐卡片。
3. 已登录用户有 active 套餐 → 不重复推荐。
4. 用户请求 12 节自由泳一对一但标准套餐无匹配 → 返回自定义套餐卡片，点击跳转套餐详情。
5. 新建会话 → 清空聊天、恢复欢迎语。
6. 查看历史会话 → 抽屉加载、切换会话。
7. LLM 超时 → 前端展示错误与重试。
8. 敏感输入 → 不调用 Tool，返回友好提示。

### 6.5 性能验证

- 使用 `curl -w "@curl-format.txt"` 或 k6 压测 `/api/ai-assistant/chat`：
  - P99 < 5s（含 LLM 调用）
  - `/api/internal/ai/*` P99 < 100ms
- Redis 限流：单用户 30 次/分钟、单 IP 60 次/分钟。

### 6.6 安全验证

- 内部接口 `/api/internal/ai/**` 仅 Python 可访问。
- LLM Prompt 与用户日志中不出现手机号、openid、union_id、真实姓名。
- 用户输入长度限制 500 字；含敏感词时拒绝调用 Tool。
- JWT 鉴权：游客不能访问历史会话接口。

---

## 7. 交付物与验收标准

| 交付物 | 路径 | 验收标准 |
|--------|------|----------|
| 本开发计划 | `docs/tech/ai-assistant-dev-plan.md` | 已覆盖任务、接口、并行方案、验证方法 |
| 用户端页面 | `miniapp-user/src/pages/ai-assistant/**` | lint/test 通过，视觉还原无 Block |
| Java Gateway | `backend/src/main/java/com/leyoswimming/controller/user/UserAiAssistantController.java` 等 | `mvnw test` 通过 |
| 内部数据接口 | `backend/src/main/java/com/leyoswimming/controller/internal/InternalAiController.java` | Token 鉴权 + 数据查询正常 |
| Python 扩展 | `ai-service/app/models/schemas.py`、`tools/recommendation_tools.py`、`services/chat_service.py` | `pytest` 通过 |
| 数据库迁移 | `backend/src/main/resources/db/migration/V20260818__add_ai_chat_tables.sql` | Flyway 执行成功 |
| 测试用例 | 各层 `*Test.java`、`*test.tsx`、`tests/test_*.py` | 覆盖率 ≥ 80% |
| 视觉对比报告 | `tmp/ai-assistant-visual-diff.md` | 无 HIGH/Block 问题 |

---

## 8. 风险与回退

| 风险 | 影响 | 应对 |
|------|------|------|
| LLM API 不稳定或成本过高 | 功能不可用 | 保留 `ENABLE_MOCK_DATA=true` 模式，降级为规则推荐 |
| Java 内部接口开发延迟 | Python 无法获取真实数据 | Python 继续用 mock 数据，接口就绪后切换 |
| Calicat 视觉资源缺失 | 视觉还原偏差 | 先用占位图，标记 `TODO: replace with Calicat asset` |
| 自定义套餐价格计算分歧 | 跳转后价格不一致 | 统一公式：`total_price = reference_price × hours`，由 Python 计算并透传 |
| 会话消息过多导致 LLM Token 超限 | 调用失败 | 限制上下文 20 条，超过提示新建会话 |

---

## 9. 附录：Calicat 资产清单

| 内容 | Calicat file_id | node-id | 本地截图 |
|------|-----------------|---------|----------|
| 主页面（初始状态） | 2083742072257646592 | d05e78e9-daee-4a12-9ecf-991b992f1d69 | `U-ai-assistant-initial.png` |
| 聊天状态 | 2083742072257646592 | 5364cc07-83b7-4b8f-add5-0adad815b75e | `U-ai-assistant-chat.png` |
| 加载状态 | 2083742072257646592 | 8e6057a1-bb3f-43a7-b899-62b34decd184 | `U-ai-assistant-loading.png` |
| 历史会话抽屉 | 2083742072257646592 | d5f695b1-2725-4309-a7a0-787def5ef5fe | `U-ai-assistant-history.png` |
| 套餐推荐对话 | 2083742072257646592 | 3db85d5b-5445-42ed-a7f7-f30f066997a4 | `U-ai-assistant-recommend.png` |

**Design Tokens**：`miniapp-user/src/assets/calicat/tokens/U-ai-assistant.json`  
**SCSS Overrides**：`miniapp-user/src/styles/calicat-overrides.scss`

---

## 10. 附录：AI 助理开发经验教训

> 本章节汇总 US-058 开发及后续调优过程中暴露的系统性问题与解决方案，供后续迭代与类似功能参考。

### 10.1 LLM 输出与推荐数据一致性

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 1 | AI 文字回复提到 3 个教练，但 `recommendations` 返回 4 个 | LLM 自由生成 text，未受推荐列表约束 | 服务端预生成最终推荐列表，作为 JSON 上下文传给 LLM，强制 text 只能引用列表中的项 |
| 2 | 用户问「7 节一对二自由泳」，text 说推荐自定义套餐，但 recommendations 里没有 | LLM 自行决定推荐自定义套餐，但服务端未生成对应项 | 当标准/体验套餐不匹配时，服务端自动基于教练参考价生成 `custom_package` 推荐项，再传给 LLM |
| 3 | 推荐教练时返回套餐 | 推荐类型判断基于完整会话上下文，历史中的「套餐」关键词导致误判 | 推荐类型只根据当前消息判断；过滤条件仍可从历史上下文累积 |
| 4 | text 中推荐项顺序与卡片展示顺序不一致 | 服务端与前端各自排序 | 服务端对推荐列表统一按总价升序，LLM 按此顺序介绍，前端原样展示 |

### 10.2 意图识别与参数过滤

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 5 | 推荐男教练时返回女教练 | 未提取性别参数 | 增加 `_extract_gender`，传给 `query_coaches` |
| 6 | 推荐 25 岁以下教练未生效 | 未提取年龄限制 | 增加 `_extract_age_limit`，Java 后端新增 `maxAge` 过滤 |
| 7 | 基于上文补充条件后返回结果不变 | 上下文理解失效 | `_build_intent_context` 拼接历史对话，基于完整上下文提取需求 |
| 8 | 用户说「蝶泳和仰泳」只匹配到其中一种 | 未处理多泳姿同时匹配 | 提取多个泳姿，要求教练/套餐同时满足 |
| 9 | 自由泳无法匹配 `freestyle` | 用户输入中文，数据库存英文 | 引入 `normalize_stroke` 统一泳姿格式 |
| 10 | 推荐结果未充分利用教练/套餐描述 | description 字段未透传给 AI | 后端 DTO 新增 `description`，AI 服务透传到 `RecommendationItem`，并在 `SYSTEM_PROMPT` 中明确要求 AI 参考描述 |

### 10.3 数据层与内部接口

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 11 | 后台管理接口能看到教练性别，但 AI 内部查询返回空 | AI 内部接口把 `female` 转成中文「女」后查询，数据库实际存英文 | 查询条件同时匹配中英文；响应统一输出 `male`/`female` |
| 12 | 会蝶泳和仰泳的女教练明明存在却查不到 | 部分教练 `gender` 字段为 NULL | 严格过滤无结果时回退忽略性别，并在推荐理由标注「性别信息未录入」；同时建议运营补全性别字段 |
| 13 | chat 接口偶发 409（conflict, 900002） | Redis 会话历史中存在孤立 `tool` 消息，OpenAI 校验失败 | `_history_to_messages` 跳过没有对应 `assistant_tool_calls` 的孤立 tool 消息 |

### 10.4 日志与可观测性

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 14 | AI 服务日志全部混在一起，难以按会话排查 | 使用 structlog 控制台输出，无文件分片 | 自定义 `SessionFileHandler`，按 `session_id` 分文件写入 `ai-service/logs/` |
| 15 | AI 服务启动后文件 handler 不生效 | uvicorn 启动时覆盖 logging 配置 | 在 FastAPI `lifespan` 的 startup 阶段重新调用 `configure_logging()` |
| 16 | 后端日志无日期归档 | 仅控制台输出 | `logback-spring.xml` 配置 `TimeBasedRollingPolicy`，按天写入 `backend/logs/leyo-backend-yyyy-MM-dd.log` |
| 17 | 无法快速定位 AI 推荐时传了什么参数 | 缺少工具调用参数日志 | 在 `chat_service.py` 中增加 `bootstrap_intent_extracted`、`bootstrap_invoking_tool`、`bootstrap_tool_finished`、`chat_ai_response` 等日志 |

### 10.5 前端展示

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 18 | AI 返回的 Markdown 无换行/加粗/列表样式 | 消息气泡直接渲染纯文本 | `AiMessageBubble.tsx` 实现基础 Markdown 渲染：加粗、无序/有序列表、换行 |
| 19 | 底部输入框按 Enter 发送后输入框未清空 | Enter 事件与发送逻辑绑定后未清空状态 | 调整 `AiInputBar` 事件处理，发送成功后清空输入值 |

### 10.6 工程管理

| # | 问题表现 | 根因 | 解决方案 |
|---|---------|------|---------|
| 20 | 项目根目录堆积大量临时脚本和 diff 文件 | 调试过程中未整理中间产出物 | 统一移入 `tmp/` 目录，避免污染根目录 |

### 10.7 后续开发检查清单

- [ ] 新增过滤条件时，同步更新意图提取函数、工具参数、后端查询接口
- [ ] 新增/修改推荐类型时，确保服务端生成的 recommendations 列表与 LLM text 一致
- [ ] 修改 LLM SYSTEM_PROMPT 后，用典型问法回归测试，确认 text 与 recommendations 一致
- [ ] 修改日志配置后，验证 uvicorn 启动不会覆盖 handler
- [ ] 涉及多值匹配字段（如泳姿）时，同时支持中文输入与数据库英文存储
- [ ] 涉及性别等枚举字段时，同时兼容中英文及 NULL 值
- [ ] AI 消息气泡需要支持 Markdown 基础样式

---

<p align="right">
<sub>本文档遵循项目开发流程规范，作为 US-058 实施入口。</sub>
</p>
