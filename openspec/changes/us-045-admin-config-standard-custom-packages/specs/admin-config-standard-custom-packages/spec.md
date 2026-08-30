# Spec Delta: admin-config-standard-custom-packages

> 本 spec 为 US-045 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 管理员新增标准套餐

系统 MUST 提供管理员新增标准套餐模板接口。系统 MUST 校验套餐名称唯一性、课时数大于 0、售价非负；校验通过后写入 `package_template` 表并返回创建结果。

#### Scenario: 管理员新增标准套餐成功

```gherkin
Given 管理员已登录且具有套餐配置权限
And   系统中不存在名称为"暑期 10 节课"的标准套餐
When  管理员提交 POST /api/admin/package-template/add，请求体：
  {
    "name": "暑期 10 节课",
    "coachIds": [1, 2],
    "totalHours": 10,
    "validDays": 180,
    "price": 300000,
    "status": "inactive"
  }
Then  系统返回 HTTP 201 且 package_template 表新增 1 条记录
And   package_template_coach 表新增 2 条记录，分别关联教练 1 与教练 2
And   该记录 status='inactive'，total_hours=10，price=3000.00
And   前端列表展示"暑期 10 节课"且状态为"未上架"
```

#### Scenario: 新增标准套餐时名称重复

```gherkin
Given 系统中已存在名称为"暑期 10 节课"的标准套餐
When  管理员再次提交 POST /api/admin/package-template/add，请求体 { "name": "暑期 10 节课" }
Then  系统返回错误码 DUPLICATE_PACKAGE_NAME
And   HTTP 状态码 409
And   package_template 表不新增记录
```

#### Scenario: 新增标准套餐参数非法

```gherkin
Given 管理员已登录
When  管理员提交 POST /api/admin/package-template/add，请求体 { "totalHours": 0, "price": -100 }
Then  系统返回错误码 INVALID_PACKAGE_PARAM
And   HTTP 状态码 400
And   提示"课时数必须大于 0，售价不能为负数"
```

---

### Requirement: REQ-002 管理员编辑、详情查询与上下架标准套餐

系统 MUST 提供管理员编辑标准套餐模板、查询详情及上下架切换接口。系统 MUST 校验模板存在性；下架后游客端/学员端 MUST 不再展示该套餐，已购 package 不受影响。

#### Scenario: 管理员编辑标准套餐并下架

```gherkin
Given 系统中已存在标准套餐 A，status='active'，price=2000.00，packageTemplateId=1
When  管理员提交 POST /api/admin/package-template/update，请求体：
  {
    "packageTemplateId": 1,
    "price": 1800.00,
    "status": "inactive"
  }
Then  系统返回 HTTP 200
And   package_template.price 更新为 1800.00
And   package_template.status 更新为 'inactive'
And   游客端/学员端不再展示该套餐
```

#### Scenario: 编辑不存在的套餐

```gherkin
Given 系统中不存在 packageTemplateId=99999 的标准套餐
When  管理员提交 POST /api/admin/package-template/update，请求体 { "packageTemplateId": 99999 }
Then  系统返回 HTTP 404
And   返回错误码 TEMPLATE_NOT_FOUND
```

#### Scenario: 查询套餐详情

```gherkin
Given 系统中已存在标准套餐 A，packageTemplateId=1，关联教练 [1, 2]
When  管理员提交 POST /api/admin/package-template/detail，请求体 { "packageTemplateId": 1 }
Then  系统返回 HTTP 200
And   响应包含 packageTemplateId、name、coachIds [1, 2]、totalHours、validDays、price、status
```

---

### Requirement: REQ-003 管理员权限控制

系统 MUST 对套餐配置接口进行权限控制。无 `package:write` 权限的管理员 MUST 无法调用写接口，系统 MUST 返回 HTTP 403。

#### Scenario: 无权限管理员访问配置接口

```gherkin
Given 管理员已登录但角色无"套餐配置"权限
When  管理员调用 POST /api/admin/package-template/add
Then  系统返回 HTTP 403
And   返回错误码 FORBIDDEN
And   package_template 表不新增记录
```

---

### Requirement: REQ-004 管理员配置自定义套餐规则

系统 MUST 提供管理员配置自定义套餐全局规则接口。系统 MUST 校验 0 < minHours ≤ maxHours ≤ 100，defaultValidDays > 0，unitPriceFloor ≥ 0；校验通过后写入 `custom_package_config` 表（全局仅保留一条记录）。

#### Scenario: 管理员配置自定义套餐规则成功

```gherkin
Given 管理员已登录且具有套餐配置权限
And   当前 custom_package_config 表无记录
When  管理员提交 POST /api/admin/package-template/custom-config，请求体：
  {
    "minHours": 5,
    "maxHours": 50,
    "defaultValidDays": 180,
    "unitPriceFloor": 20000
  }
Then  系统返回 HTTP 200
And   custom_package_config 表新增 1 条记录
And   该记录 min_hours=5, max_hours=50, default_valid_days=180, unit_price_floor=200.00
And   学员端购买自定义套餐时课时数可选范围变为 5~50
```

#### Scenario: 自定义套餐规则参数非法

```gherkin
Given 管理员已登录且具有套餐配置权限
When  管理员提交 POST /api/admin/package-template/custom-config，请求体：
  {
    "minHours": 0,
    "maxHours": 50,
    "defaultValidDays": 0,
    "unitPriceFloor": -10
  }
Then  系统返回 HTTP 400
And   返回错误码 INVALID_CUSTOM_PACKAGE_CONFIG
And   custom_package_config 表不新增记录
```
