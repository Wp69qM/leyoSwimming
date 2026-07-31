## Why

正价套餐购买是注册用户转化为正式学员的核心交易入口。用户需要选择教练、课时数，确认协议后生成待支付订单。

## What Changes

- 新增 `POST /api/orders/formal` 创建正价套餐订单
- 新增 `GET /api/agreements/status` 查询协议签署状态
- 新增协议签署记录写入
- 新增单教练冲突校验、教练状态校验、自定义课时校验
- 新增订单幂等机制
- 新增未成年人监护人手机号短信校验与订单创建后短信通知

## Capabilities

### New Capabilities

- `student-buy-formal-package`: 学员/注册用户购买正价套餐

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `order`、`agreement_sign` 记录
- **API**：新增 2 个端点
- **校验**：单教练约束、教练状态、协议版本、自定义课时范围
- **依赖**：依赖 US-004/US-005 / US-011 / US-019 / US-045；被 US-021 / US-025 依赖
