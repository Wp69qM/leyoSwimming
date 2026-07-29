# Spec Delta: guest-info-browsing

> 本 spec 为 US-002 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 公开套餐列表查询

系统 MUST 提供公开套餐列表查询接口，游客无需登录即可浏览。系统 MUST 仅返回上架状态（status=1）且套餐类型为体验（package_type=0）或正式（package_type=1）的套餐模板；自定义套餐（package_type=2）SHALL 不在列表接口返回。系统 MUST 按 sort_order 升序、package_type 升序（体验在前）排序。

#### Scenario: 正常浏览套餐列表

```gherkin
Given 系统已配置 1 个体验套餐 + 3 个标准套餐（1/6/8 节），且均为上架状态
When  游客进入套餐列表页
Then  列表按"体验套餐 → 正式套餐"顺序展示 4 个套餐
And   每个套餐展示名称、课时数、价格、有效期
And   排序按 sort_order 升序，sort_order 相同时体验套餐在前
```

#### Scenario: 无套餐可展示

```gherkin
Given 管理员未配置任何套餐模板，或所有套餐模板均为下架状态（status=0）
When  游客进入套餐列表页
Then  列表返回空数组 + HTTP 200
And   页面展示空状态插画 + 文案"暂无可选套餐，敬请期待"
```

#### Scenario: 自定义套餐不返回

```gherkin
Given 系统配置了 1 个 package_type=2 的自定义套餐模板且 status=1
When  游客请求套餐列表
Then  返回的 items 数组中不包含该自定义套餐模板
```

---

### Requirement: REQ-002 公开公告与场馆信息查询

系统 MUST 提供公开公告列表查询接口，仅返回 visible_scope=all 且在时间窗内（start_at <= NOW() < end_at）生效的公告，按 priority 降序排列。系统 MUST 提供公开场馆信息查询接口，返回场馆名称、地址、经纬度、开业年限、泳池状态及当前闭馆换水提示。场馆未配置时系统 MUST 返回 404 + `VENUE_NOT_CONFIGURED`。无生效公告时系统 SHALL 返回空数组（前端隐藏通知栏区域，不展示空状态）。

#### Scenario: 正常浏览公告与场馆信息

```gherkin
Given 系统有 2 条生效中公告（visible_scope=all，priority 分别为 2 和 0）
And   场馆信息已配置（名称="乐游游泳馆"，地址="北京市朝阳区..."）
When  游客进入小程序首页
Then  通知栏按 priority 降序展示 2 条公告
And   通知栏滚动展示，每条 4 秒
When  游客点击"场馆信息"入口
Then  场馆信息页展示名称、地址、开业年限、泳池状态
And   页面底部展示"导航"按钮
```

#### Scenario: 公告维护中 / 无生效公告

```gherkin
Given 系统无任何生效中公告（所有公告 start_at/end_at 不在当前时间区间）
When  游客进入首页
Then  通知栏区域整体隐藏
And   套餐入口与场馆入口正常展示
```

#### Scenario: 场馆信息未配置

```gherkin
Given 管理员尚未配置场馆信息（venue 表为空）
When  游客点击"场馆信息"入口
Then  场馆信息接口返回 404 + VENUE_NOT_CONFIGURED
And   页面展示占位文案"场馆信息配置中"
And   不展示"导航"按钮
```

#### Scenario: 闭馆换水时间窗命中

```gherkin
Given venue_closure 表存在一条记录，start_at <= NOW() < end_at
And   closure_type=1（换水）
When  游客请求场馆信息
Then  返回的 closureNotice 字段填充 type="换水"、reason、startAt、endAt
And   前端场馆信息页置顶红色提示条展示换水信息
```
