# US-007 用户账号注销 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 注销条件检查接口 | 写失败测试 | 最小实现 | commit |
| 2 | 正常注销流程 | 写失败测试 | 最小实现 | commit |
| 3 | active 套餐拦截 | 写失败测试 | 最小实现 | commit |
| 4 | 未完成订单拦截 | 写失败测试 | 最小实现 | commit |
| 5 | 进行中预约拦截 | 写失败测试 | 最小实现 | commit |
| 6 | 二次确认弹窗取消 | 写失败测试 | 最小实现 | commit |
| 7 | 注销后登录态失效 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_user_cancel.py

def test_cancel_account_success():
    """正常注销账号"""
    pass

def test_cancel_account_active_package_blocked():
    """存在 active 套餐拒绝注销"""
    pass

def test_cancel_account_pending_order_blocked():
    """存在未完成订单拒绝注销"""
    pass

def test_cancel_account_idempotent():
    """重复注销幂等"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_user_cancel_api.py

def test_api_cancel_check_200():
    """POST /api/user/account/cancel-check 返回条件状态"""
    pass

def test_api_cancel_200():
    """POST /api/user/account/cancel 注销成功"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_cancel_flow.py

def test_e2e_cancel_account():
    """用户注销后无法再次登录"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常注销账号 | test_cancel_account_success / test_api_cancel_200 |
| 存在 active 套餐时注销 | test_cancel_account_active_package_blocked |
| 存在未完成订单时注销 | test_cancel_account_pending_order_blocked |
| 存在进行中预约时注销 | test_cancel_account_ongoing_booking_blocked |
| 二次确认弹窗中取消 | test_cancel_account_popup_cancelled |
