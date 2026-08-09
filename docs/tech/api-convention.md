# API 接口规范

> **适用范围**：leyoSwimming 项目所有后端 HTTP API（管理后台、教练端小程序、用户端小程序、内部服务间调用）。
> **设计范式**：RPC over HTTP —— 以业务动作（方法）为核心，通过 URL 路径表达操作意图，请求参数主要放 JSON body。

---

## 1. 核心原则

1. **动作语义优先**：URL 应表达"做什么"，而非"操作哪个资源"。
2. **统一使用 POST**：列表查询、详情查询、创建、更新、删除、业务动作等统一使用 `POST` 方法。
3. **避免 URL 路径参数**：业务 ID、筛选条件、分页参数等尽量通过 JSON body 传递，不在 URL 路径中体现。
4. **契约优先**：先定义接口契约（请求/响应字段、错误码），再实现业务逻辑。
5. **单一职责**：一个接口只完成一个明确的业务动作，避免"万能接口"。

---

## 2. URL 命名规范

### 2.1 基本结构

```
POST /api/{模块}/{资源单数}/{动作}
```

| 组成部分 | 说明 | 示例 |
|---------|------|------|
| `/api` | 固定前缀 | — |
| `{模块}` | 业务领域，小写 | `admin`、`coach`、`user`、`system` |
| `{资源单数}` | 领域对象，小写，单数 | `coach`、`application`、`order`、`package` |
| `{动作}` | 业务动作，小写驼峰或短横线 | `list`、`detail`、`add`、`update`、`delete`、`approve`、`reject` |

### 2.2 常用动作命名

| 业务意图 | 推荐动作名 | 示例 |
|---------|-----------|------|
| 分页列表查询 | `list` | `POST /api/admin/coach/application/list` |
| 单条详情查询 | `detail` | `POST /api/admin/coach/application/detail` |
| 新增创建 | `add` / `create` | `POST /api/admin/coach/application/add` |
| 全量更新 | `update` | `POST /api/admin/coach/application/update` |
| 删除 | `delete` / `remove` | `POST /api/admin/coach/application/delete` |
| 业务操作（通过/驳回/取消等） | 动词本身 | `POST /api/admin/coach/application/approve` |
| 上传文件 | `upload` | `POST /api/common/file/upload` |

### 2.3 禁止事项

- 禁止在 URL 中混用 GET/POST 表达同一资源，如 `GET /api/users` + `POST /api/users`。
- 禁止在 URL 路径中放置 ID，如 `/api/admin/coach/applications/{id}`。
- 禁止 URL 中出现大写字母、下划线或混合风格。

---

## 3. HTTP 方法

所有接口统一使用 `POST`。

| 方法 | 使用场景 | 说明 |
|------|---------|------|
| `POST` | 所有接口 | 包括查询、创建、更新、删除、业务动作 |

> 原因：统一方法可降低前端封装成本，避免 URL 长度限制，复杂查询条件可自由放在 body 中；同时与 RPC "方法调用" 语义一致。

---

## 4. 请求规范

### 4.1 Content-Type

```
Content-Type: application/json
```

### 4.2 请求体格式

所有业务参数通过 JSON body 传递：

```json
{
  "applicationId": 10001,
  "reason": "证书不清晰",
  "page": 1,
  "pageSize": 20
}
```

### 4.3 公共请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无强制公共字段 | — | — | 认证信息通过 Header（如 `Authorization: Bearer {token}`）传递，不放 body |

### 4.4 分页查询请求

```json
{
  "page": 1,
  "pageSize": 20,
  "keyword": "",
  "status": "pending"
}
```

### 4.5 ID 传递

```json
{
  "applicationId": 10001,
  "coachId": 20001
}
```

> 注意：字段命名使用 lowerCamelCase，ID 字段优先使用 `{资源}Id` 形式，如 `applicationId`、`coachId`、`userId`。

---

## 5. 响应规范

### 5.1 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | integer | `0` 表示成功；非 `0` 表示业务错误，具体值见错误码规范 |
| `message` | string | 成功时为 `"success"` 或空；失败时为可读错误描述 |
| `data` | object / array / null | 业务数据，失败时可为 `null` |

