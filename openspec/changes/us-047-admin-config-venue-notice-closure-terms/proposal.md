> **OpenSpec Proposal | 映射自 `docs/stories/US-047-管理员-配置场馆公告闭馆换水与用户须知和隐私协议/user-story.md` §1-§5**

## Why

场馆信息、公告、闭馆/换水及《用户须知》《隐私协议》是 leyoSwimming 运营合规的基础。MVP 要求管理员能集中维护这些内容，并在变更时自动触达用户、处理预约或要求重新同意。

PRD [§5.5.4](../../../docs/prd/prd.md) 要求配置场馆名称、地址、导航、开业年限、泳池状态，以及公告、闭馆/换水和用户须知；[§11.2](../../../docs/prd/prd.md) 要求首次注册/登录需同意隐私协议与用户须知。

## What Changes

- 新增 `venue`、`notice`、`venue_closure` 表
- 新增 `terms_policy` 表（存储《用户须知》版本与内容，与 US-009 统一表名）
- 新增 `privacy_policy` 表（存储《隐私协议》版本与内容，与 US-009 统一表名）
- 新增 `user_terms_consent` 表（用户/教练《用户须知》同意记录，与 US-009 统一表名）
- 新增 `user_privacy_consent` 表（用户/教练《隐私协议》同意记录，与 US-009 统一表名）
- 新增场馆信息、公告、闭馆/换水接口
- 新增《用户须知》与《隐私协议》版本管理接口及签署记录查询接口
- 闭馆设置后异步取消受影响预约并释放课时
- 《用户须知》/《隐私协议》更新后标记所有已同意用户为"待重新同意"并推送消息
- 前端 web-admin 新增「场馆运营」页面，其中「协议管理」Tab 包含《用户须知》《隐私协议》两个子 Tab

## Capabilities

### New Capabilities

- `admin-config-venue-notice-closure-terms`: 管理员配置场馆、公告、闭馆换水、《用户须知》与《隐私协议》（含签署记录）

## Impact

- **数据表**：新增 `venue`/`notice`/`venue_closure`/`terms_policy`/`privacy_policy`/`user_terms_consent`/`user_privacy_consent`；修改 `booking`/`package`
- **API**：新增 7 个管理员接口（场馆、公告、闭馆、用户须知、隐私协议、两种签署记录查询）
- **缓存**：新增 `venue:info`、`notices:active`、`terms:active`、`privacy:active` 缓存
- **前端**：新增 web-admin「场馆运营」页面，含「协议管理」Tab 及子 Tab
- **依赖**：依赖 US-014/US-029 预约数据；被 US-002/US-009/US-019 依赖
