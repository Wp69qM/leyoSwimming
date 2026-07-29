# Spec Delta: coach-browsing

> 本 spec 为 US-001 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 公开教练列表查询

系统 MUST 提供公开教练列表查询接口，游客无需登录即可浏览。系统 MUST 仅展示在职教练（status=1），并按综合评分降序排列。

#### Scenario: 正常浏览教练列表

```gherkin
Given 系统有 5 名在职教练
When  游客进入"找教练"页面
Then  列表按综合评分从高到低展示 5 名教练
And   每张教练卡片展示姓名、实时状态徽标、综合评分、任教年限、证书标签
```

#### Scenario: 空状态

```gherkin
Given 系统内无任何在职教练
When  游客进入"找教练"页面
Then  列表展示空状态插画
And   展示文案"暂无教练入驻，敬请期待"
```

#### Scenario: 分页超出范围

```gherkin
Given 系统有 5 名在职教练
When  游客请求第 2 页（size=10）
Then  返回空数组 + HTTP 200
```

---

### Requirement: REQ-002 公开教练详情查询

系统 MUST 提供公开教练详情查询接口。系统 MUST 对申请中（status=0）和已离职（status=4）的教练返回 404，不公开其信息。

#### Scenario: 正常查看教练详情

```gherkin
Given 某教练状态为在职（status=1）且实时状态为"空闲中"
When  游客进入该教练详情页
Then  详情页展示姓名、证书、任教年限、综合评分、评价、可约时间、实时状态
And   实时状态为"空闲中"时展示"立即预约"按钮
```

#### Scenario: 教练请假中

```gherkin
Given 某教练实时状态为"请假中"
When  游客进入该教练详情页
Then  详情页正常展示教练资料
And   实时状态徽标显示"请假中"
And   不展示"立即预约"按钮
And   展示文案"教练当前请假中，可查看其可约时间"
```

#### Scenario: 教练申请中不出现

```gherkin
Given 某教练状态为"申请中"（status=0）
When  游客浏览教练列表或直接访问该教练详情 URL
Then  该教练不出现在公开列表
And   详情页提示"教练信息不存在"
```
