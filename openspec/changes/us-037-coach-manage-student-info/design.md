## Context

本变更在教练端引入「我的学员」能力。教练需要维护与自己存在 booking/package 关联的学员信息切片，并支持未成年人及监护人字段。核心约束是教练切片与学员本人资料解耦。

## Goals / Non-Goals

**Goals:**
- 提供关联学员列表与详情/编辑接口
- 支持未成年人标识与监护人手机号必填校验
- 保证数据隔离与敏感信息加密
- 记录审计日志

**Non-Goals:**
- 修改 `user` 表中学员自主资料
- 实现监护人短信通知（由其他 US 触发）
- 家长独立账号体系

## Decisions

1. **独立 `coach_student_profile` 表**
   - 理由：避免教练视角覆盖学员本人资料，支持不同教练对同一学员保存不同切片
   - 替代方案：扩展 `user` 表加 `coach_notes` 字段 ——  rejected，无法隔离多教练

2. **AES-256 加密存储 guardian_phone**
   - 理由：满足《个人信息保护法》对敏感个人信息的要求
   - 替代方案：哈希存储 —— rejected，管理员/教练后续需要联系监护人，需可逆加密

3. **关联关系校验基于 `package` + `booking`**
   - 理由：只要学员购买过该教练套餐或存在预约，即认为存在教学关联
   - 替代方案：单独维护 `coach_student` 关联表 —— 当前数据已足够，MVP 不新增关联表

4. **缓存列表与详情，更新时失效**
   - 理由：我的学员列表访问频率中等，缓存可降低 DB 压力
   - TTL 5 分钟，权衡一致性与性能

## Risks / Trade-offs

- **[Risk]** 教练误标记未成年人导致监护人信息缺失 → **Mitigation**: 前端开关与后端双重校验
- **[Risk]** 并发保存产生重复审计日志 → **Mitigation**: idempotency_key 去重
- **[Risk]** 加密字段无法按手机号搜索 → **Mitigation**: MVP 仅按 user_id 查询，搜索需求 P2

## Migration Plan

1. 执行 Knex migration 创建 `coach_student_profile` 表与唯一索引
2. 部署后端接口与教练端页面
3. 对历史 booking/package 关联自动生成空切片（可选，MVP 可懒加载）
4. 回滚：删除表并回退代码

## Open Questions

- 是否需要教练首次查看学员时自动初始化空切片？建议 MVP 采用懒加载，查询不存在时返回默认值并在保存时创建。
