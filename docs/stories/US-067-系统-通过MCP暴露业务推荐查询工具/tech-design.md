# Tech Design: US-067 系统-通过MCP暴露业务推荐查询工具

## 1. 总体架构

```
外部 MCP 客户端
    │  Bearer <MCP_API_TOKEN>（复用 US-066 鉴权）
    ▼
ai-service FastAPI
    └── /mcp-server/mcp FastMCP
            ├── query_knowledge      （US-066）
            ├── query_coaches        （本 US）
            ├── query_packages       （本 US）
            └── get_hot_recommendations（本 US）
                    │
                    ▼
            JavaInternalClient ──→ backend /api/internal/ai/*（US-058 既有链路）
```

## 2. 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `ai-service/app/tools/normalizers.py` | 新增 | 从 `recommendation_tools.py` 提取 `normalize_stroke` / `normalize_gender` / `normalize_package_mode` 为模块级共享函数 |
| `ai-service/app/tools/recommendation_tools.py` | 修改 | 改为引用 `normalizers.py`（行为不变） |
| `ai-service/app/mcp/server.py` | 修改 | 注册 3 个推荐工具（`_get_java_client` 惰性单例 + `_backend_error` 统一故障隔离） |
| `ai-service/app/config.py` | 修改 | 新增 `mcp_limit_max`（默认 20） |
| `ai-service/tests/mcp/helpers.py` | 新增 | MCP 协议测试共享工具（JSON-RPC 构造/解析、session manager 管理） |
| `ai-service/tests/mcp/test_recommendation_tools.py` | 新增 | 工具单测（白名单/归一化/limit 截断/故障隔离） |

## 3. 关键实现

### 3.1 归一化提取（normalizers.py）

```python
# 从 recommendation_tools.py 原样迁移，LangChain 与 MCP 共用
def normalize_stroke(stroke: str | None) -> str | None: ...
def normalize_gender(gender: str | None) -> str | None: ...
def normalize_package_mode(mode: str | None) -> str | None: ...
```

### 3.2 MCP 推荐工具（server.py 追加）

```python
@mcp.tool()
async def query_coaches(stroke: str | None = None, gender: str | None = None,
                        max_price: int | None = None, max_age: int | None = None,
                        class_size: str | None = None, limit: int = 5) -> dict:
    """查询游泳培训平台的教练列表（只读）。"""
    settings = get_settings()
    effective_limit = min(limit, settings.mcp_limit_max)  # 上限 20，防大结果集
    try:
        client = _get_java_client(settings)  # 惰性单例，MCP 启动不依赖 backend
        coaches = await client.query_coaches(
            stroke=normalize_stroke(stroke), gender=normalize_gender(gender),
            max_price=max_price, max_age=max_age, class_size=class_size,
            limit=effective_limit)
        return {"list": coaches}
    except Exception as exc:
        return _backend_error("query_coaches", exc)  # {"error": "..."}
```

`query_packages` / `get_hot_recommendations` 同构。返回统一 `{"list": [...]}` 包装
（与 US-066 query_knowledge 一致，FastMCP 对裸 list 会展开为多 content 块）；
backend 故障统一返回结构化 `{"error": "业务数据服务暂不可用，请稍后再试"}` 而非抛异常，
保证 MCP 连接稳定与工具间故障隔离。

### 3.3 排除清单（安全约束）

`build_tools()` 中的 `get_user_profile` / `get_user_packages` 依赖 `user_hash` 闭包，**不注册到 MCP**。代码层面通过"仅显式注册白名单工具"实现（MCP 工具逐个 `@mcp.tool()` 声明，无批量导入路径），并在 `tools/list` 集成测试中断言不出现这两个工具名。

## 4. 设计决策

| 决策 | 选项 | 结论 |
|------|------|------|
| 工具复用方式 | 包装 LangChain @tool / 直调 JavaInternalClient | **直调 client**：@tool 闭包签名与 MCP schema 不兼容 |
| 归一化 | MCP 内重写 / 提取共享 | **提取 normalizers.py**：单一事实来源，防止两协议层行为漂移 |
| backend 故障 | 抛异常 / 结构化错误 | **结构化错误**：MCP 连接稳定，工具间隔离 |
| limit 上限 | 不限 / 截断 | **截断至 20**：防外部 Agent 拉全量数据 |

## 5. 风险与对策

- **业务数据经 MCP 外泄面**：仅暴露公开可浏览数据（教练列表/套餐与游客端可见口径一致），不含用户个体数据
- **backend 依赖**：本地验证需先启动 backend；工具层 try/catch 隔离，MCP 服务启动不依赖 backend
