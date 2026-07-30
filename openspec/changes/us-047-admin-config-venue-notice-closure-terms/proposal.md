## Why

场馆信息、公告、闭馆/换水及《用户须知》是 leyoSwimming 运营合规的基础。MVP 要求管理员能集中维护这些内容，并在变更时自动触达用户、处理预约或要求重新签署。

## What Changes

- 新增 `venue`、`notice`、`venue_closure`、`terms_of_service`、`user_terms_sign` 表
- 新增场馆信息、公告、闭馆/换水、用户须知版本管理接口
- 闭馆设置后异步取消受影响预约并释放课时
- 用户须知更新后标记所有已签署用户为"待重新签署"并推送消息
- 前端 web-admin 新增「场馆运营」页面

## Capabilities

### New Capabilities

- `admin-config-venue-notice-closure-terms`: 管理员配置场馆、公告、闭馆换水与用户须知

## Impact

- **数据表**：新增 venue/notice/venue_closure/terms_of_service/user_terms_sign；修改 booking/package
- **API**：新增 5 个管理员接口
- **缓存**：新增 venue/notices/terms 缓存
- **前端**：新增 web-admin「场馆运营」页面
- **依赖**：依赖 US-014/US-029 预约数据；被 US-002/US-019 依赖
