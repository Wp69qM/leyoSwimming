> **OpenSpec Spec | 映射自 `docs/stories/US-005-用户-完善个人资料/user-story.md` §6**

## Capability

用户完善个人资料

## ADDED Requirements

### Requirement: REQ-001 首次登录后强制完善资料

当用户使用微信授权登录或手机号验证码登录成功后，若 `profile_completed=false`，系统 MUST 强制跳转至完善个人资料页，未保存必填项前不可进入首页。未成年人（`age < 18`）MUST 额外填写监护人姓名与监护人手机号。

#### Scenario: 微信授权登录后首次完善个人资料（成年人）
- **GIVEN** 用户已完成微信授权登录，`identity_status = '注册用户'` 且 `profile_completed = false`
- **AND** 系统未存在手机号 13800138000 的其他注册用户
- **AND** 用户已同意隐私协议 v2.0
- **WHEN** 用户上传头像、填写姓名 "张 swimmer"、确认手机号 13800138000、选择年龄 25、性别 男
- **AND** 用户填写有游泳基础，选择泳姿 "蛙泳/自由泳"，游泳年限 "3年"，个人描述 "想提高自由泳"
- **AND** 用户点击「保存」
- **THEN** 系统保存成功
- **AND** `user.profile_completed = true`
- **AND** `user.identity_status` 保持 '注册用户' 不变
- **AND** `user.status = 0`（正常）
- **AND** `user.phone = 13800138000`
- **AND** `user.name = 张 swimmer`
- **AND** `user.age = 25`
- **AND** `user.gender = 'male'`
- **AND** 页面跳转至首页

#### Scenario: 未成年人首次登录后完善个人资料并填写监护人信息
- **GIVEN** 用户已完成微信授权登录，`identity_status = '注册用户'` 且 `profile_completed = false`
- **AND** 系统未存在手机号 13900139000 的其他注册用户
- **AND** 用户已同意隐私协议 v2.0
- **WHEN** 用户上传头像、填写姓名 "李小小"、确认手机号 13900139000、选择年龄 12、性别 女
- **AND** 系统展示监护人信息填写区
- **AND** 用户填写监护人姓名 "李大伟"、监护人手机号 13800138000
- **AND** 用户选择无游泳基础
- **AND** 用户点击「保存」
- **THEN** 系统保存成功
- **AND** `user.profile_completed = true`
- **AND** `user.age = 12`
- **AND** `user.guardian_name = "李大伟"`
- **AND** `user.guardian_phone = 13800138000`
- **AND** 页面跳转至首页

### Requirement: REQ-002 从「我的」编辑个人资料

系统 MUST 允许已完善资料的用户从「我的 → 个人资料」进入编辑资料页，修改任意字段后保存。年龄变更导致未成年人/成年人身份切换时，系统 MUST 动态展示或隐藏监护人信息填写区。

#### Scenario: 从「我的」编辑个人资料
- **GIVEN** 用户已完善个人资料，`profile_completed = true`
- **WHEN** 用户在「我的」页面点击「编辑资料」
- **AND** 用户将姓名从 "张 swimmer" 修改为 "张教练"
- **AND** 用户点击「保存」
- **THEN** 系统保存成功
- **AND** `user.name = "张教练"`
- **AND** `user.profile_completed` 保持 true
- **AND** 页面返回「我的」页面

### Requirement: REQ-003 手机号唯一性校验

用户修改手机号时，系统 MUST 校验该手机号未被其他账号绑定。

#### Scenario: 手机号已被其他账号绑定
- **GIVEN** 系统中已存在手机号 13800138000 的其他注册用户
- **WHEN** 用户在个人资料页将手机号修改为 13800138000 并提交
- **THEN** 返回错误码 `PHONE_ALREADY_BOUND`
- **AND** 前端提示"该手机号已绑定其他账号，请更换"
- **AND** 用户资料未被保存

### Requirement: REQ-004 必填项缺失校验

头像、姓名、手机号、年龄、性别为必填项，缺失时系统 MUST 阻止提交。未成年人（`age < 18`）MUST 额外校验监护人姓名和监护人手机号必填。

#### Scenario: 必填项缺失
- **GIVEN** 用户已完成登录
- **WHEN** 用户未填写姓名直接点击「保存」
- **THEN** 前端阻止提交
- **AND** 姓名输入框下方提示"姓名不能为空"
- **AND** 后端未收到请求

#### Scenario: 未成年人未填写监护人信息
- **GIVEN** 用户已完成登录
- **WHEN** 用户选择年龄 12
- **AND** 用户未填写监护人姓名和监护人手机号
- **AND** 用户点击「保存」
- **THEN** 前端阻止提交
- **AND** 监护人信息区域提示"请填写监护人姓名和手机号"
- **AND** 后端未收到请求

### Requirement: REQ-005 姓名敏感词校验

系统 MUST 对用户输入的姓名进行敏感词过滤。

#### Scenario: 姓名含敏感词
- **GIVEN** 用户已完成登录
- **WHEN** 用户填写姓名 "习近平" 并提交
- **THEN** 返回错误码 `SENSITIVE_NAME`
- **AND** 前端提示"姓名包含敏感内容，请重新输入"
- **AND** `user.name` 未被保存

### Requirement: REQ-006 游泳基础字段联动

当用户选择「无游泳基础」时，系统 MUST 隐藏「会什么泳姿」和「游泳年限」字段且不收集；选择「有游泳基础」时系统 MUST 展示并收集这两个字段。

#### Scenario: 无游泳基础时隐藏泳姿和年限
- **GIVEN** 用户进入完善个人资料页
- **WHEN** 用户选择「无游泳基础」
- **THEN** 系统隐藏「会什么泳姿」和「游泳年限」字段
- **AND** 提交时忽略这两个字段

### Requirement: REQ-007 未成年人监护人信息联动

当用户填写的年龄 < 18 周岁时，系统 MUST 动态展示监护人姓名和监护人手机号输入区，且这两个字段 MUST 为必填；当用户填写的年龄 ≥ 18 周岁时，系统 MUST 隐藏监护人信息区，但已保存的 `guardian_name` / `guardian_phone` 可保留。

#### Scenario: 用户将年龄从成年人修改为未成年人
- **GIVEN** 用户已进入完善个人资料页
- **WHEN** 用户将年龄从 25 修改为 12
- **THEN** 系统动态展示监护人姓名和监护人手机号输入区
- **AND** 用户点击「保存」时校验监护人信息必填

#### Scenario: 用户将年龄从未成年人修改为成年人
- **GIVEN** 用户已进入完善个人资料页，当前年龄为 12
- **WHEN** 用户将年龄从 12 修改为 25
- **THEN** 系统隐藏监护人信息填写区
- **AND** 已保存的 `guardian_name` / `guardian_phone` 保留在数据库但不再展示和校验必填

## Delta Header

```yaml
delta:
  change: us-005-user-complete-profile
  capability: user-complete-profile
  type: revise
  rationale: 新增未成年人监护人信息管理：年龄 < 18 时必填 guardian_name / guardian_phone；补充对应 GWT 场景、异常分支与边界场景
  scope: docs/stories/US-005, openspec/changes/us-005-user-complete-profile
```
