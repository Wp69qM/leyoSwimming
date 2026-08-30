# US-014 教练管理可约时段 — 测试计划

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## TDD 任务清单

| # | Task | RED | GREEN | COMMIT |
|---|------|-----|-------|--------|
| 1 | 可约时段添加接口基础结构 | 写失败测试 | 最小实现 | commit |
| 2 | 时段冲突校验 | 写失败测试 | 最小实现 | commit |
| 3 | 过去时间校验 | 写失败测试 | 最小实现 | commit |
| 4 | 删除时段时校验已有 booking | 写失败测试 | 最小实现 | commit |
| 5 | 复制上周排班 | 写失败测试 | 最小实现 | commit |
| 6 | 学员端可见性同步 | 写失败测试 | 最小实现 | commit |

---

## 测试用例

### 单元测试

```python
# backend/tests/test_coach_schedule.py

def test_create_schedule_slot_success():
    """正常添加可约时段，status = available"""
    pass

def test_create_schedule_slot_conflict():
    """时段冲突返回 SLOT_TIME_CONFLICT"""
    pass

def test_create_schedule_slot_past_time():
    """过去时间返回 PAST_TIME_NOT_ALLOWED"""
    pass

def test_delete_slot_with_booking_fails():
    """已预约时段删除返回 SLOT_HAS_BOOKING"""
    pass

def test_copy_last_week_schedule():
    """复制上周排班，闭馆日期自动跳过"""
    pass
```

### 集成测试

```python
# backend/tests/integration/test_coach_schedule_api.py

def test_api_create_schedule_slots_201():
    """POST /api/coach/schedule-slots 批量添加成功"""
    pass

def test_api_delete_slot_409_has_booking():
    """删除已预约时段返回 409"""
    pass

def test_api_copy_last_week_200():
    """复制上周排班返回生成结果"""
    pass
```

### E2E 测试

```python
# e2e/tests/test_coach_schedule_flow.py

def test_e2e_coach_publish_and_user_sees_slot():
    """教练发布时段后学员端可见"""
    pass
```

---

## 验收标准映射

| GWT 场景 | 测试方法 |
|----------|----------|
| 正常添加可约时段 | test_create_schedule_slot_success / test_api_create_schedule_slots_201 |
| 批量复制上周排班 | test_copy_last_week_schedule / test_api_copy_last_week_200 |
| 添加时段冲突 | test_create_schedule_slot_conflict |
| 删除已有预约的时段 | test_delete_slot_with_booking_fails / test_api_delete_slot_409_has_booking |
| 发布后学员端可见 | test_e2e_coach_publish_and_user_sees_slot |
