## Context

本 US 实现管理后台的教练账号管理功能，包括教练列表查询、详情查看、资料编辑、新建教练、取消入驻。所有写操作需记录审计日志。接口遵循 [api-convention.md](../../../docs/tech/api-convention.md) 的 RPC over HTTP 规范。

## Goals / Non-Goals

**Goals:**
- 管理员可按状态、实时状态、关键词筛选教练
- 管理员可查看教练完整生效资料及历史记录
- 管理员可编辑教练全部资料（含实名与资质），无需重新审核
- 管理员可直接新建教练（status=1）
- 管理员可取消教练入驻（status=3）
- RBAC 权限细化到操作级

**Non-Goals:**
- 不实现教练自助入驻（由 US-010 实现）
- 不实现教练入驻审核（由 US-011 实现）
- 不实现教练离职后的完整处理流程（由 US-041 实现，本 US 仅触发状态变更）

## Decisions

1. **列表按 `coach.approved_at` 降序**
   - 理由：管理员关注最近入库/审批通过的教练

2. **管理员编辑实名与资质无需重新审核**
   - 理由：管理员是信任方，直接修改生效资料即可

3. **新建教练直接 status=1**
   - 理由：管理员操作等同于审批通过，跳过审核队列

4. **取消入驻即 status=3（已离职）**
   - 理由：与 US-039 / US-041 的教练离职状态机保持一致

## RBAC Permission Matrix

| 权限码 | 说明 |
|--------|------|
| `COACH:READ` | 查看教练列表/详情 |
| `COACH:WRITE` | 新建/编辑教练资料 |
| `COACH:CANCEL_ENTRY` | 取消教练入驻 |

## Risks / Trade-offs

- **[Risk]** 管理员误操作修改教练敏感资料 → **Mitigation**: 编辑保存前二次确认，所有修改写入 audit_log
- **[Risk]** 取消入驻后学员课程未处理 → **Mitigation**: 触发 US-041 离职处理流程，由专门 US 负责

## Migration Plan

1. 确认 `coach` 表字段与 US-010 保持一致
2. 确认 `coach_certificate` 表已支持 PORTRAIT / ID_CARD_FRONT / ID_CARD_BACK / QUALIFICATION / HEALTH
3. 部署后端接口与管理端页面
4. 回滚：删除新增接口与前端路由

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-010 | 本 US 依赖 | 教练入驻字段与校验规则 |
| US-011 | 本 US 依赖 | 审核详情字段展示方式 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |
| US-041 | 依赖本 US | 取消入驻后触发离职处理 |

## API Design

> 本变更所有接口遵循 [API 接口规范](../../../docs/tech/api-convention.md)：统一使用 `POST`，URL 按动作命名，参数通过 JSON body 传递。

### POST /api/admin/coach/list

- 鉴权：是（管理员 + `COACH:READ`）
- Request body: `{ page, pageSize, keyword, status, realtimeStatus }`
- Response 200: `{ total, list: [{ coachId, name, gender, age, teachingYears, teachingStrokes, approvedAt, status, realtimeStatus }] }`
- Response 403: `ADMIN_PERMISSION_DENIED`

### POST /api/admin/coach/detail

- 鉴权：是（管理员 + `COACH:READ`）
- Request body: `{ coachId }`
- Response 200: `{ coachId, profile: { ... }, certificates: [...], applicationHistory: [...], auditLog: [...] }`
- Response 403: `ADMIN_PERMISSION_DENIED`
- Response 404: `COACH_NOT_FOUND`

### POST /api/admin/coach/add

- 鉴权：是（管理员 + `COACH:WRITE`）
- Request body: 完整教练入驻字段
- Response 200: `{ coachId, status: 1, approvedAt }`
- Response 400: 字段校验错误
- Response 403: `ADMIN_PERMISSION_DENIED`
- Response 409: `PHONE_ALREADY_EXISTS` / `ID_CARD_ALREADY_EXISTS`

### POST /api/admin/coach/update

- 鉴权：是（管理员 + `COACH:WRITE`）
- Request body: `{ coachId, profile: { ... }, certificates: [...] }`
- Response 200: `{ coachId, updatedAt }`
- Response 400: 字段校验错误
- Response 403: `ADMIN_PERMISSION_DENIED`
- Response 404: `COACH_NOT_FOUND`
- Response 409: `PHONE_ALREADY_EXISTS` / `ID_CARD_ALREADY_EXISTS`

### POST /api/admin/coach/cancelEntry

- 鉴权：是（管理员 + `COACH:CANCEL_ENTRY`）
- Request body: `{ coachId, reason }`
- Response 200: `{ coachId, status: 3 }`
- Response 400: `COACH_STATUS_NOT_APPROVED`
- Response 403: `ADMIN_PERMISSION_DENIED`
- Response 404: `COACH_NOT_FOUND`
