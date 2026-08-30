## Context

本变更为教练提供社交分享能力，将公开资料与可约时段组合成可传播内容。落地页对游客开放，因此需要严格控制隐私字段，并防止 scene 被篡改。

## Goals / Non-Goals

**Goals:**
- 教练可生成分享卡片/海报数据
- 游客可访问公开落地页查看资料与时段
- 非已通过教练不能分享
- 非法/过期 scene 有错误处理

**Non-Goals:**
- 不生成真实朋友圈海报图片（使用小程序 canvas 或原生分享）
- 不实现购买/预约逻辑
- 不修改 coach 状态机

## Decisions

1. **公开接口独立于教练接口**
   - 理由：游客无需登录，需单独做字段白名单与限流
   - 路径：`/api/public/v1/coaches/...`

2. **scene 使用 base64url(query string) + 可选签名**
   - 理由：兼容微信小程序 scene 长度限制（≤ 32 字符），同时防止篡改
   - 替代方案：纯数字 scene 映射数据库 —— 需要额外存储，MVP 使用编码方案

3. **公开资料白名单过滤**
   - 理由：防止误将 phone、wechat_qr 等敏感字段返回给游客
   - 实现：DTO 显式选择字段

4. **短缓存 TTL 保证时段新鲜度**
   - 理由：可约时段变化频繁，缓存过长会导致落地页与实际不一致
   - TTL：profile 1 分钟，slots 30 秒

## Risks / Trade-offs

- **[Risk]** 公开接口被爬虫批量访问 → **Mitigation**: IP 限流 + CDN/WAF
- **[Risk]** scene 被篡改导致跳转到错误教练 → **Mitigation**: 签名校验
- **[Risk]** 海报图片生成耗时长 → **Mitigation**: 优先使用小程序原生分享，海报 P2 优化

## Migration Plan

1. 新增 `coach_share_log` 表（可选）
2. 部署后端分享与公开接口
3. 教练端与用户端小程序新增页面
4. 配置公开 API 限流规则

## Open Questions

- 是否需要统计分享带来的转化？建议 P2 通过埋点实现，MVP 仅记录 share_log。
