# leyo AI Service

基于 FastAPI + LangChain 的 AI 推荐服务，为 leyoSwimming 用户端小程序提供教练与套餐推荐能力。

## 技术栈

- Python 3.11
- FastAPI
- LangChain + LangChain-OpenAI
- Redis（会话缓存）
- httpx（调用 Java 内部接口）

## 目录结构

```
ai-service/
├── app/
│   ├── clients/           # Java 内部接口客户端
│   ├── models/            # Pydantic 模型
│   ├── services/          # 业务服务（ChatService）
│   ├── tools/             # LangChain Tool 注册
│   ├── utils/             # 日志等工具
│   ├── config.py          # 配置管理
│   └── main.py            # FastAPI 入口
├── tests/                 # 测试
├── Dockerfile
├── requirements.txt
└── .env.example
```

## 快速启动

### 1. 本地虚拟环境

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# 复制环境变量并填写 LLM API Key
cp .env.example .env

# 启动服务
python app/main.py
```

访问：`http://localhost:8000/health`
文档：`http://localhost:8000/docs`

### 2. Docker Compose

```bash
# 在项目根目录
cd deploy
# 配置 .env 后
docker-compose up -d ai-service
```

## 核心接口

| 接口 | 说明 |
|------|------|
| `GET /health` | 健康检查 |
| `POST /api/ai-assistant/chat` | 发送消息，获取推荐 |
| `POST /api/ai-assistant/session/create` | 创建新会话 |
| `POST /api/ai-assistant/session/list` | 历史会话列表 |
| `POST /api/ai-assistant/session/detail` | 会话详情 |

## 环境变量

参见 `.env.example`。

关键变量：

- `LLM_API_KEY`：LLM 服务 API Key（必填）
- `LLM_MODEL`：模型名称，默认 `qwen-plus`
- `LLM_BASE_URL`：OpenAI 兼容接口地址
- `REDIS_HOST` / `REDIS_PASSWORD`：Redis 配置
- `ENABLE_MOCK_DATA`：`true` 时使用本地 Mock 数据，不调用 Java 内部接口

## LangChain Agent 说明

当前使用 `ChatOpenAI.bind_tools()` + 手动 Tool-Calling 循环，避免对 `langchain.agents` 高版本 API 的依赖，保持与 Python 3.11/3.12/3.14 的兼容性。注册的 Tool 包括：

- `query_coaches`：召回教练
- `query_packages`：召回套餐
- `get_user_profile`：获取用户画像（登录态）
- `get_user_packages`：获取已购套餐（登录态）
- `get_hot_recommendations`：热门推荐 / 冷启动

## 注意事项

1. LLM API Key 不要提交到代码仓库。
2. 内部接口 `/api/internal/ai/*` 需要在 Java 后端实现并配置 Token 鉴权。
3. MVP 阶段开启 `ENABLE_MOCK_DATA=true` 即可独立调试推荐流程。
4. 本地开发使用 Python 3.11/3.12 最佳；当前代码在 Python 3.14 虚拟环境下已通过测试。
5. 运行测试：`python -m pytest tests/ -v`。
