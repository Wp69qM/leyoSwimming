# US-038 教练分享个人主页与可约时段技术设计

---

## 1. 上下文

本 US 为教练提供社交分享能力，将个人主页与可约时段打包成小程序卡片/海报。落地页面向游客，需隐藏敏感联系方式，并提供直接购买/预约入口。

---

## 2. 目标 / 非目标

**目标：**
- 教练可生成分享卡片/海报
- 游客可通过分享链接查看教练公开资料与可约时段
- 不可分享的教练状态需拦截
- 落地页引导登录/购买转化

**非目标：**
- 不实现完整朋友圈海报图片生成服务（可用小程序原生分享或 canvas）
- 不修改教练状态机
- 不实现购买/预约逻辑（由 US-004 / US-017 / US-020 实现）

---

## 3. 数据模型

### 3.1 新增表

#### `coach_share_log`（可选，用于统计）

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| share_id | BIGINT PK | | |
| coach_id | BIGINT FK | IDX | |
| share_type | TINYINT | | 0=主页 1=可约时段 |
| scene | VARCHAR(128) | UK | 分享场景参数 |
| created_at | DATETIME | IDX | |

### 3.2 读取表

- `coach`：读取公开资料（姓名、头像、证书、任教年限、评分、参考单价）
- `schedule_slot`：读取未来可约时段（status = 待预约）
- `user`：落地页游客身份无需读取

---

## 4. API 设计

### 4.1 `GET /api/coach/v1/share/profile-slots`

- **鉴权**：教练 JWT，`coach.status = 1`
- **功能**：返回生成分享卡片所需的数据
- **响应 200**：
  ```json
  {
    "scene": "coach=123&ts=1753879200",
    "coach": {
      "name": "李教练",
      "avatar_url": "...",
      "years_teaching": 5,
      "rate": 4.8,
      "price_per_hour": 180.00
    },
    "slots": [
      { "date": "2026-08-02", "start_time": "09:00", "end_time": "10:00" }
    ]
  }
  ```
- **错误码**：`COACH_NOT_APPROVED`（403）

### 4.2 `GET /api/public/v1/coaches/{coach_id}/share`

- **鉴权**：无（游客可访问）
- **功能**：根据 scene 解析后的 coach_id 返回公开资料
- **响应 200**：教练公开资料（不含 phone / wechat_qr）
- **错误码**：`COACH_NOT_FOUND`（404）、`SHARE_EXPIRED`（410）

### 4.3 `GET /api/public/v1/coaches/{coach_id}/available-slots`

- **鉴权**：无
- **功能**：返回未来 7 天可约时段
- **响应 200**：slots 数组
- **错误码**：`COACH_NOT_FOUND`（404）

---

## 5. 状态机

本 US 不涉及业务状态机转换。

---

## 6. 缓存策略

| Key | 类型 | TTL | 失效策略 |
|-----|------|-----|---------|
| `public:coach:{coach_id}:profile` | 教练公开资料 JSON | 1 分钟 | 教练资料更新时删除 |
| `public:coach:{coach_id}:slots` | 可约时段 JSON | 30 秒 | 排班/预约变化时删除 |

---

## 7. 性能指标

- `GET /api/coach/v1/share/profile-slots` P99 < 300ms
- `GET /api/public/v1/coaches/{id}/share` P99 < 200ms
- `GET /api/public/v1/coaches/{id}/available-slots` P99 < 200ms
- 落地页首屏 < 500ms

---

## 8. 安全

- 公开接口不返回 coach.phone、coach.wechat_qr
- scene 参数使用 base64url 编码，避免明文暴露内部 ID；可加入签名字段防止篡改
- 接口限流：公开 API 单 IP 100 次/分钟

---

## 9. 跨 US 依赖

- 依赖 US-012（教练主页管理）提供 coach 公开资料
- 依赖 US-014（教练管理可约时段）提供 schedule_slot 数据
- 触发 US-004（登录）、US-017（体验课购买）、US-020（正价套餐购买）的转化入口

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 生成分享卡片 | `test_generate_share_card_success` | 集成 |
| 游客查看落地页 | `test_visitor_open_share_landing_success` | 集成 |
| 非已通过教练禁止分享 | `test_not_approved_coach_cannot_share` | 集成 |
| 无可约时段兜底 | `test_share_no_slots_fallback` | 集成 |
| 非法/过期 scene | `test_invalid_share_scene_returns_error` | 集成 |
| 隐私字段不暴露 | `test_public_share_hides_private_fields` | 单元/集成 |
