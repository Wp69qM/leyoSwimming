# Test Plan: US-058 用户-AI助理推荐教练与套餐

## Task 1: Java 内部数据接口 [P0]

**Files:**
- Create: `backend/src/controllers/ai-assistant-internal-controller.ts`, `backend/src/services/ai-assistant-internal-service.ts`, `backend/src/routes/ai-assistant-internal-routes.ts`
- Test: `backend/tests/controllers/ai-assistant-internal.test.ts`

**Spec coverage:** 教练召回、套餐召回、用户画像、已购套餐、热门推荐

- [ ] **RED:** `/api/internal/ai/coaches/query` 返回脱敏教练列表；`/api/internal/ai/packages/query` 返回套餐列表；`/api/internal/ai/user/profile` 返回用户画像；`/api/internal/ai/user/packages` 返回已购套餐；`/api/internal/ai/recommendations/hot` 返回热门推荐；未授权 IP/Token 返回 403
- [ ] **GREEN:** 实现内部接口，统一脱敏，IP + Token 鉴权
- [ ] **REFACTOR:** 抽取内部接口通用鉴权中间件和脱敏 DTO
- [ ] **COMMIT:** `feat(ai): add internal data APIs for AI assistant`

## Task 2: Java AI Assistant 对外接口 [P0]

**Files:**
- Create: `backend/src/controllers/ai-assistant-controller.ts`, `backend/src/services/ai-assistant-service.ts`, `backend/src/routes/ai-assistant-routes.ts`
- Test: `backend/tests/controllers/ai-assistant.test.ts`

**Spec coverage:** chat、session list、session create、session detail

- [ ] **RED:** `/api/ai-assistant/chat` 转发用户输入到 Python AI Service 并返回结构化回复；`/api/ai-assistant/session/create` 返回新 sessionId；`/api/ai-assistant/session/list` 返回历史会话（需登录）；`/api/ai-assistant/session/detail` 返回消息记录；游客可访问 chat/create；未登录访问 list/detail 返回 401
- [ ] **GREEN:** 实现接口，封装 Python AI Service 调用，幂等键处理
- [ ] **REFACTOR:** 抽取 Python AI Service HTTP Client 和错误处理
- [ ] **COMMIT:** `feat(api): add AI assistant chat and session APIs`

## Task 3: Python AI Service 基础服务 [P0]

**Files:**
- Create: `ai-service/src/main.py`, `ai-service/src/config.py`, `ai-service/src/clients/java_client.py`
- Test: `ai-service/tests/test_java_client.py`

**Spec coverage:** Java 内部接口调用、错误处理

- [ ] **RED:** Python client 调用 Java 内部接口成功；超时返回友好错误；无效 Token 返回 403；JSON 解析失败返回错误
- [ ] **GREEN:** 实现 httpx client，统一异常封装
- [ ] **REFACTOR:** 抽取 base client 和 retry 逻辑
- [ ] **COMMIT:** `feat(ai-service): add Java internal API client`

## Task 4: Python LangChain Tools [P0]

**Files:**
- Create: `ai-service/src/tools/coach_tools.py`, `ai-service/src/tools/package_tools.py`, `ai-service/src/tools/user_tools.py`
- Test: `ai-service/tests/test_tools.py`

**Spec coverage:** query_coaches、query_packages、get_user_profile、get_user_packages、get_hot_recommendations

- [ ] **RED:** 每个 tool 调用对应 Java 接口并返回结构化数据；tool schema 可被 LangChain 正确识别；游客场景不调用 user 相关 tool
- [ ] **GREEN:** 实现 5 个 @tool 函数
- [ ] **REFACTOR:** 抽取 tool 参数验证和日志记录
- [ ] **COMMIT:** `feat(ai-service): add LangChain tools for recommendation`

## Task 5: Python LangChain Agent [P0]

**Files:**
- Create: `ai-service/src/agent/recommendation_agent.py`, `ai-service/src/prompts/recommendation_prompt.py`
- Test: `ai-service/tests/test_agent.py`

**Spec coverage:** 意图识别、Tool 选择、推荐理由生成、敏感输入过滤

- [ ] **RED:** 输入「推荐自由泳教练」调用 query_coaches；输入「预算3000套餐」调用 query_packages；已登录用户输入触发 get_user_profile + query_packages；敏感输入不调用 tool 并返回拒绝回复
- [ ] **GREEN:** 实现 create_tool_calling_agent + Prompt + AgentExecutor
- [ ] **REFACTOR:** 抽取 prompt 模板和输出解析器
- [ ] **COMMIT:** `feat(ai-service): add recommendation agent`

## Task 6: 会话与 Memory [P1]

**Files:**
- Create: `ai-service/src/memory/redis_memory.py`, `backend/src/services/ai-chat-session-service.ts`
- Test: `ai-service/tests/test_memory.py`, `backend/tests/services/ai-chat-session.test.ts`

**Spec coverage:** 多轮对话上下文、会话持久化

- [ ] **RED:** Redis memory 能存储和读取最近 N 条消息；会话服务能创建/查询/分页会话；消息记录包含 recommendations 字段
- [ ] **GREEN:** 实现 Redis memory 和会话服务
- [ ] **REFACTOR:** 抽取 memory 接口，支持后续换实现
- [ ] **COMMIT:** `feat(ai): add session and memory support`

## Task 7: 前端 AI 助理页 [P0]

**Files:**
- Create: `miniapp-user/src/pages/ai-assistant/index.tsx`, `miniapp-user/src/pages/ai-assistant/components/ChatMessage.tsx`, `miniapp-user/src/pages/ai-assistant/components/RecommendationCard.tsx`
- Test: `miniapp-user/src/pages/ai-assistant/index.test.tsx`

**Spec coverage:** 欢迎语、快捷标签、消息渲染、推荐卡片、历史抽屉、输入发送

- [ ] **RED:** 页面加载展示 leyo 欢迎语和快捷标签；点击快捷标签发送消息；用户消息靠右，leyo 消息靠左；推荐卡片展示教练/套餐信息；点击「历史」打开抽屉；点击「新会话」清空聊天
- [ ] **GREEN:** 实现页面组件和交互
- [ ] **REFACTOR:** 抽取聊天 hooks 和消息类型定义
- [ ] **COMMIT:** `feat(miniapp): add AI assistant page`

## Task 8: E2E 关键流程 [P1]

**Files:**
- Create: `e2e/tests/ai-assistant.spec.ts`

**Spec coverage:** 游客推荐教练、已登录用户推荐套餐、新建会话、查看历史

- [ ] 游客进入 AI 助理页，点击「自由泳教练」，看到推荐教练卡片
- [ ] 已登录用户输入「预算3000推荐自由泳套餐」，看到推荐套餐卡片
- [ ] 已登录用户购买过某套餐后，再次询问不重复推荐该套餐
- [ ] 用户点击「新会话」，聊天清空
- [ ] 用户点击「历史」，选择历史会话，加载历史消息

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
- 单元测试覆盖率目标 ≥ 80%
