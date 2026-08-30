# US-008 用户账号安全设置 — 测试计划

> **状态**：已填写　|　**最后更新**：2026-07-31

---

## TDD 任务清单

| # | Task | 对应 GWT | RED | GREEN | COMMIT |
|---|------|----------|-----|-------|--------|
| 1 | 换绑手机号接口 | §6.1 | 写失败测试 | 最小实现 | commit |
| 2 | 修改密码接口（含首次设置） | §6.2 / §6.6 | 写失败测试 | 最小实现 | commit |
| 3 | 设备列表查询 | §6.5 | 写失败测试 | 最小实现 | commit |
| 4 | 下线设备功能 | §6.5 | 写失败测试 | 最小实现 | commit |
| 5 | 换绑频率限制 | §8.1 | 写失败测试 | 最小实现 | commit |
| 6 | 小程序账号安全页 | §6.1 / §6.2 / §6.5 | 写失败测试 | 最小实现 | commit |

---

## Task 1: 换绑手机号接口 [P0]

**Files:**
- Create: `backend/src/controllers/user/security.ts`
- Create: `backend/src/routes/user/security.ts`
- Create: `backend/src/services/user/phone_change.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**RED:**
- 新手机号未绑定 + 原手机号验证码正确 → 返回 200，phone 更新
- 新手机号已被绑定 → 返回 `PHONE_ALREADY_BOUND`
- 验证码错误 → 返回 `CODE_INVALID`
- 24 小时内第二次换绑 → 返回 `PHONE_CHANGE_LIMIT`

**GREEN:**
- 实现 `PUT /api/user/security/phone`，二次验证（old_phone_code 或 current_password）
- 校验新手机号唯一性，更新 user.phone 与 phone_changed_at
- Redis 记录 `phone_change:limit:{user_id}`，TTL 24h

**COMMIT:** `feat(user): add phone change endpoint with verification and rate limit`

---

## Task 2: 修改密码接口（含首次设置） [P0]

**Files:**
- Modify: `backend/src/controllers/user/security.ts`
- Create: `backend/src/services/user/password_change.ts`
- Test: `backend/tests/services/user/password_change.test.ts`

**RED:**
- 原密码正确 + 新密码合规 → 返回 200，password_hash 更新
- 原密码错误 → 返回 `INVALID_OLD_PASSWORD`
- 新密码与旧密码相同 → 返回 `NEW_PASSWORD_SAME_AS_OLD`
- 微信登录用户 password_hash 为空时，省略 old_password 可直接设置

**GREEN:**
- 实现 `PUT /api/user/security/password`：bcrypt 校验、弱密码校验、hash 更新
- 修改成功后清除该用户除当前 session 外的所有会话
- 记录 audit_log

**COMMIT:** `feat(user): add password change endpoint with session revocation`

---

## Task 3: 设备列表查询 [P0]

**Files:**
- Create: `backend/src/repositories/user_session.ts`
- Modify: `backend/src/controllers/user/security.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**RED:**
- `GET /api/user/security/devices` 返回最近 30 天内登录的设备列表
- 当前访问设备标记 `is_current=true`

**GREEN:**
- 实现设备列表查询，按 last_active_at 降序，过滤 30 天内记录
- 根据请求 token 匹配当前 session

**COMMIT:** `feat(user): add login device list endpoint`

---

## Task 4: 下线设备功能 [P0]

**Files:**
- Modify: `backend/src/controllers/user/security.ts`
- Modify: `backend/src/repositories/user_session.ts`
- Test: `backend/tests/controllers/user/security.test.ts`

**RED:**
- 下线其他设备 → 返回 204，该设备 token 访问返回 401
- 下线不存在的设备 → 返回 `DEVICE_NOT_FOUND`

**GREEN:**
- 实现 `DELETE /api/user/security/devices/{id}`
- 删除 DB session 记录并清除 Redis `session:{token}`

**COMMIT:** `feat(user): add device revoke endpoint`

---

## Task 5: 换绑频率限制 [P1]

**Files:**
- Modify: `backend/src/services/user/phone_change.ts`
- Test: `backend/tests/services/user/phone_change.test.ts`

**RED:**
- 24 小时内第二次换绑 → 返回 `PHONE_CHANGE_LIMIT`
- 超过 24 小时后再次换绑 → 允许

**GREEN:**
- 基于 Redis `phone_change:limit:{user_id}` 实现 24h 限流

**COMMIT:** `feat(user): add 24h phone change rate limit`

---

## Task 6: 小程序账号安全页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/security/index.tsx`
- Create: `miniapp-user/src/pages/security/devices.tsx`
- Test: `miniapp-user/src/pages/security/index.test.tsx`

**RED:**
- 换绑手机成功后页面显示新手机号
- 修改密码成功后展示成功提示
- 下线设备后列表刷新
- 微信登录用户首次进入密码设置页不展示原密码输入框

**GREEN:**
- 实现账号安全设置页与设备管理页
- 根据 `password_hash` 是否为空动态展示首次设置/修改密码表单

**COMMIT:** `feat(miniapp): add account security settings pages`

---

## 测试用例

### 单元测试

```python
# backend/tests/test_user_security.py

def test_change_phone_success():
    """正常换绑手机号"""
    pass

def test_change_phone_already_bound():
    """新手机号已被绑定"""
    pass

def test_change_phone_rate_limit():
    """24 小时内频繁换绑被限流"""
    pass

def test_change_password_success():
    """正常修改密码，其他设备会话失效"""
    pass

def test_change_password_wrong_old():
    """原密码错误"""
    pass

def test_set_password_first_time():
    """微信登录用户首次设置密码"""
    pass

def test_revoke_device_session():
    """下线设备使 token 失效"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_user_security_api.py

def test_api_change_phone_200():
    """PUT /api/user/security/phone 正常返回"""
    pass

def test_api_change_password_200():
    """PUT /api/user/security/password 正常返回"""
    pass

def test_api_devices_list_200():
    """GET /api/user/security/devices 返回设备列表"""
    pass

def test_api_revoke_device_204():
    """DELETE /api/user/security/devices/{id} 正常返回"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_security_settings.py

def test_e2e_change_password():
    """用户修改密码后使用新密码登录"""
    pass

def test_e2e_revoke_device():
    """用户下线其他设备"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常换绑手机号 | test_change_phone_success / test_api_change_phone_200 |
| 正常修改密码 | test_change_password_success |
| 新手机号已被绑定 | test_change_phone_already_bound |
| 原密码错误 | test_change_password_wrong_old |
| 下线登录设备 | test_revoke_device_session / test_api_revoke_device_204 |
| 微信登录用户首次设置密码 | test_set_password_first_time |
| 24 小时内频繁换绑 | test_change_phone_rate_limit |

---

## 执行纪律

- 严格顺序：Task 1 → 2 → 3 → 4 → 5 → 6 → 7
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2, 4-5），P1 选做（Task 3, 6-7）
