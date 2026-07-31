# Spec Delta: admin-config-standard-custom-packages

> 本 spec 为 US-045 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员新增标准套餐

系统 MUST 提供管理员新增标准套餐模板接口。系统 MUST 校验套餐名称唯一性、课时数大于 0、售价非负；校验通过后写入 `package_template` 表并返回创建结果。

#### Scenario: 管理员新增标准套餐成功

```gherkin
Given 管理员已登录且具有套餐配置权限
And   系统中不存在名称为"暑期 10 节课"的标准套餐
When  管理员提交标准套餐：名称="暑期 10 节课", 课时数=10, 有效期=180 天, 售价=3000.00, 状态=上架
Then  系统返回 HTTP 200 且 package_template 表新增 1 条记录
And   该记录 status='active'，total_hours=10，price=3000.00
And   前端列表展示"暑期 10 节课"且状态为"上架"
```

#### Scenario: 新增标准套餐时名称重复

```gherkin
Given 系统中已存在名称为"暑期 10 节课"的标准套餐
When  管理员再次提交名称为"暑期 10 节课"的标准套餐
Then  系统返回错误码 DUPLICATE_PACKAGE_NAME
And   HTTP 状态码 409
And   package_template 表不新增记录
```

#### Scenario: 新增标准套餐参数非法

```gherkin
Given 管理员已登录
When  管理员提交标准套餐：课时数=0，售价=-100
Then  系统返回错误码 INVALID_PACKAGE_PARAM
And   HTTP 状态码 400
And   提示"课时数必须大于 0，售价不能为负数"
```

---

### Requirement: REQ-002 管理员编辑与上下架标准套餐

系统 MUST 提供管理员编辑标准套餐模板及上下架切换接口。系统 MUST 校验模板存在性；下架后游客端/学员端 MUST 不再展示该套餐，已购 package 不受影响。

#### Scenario: 管理员编辑标准套餐并下架

```gherkin
Given 系统中已存在标准套餐 A，status='active'，price=2000.00
When  管理员将套餐 A 价格改为 1800.00 并设置为下架
Then  系统返回 HTTP 200
And   package_template.price 更新为 1800.00
And   package_template.status 更新为 'inactive'
And   游客端/学员端不再展示该套餐
```

#### Scenario: 编辑不存在的套餐

```gherkin
Given 系统中不存在 id=99999 的标准套餐
When  管理员编辑 id=99999 的套餐
Then  系统返回 HTTP 404
And   返回错误码 TEMPLATE_NOT_FOUND
```

---

### Requirement: REQ-003 管理员权限控制

系统 MUST 对套餐配置接口进行权限控制。无 `package:write` 权限的管理员 MUST 无法调用写接口，系统 MUST 返回 HTTP 403。

#### Scenario: 无权限管理员访问配置接口

```gherkin
Given 管理员已登录但角色无"套餐配置"权限
When  管理员调用 POST /api/admin/package-templates
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
And   package_template 表不新增记录
```

---

### Requirement: REQ-004 管理员配置自定义套餐规则

系统 MUST 提供管理员配置自定义套餐全局规则接口。系统 MUST 校验 0 < min_hours ≤ max_hours ≤ 100，default_valid_days > 0，unit_price_floor ≥ 0；校验通过后写入 `custom_package_config` 表（全局仅保留一条记录）。

#### Scenario: 管理员配置自定义套餐规则成功

```gherkin
Given 管理员已登录且具有套餐配置权限
And   当前 custom_package_config 表无记录
When  管理员提交自定义套餐规则：min_hours=5, max_hours=50, default_valid_days=180, unit_price_floor=200.00
Then  系统返回 HTTP 200
And   custom_package_config 表新增 1 条记录
And   该记录 min_hours=5, max_hours=50, default_valid_days=180, unit_price_floor=200.00
And   学员端购买自定义套餐时课时数可选范围变为 5~50
```

#### Scenario: 自定义套餐规则参数非法

```gherkin
Given 管理员已登录且具有套餐配置权限
When  管理员提交自定义套餐规则：min_hours=0, max_hours=50, default_valid_days=0, unit_price_floor=-10
Then  系统返回 HTTP 400
And   返回错误码 INVALID_CUSTOM_PACKAGE_CONFIG
And   custom_package_config 表不新增记录
```
