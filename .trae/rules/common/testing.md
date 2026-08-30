# Testing Requirements

## Minimum Test Coverage: 80%

Test Types (ALL required):
1. **Unit Tests** - Individual functions, utilities, components
2. **Integration Tests** - API endpoints, database operations
3. **E2E Tests** - Critical user flows (framework chosen per language)

## Test-Driven Development

MANDATORY workflow:
1. Write test first (RED)
2. Run test - it should FAIL
3. Write minimal implementation (GREEN)
4. Run test - it should PASS
5. Refactor (IMPROVE)
6. Verify coverage (80%+)

## Troubleshooting Test Failures

1. Use **tdd-guide** agent
2. Check test isolation
3. Verify mocks are correct
4. Fix implementation, not tests (unless tests are wrong)

## Agent Support

- **tdd-guide** - Use PROACTIVELY for new features, enforces write-tests-first

## 执行检查清单（新功能 / Bug 修复 / 重构前必须完成）

- [ ] 已使用 `tdd-guide` agent 或按 RED-GREEN-IMPROVE 流程开发
- [ ] 新增/修改的业务代码有对应单元测试
- [ ] 涉及数据库/外部接口的有集成测试
- [ ] 关键用户流程有 E2E 测试或用例记录
- [ ] 本地运行 `./mvnw test`、`pytest`、`vitest run` 全部通过
- [ ] 覆盖率未低于 80%（新增代码优先达到 100%）
- [ ] `code-reviewer` agent 已确认测试存在且覆盖核心路径

## code-reviewer 强制检查项

- 任何新增 public 方法必须有测试
- 任何 bug 修复必须包含回归测试
- 任何删除/修改现有测试必须说明原因
- 未达 80% 覆盖率 → 高亮问题，要求补充

## Test Structure (AAA Pattern)

Prefer Arrange-Act-Assert structure for tests:

```typescript
test('calculates similarity correctly', () => {
  // Arrange
  const vector1 = [1, 0, 0]
  const vector2 = [0, 1, 0]

  // Act
  const similarity = calculateCosineSimilarity(vector1, vector2)

  // Assert
  expect(similarity).toBe(0)
})
```

### Test Naming

Use descriptive names that explain the behavior under test:

```typescript
test('returns empty array when no markets match query', () => {})
test('throws error when API key is missing', () => {})
test('falls back to substring search when Redis is unavailable', () => {})
```
