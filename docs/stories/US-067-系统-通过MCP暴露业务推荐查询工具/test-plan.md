# Test Plan: US-067 系统-通过MCP暴露业务推荐查询工具

## 1. 测试范围

- 3 个推荐工具的注册与调用
- 中文参数归一化（泳姿/性别/套餐模式）
- 用户身份类工具排除断言
- backend 不可达/超时的故障隔离
- limit 截断与并发调用

## 2. 测试用例

### 2.1 单元测试

| 用例 | 输入 | 预期 | 对应场景 |
|------|------|------|---------|
| `test_mcp_query_coaches_chinese_normalized` | stroke="蛙泳", gender="女" | 内部收到 breaststroke / female，返回过滤后列表 | 场景 1 |
| `test_mcp_query_packages_mode_normalized` | package_mode="体验" | 归一化为 experience | 场景 2 |
| `test_mcp_get_hot_recommendations` | limit=5 | 返回 ≤5 条热门数据 | 场景 2 |
| `test_mcp_unknown_stroke_degrades_to_none` | stroke="狗刨" | 归一化为 None，按无过滤查询 | 场景 5 |
| `test_mcp_limit_clamped` | limit=1000 | 截断至 20 | 边界 1 |
| `test_mcp_backend_down_returns_structured_error` | mock backend 不可达 | 返回 `{"error": ...}`，不抛异常 | 场景 3 |
| `test_normalizers_shared_by_both_layers` | 同输入分别调 LangChain/MCP 路径 | 归一化结果一致 | 备注 |

### 2.2 集成测试

| 用例 | 步骤 | 预期 |
|------|------|------|
| 工具清单排除断言 | tools/list | 不含 get_user_profile / get_user_packages（场景 4） |
| 工具间故障隔离 | 停 backend → 调 query_coaches → 调 query_knowledge | 前者结构化错误，后者正常（场景 3） |
| backend 超时 | mock 延迟超过阈值 | 返回超时错误，连接保持（边界 2） |
| 并发调用 | 并发 query_coaches + query_packages | 两者独立正确返回（边界 3） |

### 2.3 E2E 测试

| 用例 | 步骤 | 预期 |
|------|------|------|
| MCP 客户端全工具调用 | SDK client 依次调用 3 个推荐工具 | 全部返回真实业务数据（依赖本地 backend 运行） |

## 3. 验收检查清单

- [ ] tools/list 含 3 个推荐工具且不含用户身份工具
- [ ] 中文泳姿/性别/套餐模式归一化正确
- [ ] backend 停止时推荐工具返回结构化错误、知识工具不受影响
- [ ] limit 超限被截断
- [ ] 归一化函数两条协议层共用一份实现
- [ ] 单测覆盖 ≥ 80%