### 5.2 列表查询响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      { }
    ]
  }
}
```

### 5.3 HTTP 状态码

| 状态码 | 使用场景 |
|--------|---------|
| `200` | 请求正常处理完成（包括业务失败，业务失败通过 `code` 表达） |
| `400` | 请求体格式非法、JSON 解析失败 |
| `401` | 未登录或 Token 失效 |
| `403` | 无权限访问 |
| `500` | 服务端内部错误 |

> 业务成功/失败统一返回 HTTP `200`，通过 `code` 区分具体业务状态。便于网关、Nginx、前端统一拦截处理。

---

## 6. 错误码规范

`code` 为整数：

- `0` 表示成功；
- 非 `0` 表示业务错误，统一采用 6 位整数格式 `{MODULE_CODE}{SEQ}`：
  - 前 3 位 `MODULE_CODE` 为模块码；
  - 后 3 位 `SEQ` 为模块内递增序号，从 `001` 开始。

| 模块 | 模块码 | 示例 |
|------|--------|------|
| 通用 | `100` | `100001` 参数错误 |
| 认证授权 | `200` | `200001` Token 失效 |
| 权限禁止 | `300` | `300001` 无操作权限 |
| 教练 | `400` | `400001` 教练不存在 |
| 教练入驻申请 | `500` | `500001` 申请不存在 |
| 管理员 | `600` | `600001` 无管理员权限 |
| 订单 | `700` | `700001` 订单不存在 |
| 系统 | `900` | `900001` 系统内部错误 |

### 6.1 通用错误码

| 错误码 | 说明 |
|--------|------|
| `100001` | 请求参数错误 |
| `100002` | 请求体 JSON 解析失败 |
| `100003` | 必填参数缺失 |
| `200001` | 未登录或 Token 无效 |
| `200002` | 登录已过期 |
| `300001` | 无操作权限 |
| `900001` | 系统内部错误 |

---

## 7. 幂等性

写操作接口必须保证幂等性，尤其是以下场景：

- 创建类接口：使用客户端幂等键 `idempotentKey`
- 资金类接口：下单、支付、退款
- 状态变更类接口：审核通过/驳回、取消订单

推荐在请求体中显式传入幂等键：

```json
{
  "applicationId": 10001,
  "idempotentKey": "uuid-v4-or-business-key"
}
```

---

## 8. 示例

### 8.1 列表查询

**请求**

```http
POST /api/admin/coach/application/list HTTP/1.1
Content-Type: application/json
Authorization: Bearer {token}

{
  "page": 1,
  "pageSize": 20,
  "keyword": "张教练",
  "status": "pending"
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "coachId": 20001,
        "name": "张教练",
        "gender": "男",
        "age": 30,
        "teachingYears": 5,
        "teachingStrokes": ["自由泳", "蛙泳"],
        "latestSubmittedAt": "2026-08-07T10:30:00+08:00",
        "status": "pending",
        "previousCoachStatus": -1,
        "latestApplicationId": 10001
      }
    ]
  }
}
```

### 8.2 详情查询

**请求**

```http
POST /api/admin/coach/application/detail HTTP/1.1
Content-Type: application/json
Authorization: Bearer {token}

{
  "applicationId": 10001
}
```

### 8.3 业务动作

**请求**

```http
POST /api/admin/coach/application/reject HTTP/1.1
Content-Type: application/json
Authorization: Bearer {token}

{
  "applicationId": 10001,
  "reason": "证书不清晰"
}
```

---

## 9. 版本管理

- URL 中不显式携带版本号，通过后端整体版本迭代控制。
- 如需破坏性变更，优先新增接口，旧接口保留兼容，待调用方全部迁移后下线。

---

## 10. 相关文档

- [前端-后端-开发 setup guide](./frontend-backend-setup-guide.md)
- [US 拆分规范](../spec/user-story/SPECIFICATION.md)
- [OpenSpec 变更规范](../spec/openspec/README.md)

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-07 | 开发团队 | 初版：定义 RPC over HTTP 风格 API 规范 |
