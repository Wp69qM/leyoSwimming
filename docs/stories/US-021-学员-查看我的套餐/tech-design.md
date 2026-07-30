# Tech Design: US-021 学员查看我的套餐

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 读 | 用户套餐 |
| `coach` | 读 | 教练姓名、状态 |
| `user` | 读 | 登录态 |

#### package

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_type` | 0=体验 / 1=正式 |
| `total_hours` | 总课时 |
| `consumed_count` | 已消耗 |
| `available_count` | 剩余可用 |
| `reserved_count` | 已预约 |
| `expire_at` | 有效期 |
| `status` | active / exhausted / expired / refunded / frozen |
| `frozen_reason` | 冻结原因 |

### 1.2 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_package_user_type ON package(user_id, package_type);
```

## 2. API 设计

### 2.1 GET /api/users/me/packages

- **鉴权**：必须登录
- **Response 200**:
  ```json
  {
    "active": { "items": [...], "summary": { "total", "consumed", "available" } },
    "exhausted": { "items": [...] },
    "expired": { "items": [...] },
    "refunded": { "items": [...] },
    "frozen": { "items": [...] }
  }
  ```

## 3. 状态机

（只读）

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 小程序本地 | `my_packages` | 30s | 下拉刷新强制失效 |

## 5. 性能

| 指标 | 目标 |
|------|------|
| 列表 P99 | < 100ms |

## 6. 安全

- 登录鉴权
- 仅返回当前 user_id 的 package
- 不返回敏感 coach 联系方式

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 套餐购买 |
| US-022 | 被依赖 | 更换教练 |
| US-029 | 被依赖 | 预约 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常查看 | `test_my_packages_list_success` |
| 空状态 | `test_my_packages_empty` |
| 冻结套餐 | `test_my_packages_frozen_visible` |
