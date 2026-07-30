# Spec Delta: coach-browsing

> 本 spec 为 US-001 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 公开教练列表查询

系统 MUST 提供公开教练列表查询接口，游客无需登录即可浏览。系统 MUST 仅展示状态为已通过（status=1）或申请离职中（status=4）的教练，并按综合评分降序排列；status=4 的教练不得展示"申请离职中"等任何状态标签。

#### Scenario: 正常浏览教练列表

```gherkin
Given 系统有 5 名已通过（status=1）教练
When  游客进入"找教练"页面
Then  列表按综合评分从高到低展示 5 名教练
And   每张教练卡片展示姓名、实时状态徽标、综合评分、任教年限、证书标签
```

#### Scenario: 空状态

```gherkin
Given 系统内无任何公开教练（status IN (1, 4)）
When  游客进入"找教练"页面
Then  列表展示空状态插画
And   展示文案"暂无教练入驻，敬请期待"
```

#### Scenario: 分页超出范围

```gherkin
Given 系统有 5 名已通过（status=1）教练
When  游客请求第 2 页（size=10）
Then  返回空数组 + HTTP 200
```

#### Scenario: 申请离职中教练仍可见且无标签

```gherkin
Given 系统有 1 名已通过教练和 1 名申请离职中（status=4）教练
When  游客进入"找教练"页面
Then  列表展示 2 名教练
And   申请离职中教练的卡片不展示"申请离职中"等任何状态标签
```

---

### Requirement: REQ-002 公开教练详情查询

系统 MUST 提供公开教练详情查询接口。系统 MUST 对待审核（status=0）、驳回（status=2）和已离职（status=3）的教练返回 404，不公开其信息；status=4（申请离职中）的教练可正常查询，但不得展示"申请离职中"等任何状态标签。

#### Scenario: 正常查看教练详情

```gherkin
Given 某教练状态为已通过（status=1）且实时状态为"空闲中"
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

#### Scenario: 申请离职中教练详情可见且无标签

```gherkin
Given 某教练状态为"申请离职中"（status=4）且实时状态为"空闲中"
When  游客进入该教练详情页
Then  详情页展示姓名、证书、任教年限、综合评分、评价、可约时间、实时状态
And   详情页不展示"申请离职中"等任何状态标签
```

#### Scenario: 待审核、驳回或已离职教练不出现

```gherkin
Given 某教练状态为"待审核"（status=0）、"驳回"（status=2）或"已离职"（status=3）
When  游客浏览教练列表或直接访问该教练详情 URL
Then  该教练不出现在公开列表
And   详情页提示"教练信息不存在"
```
