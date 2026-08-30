## Why

教练是平台获客的重要节点。通过让教练分享个人主页与可约时段，潜在学员可在微信生态内直接查看教练资料并预约体验课或购买套餐，显著降低转化门槛。

## What Changes

- 教练端个人主页与排班页新增「分享主页」/「分享可约时段」入口
- 后端新增分享数据生成接口，聚合教练公开资料与未来可约时段
- 新增游客可访问的公开落地页接口，隐藏手机号与微信二维码
- 新增小程序落地页，支持一键登录/预约体验课/购买套餐
- 对非已通过教练隐藏分享入口，对非法/过期 scene 给出错误页

## Capabilities

### New Capabilities

- `coach-share-profile-slots`: 教练分享个人主页与可约时段，游客通过分享链接查看并转化

### Modified Capabilities

- 无

## Impact

- 后端：新增 `/api/coach/v1/share/profile-slots` 与 `/api/public/v1/coaches/{id}/share` 等接口
- 教练端小程序：分享入口与海报生成
- 用户端小程序：新增 `/pages/coach-share/index` 落地页
- 依赖：US-012（教练主页）、US-014（可约时段）
