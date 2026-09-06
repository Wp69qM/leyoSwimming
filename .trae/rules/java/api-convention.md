# API 接口硬性规则（RPC over HTTP）

> **规范本体**：[docs/tech/api-convention.md](../../../docs/tech/api-convention.md)。本规则是其**硬性约束的执行镜像**，两者必须同步修改：任何一侧变更 API 约束时，另一侧同步更新。
> **适用范围**：leyoSwimming 所有后端 HTTP API（backend Java、ai-service internal API、管理后台/教练端/用户端/服务间调用）。
> **执行时机**：新增或修改任何 Controller / Router 端点时，**写入前**逐项核对 §2 硬性约束；code-review 阶段按 §4 清单复审。

---

## 1. 设计范式

RPC over HTTP：URL 表达"做什么"（模块/资源/动作），参数放 JSON body。**不做 RESTful 资源建模**。

```
POST /api/{模块}/{资源单数}/{动作}
```

---

## 2. 硬性约束（写入前逐项核对，任一违反即违规）

### 2.1 URL 与方法

| # | 约束 |
|---|------|
| 1 | **所有接口统一 `POST`**，包括查询、创建、更新、删除、业务动作。禁止 GET/PUT/DELETE/PATCH |
| 2 | URL 结构固定三段：`/api/{模块}/{资源单数}/{动作}`；模块与资源小写，资源用单数（`coach`、`application`、`order`） |
| 3 | **禁止 URL 路径参数**：`/api/admin/coach/application/{id}` ❌ → ID 放 body：`{"applicationId": 10001}` ✅ |
| 4 | URL 禁止大写字母、下划线 |
| 5 | 动作命名：查询 `list` / `detail`，写入 `add` / `update` / `delete`，业务动作用动词本身（`approve` / `reject` / `cancel` / `upload` / `toggle` / `ingest` 等） |
| 6 | 同一资源路径不得混用多种 HTTP 方法表达不同操作；每个动作独立 URL |
| 7 | 一个接口只做一个业务动作，禁止"万能接口" |

### 2.2 请求

| # | 约束 |
|---|------|
| 8 | `Content-Type: application/json`（文件上传除外，用 `multipart/form-data`） |
| 9 | 业务参数全部放 JSON body；认证信息放 Header（`Authorization: Bearer {token}`），**不放 body** |
| 10 | 字段命名 lowerCamelCase；ID 字段用 `{资源}Id` 形式（`applicationId`、`coachId`、`userId`） |
| 11 | 分页请求字段：`page`（从 1 开始）、`pageSize` |

### 2.3 响应

| # | 约束 |
|---|------|
| 12 | 统一响应结构：`{"code": 0, "message": "success", "data": { }}` |
| 13 | **列表响应必须用 `data.list` 承载数组**，配 `total` / `page` / `pageSize`。**禁止 `items` / `records` / `rows`**（历史 P0 教训：`data.items` 与 `data.list` 混用导致前端报错） |
| 14 | 业务成功/失败统一返回 HTTP `200`，业务状态用 `code` 区分；`400/401/403/500` 仅用于协议层/认证层错误 |
| 15 | `code`：`0` 成功；非 `0` 为 6 位业务错误码 `{MODULE_CODE}{SEQ}`（模块码见规范本体 §6，如 `100001` 参数错误、`200001` Token 失效） |

### 2.4 幂等性

| # | 约束 |
|---|------|
| 16 | 写操作接口（创建/资金/状态变更）必须考虑幂等：请求体显式传 `idempotentKey` 或服务端以业务键防重 |

---

## 3. 违规对照表

| ❌ 违规 | ✅ 正确 |
|--------|--------|
| `GET /api/admin/coach/list` | `POST /api/admin/coach/list` |
| `POST /api/admin/coach/application/{id}` | `POST /api/admin/coach/application/detail` + body `{"applicationId": 10001}` |
| `PUT /api/admin/coach/application/update` | `POST /api/admin/coach/application/update` |
| `DELETE /api/admin/coach/application` | `POST /api/admin/coach/application/delete` |
| URL 含大写/下划线：`/api/admin/coachApplication/list` | `/api/admin/coach/application/list`（拆成三段小写） |
| body 字段 `application_id` / `coachID` | `applicationId` / `coachId` |
| 列表响应 `"items": []` / `"records": []` / `"rows": []` | `"list": []` + `total` / `page` / `pageSize` |
| 业务失败返回 HTTP 500 + 错误信息 | HTTP 200 + `{"code": 400001, "message": "教练不存在"}` |
| token 放 body | `Authorization: Bearer {token}` Header |

---

## 4. code-review 复审清单

对任何涉及 API 的代码变更，按 §2 的 16 项约束逐项核对。重点：

- [ ] 无 GET/PUT/DELETE 端点新增
- [ ] 无 URL 路径参数
- [ ] 列表响应字段为 `data.list`
- [ ] 错误码为 6 位格式且模块码正确
- [ ] 写操作有幂等设计

发现违规：**BLOCK**，修复后方可通过评审；新增接口的路径/错误码与 tech-design.md 或 dev-plan 不一致时，以规范本体为准修正代码或同步文档。

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-09-06 | AI | 初版：从 docs/tech/api-convention.md v1.1 提炼硬性约束镜像（16 项约束 + 违规对照表 + 复审清单） |
