# US-013 教练管理实时状态 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 手动状态更新 | 写失败测试 | 最小实现 | commit |
| 2 | 自动状态切换任务 | 写失败测试 | 最小实现 | commit |
| 3 | 手动覆盖优先级 | 写失败测试 | 最小实现 | commit |
| 4 | 请假状态锁定 | 写失败测试 | 最小实现 | commit |
| 5 | 状态变更广播 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_realtime_status.py

def test_coach_manual_status_update():
    """手动更新实时状态"""
    pass

def test_coach_auto_status_before_class():
    """课前 15 分钟自动切换上课中"""
    pass

def test_coach_manual_override_auto():
    """手动状态覆盖自动切换"""
    pass

def test_coach_leave_status_locked():
    """请假中状态不可手动修改"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_status_api.py

def test_api_coach_realtime_status_200():
    """PUT /api/coach/realtime-status 正常返回"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_status_flow.py

def test_e2e_coach_status_broadcast():
    """教练切换状态后学员端实时更新"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 教练手动设置为空闲中 | test_coach_manual_status_update |
| 系统自动切换为上课中 | test_coach_auto_status_before_class |
| 手动状态优先级高于自动状态 | test_coach_manual_override_auto |
