# leyoSwimming OpenAPI 变更日志

> 所有 API 变更必须在合并到主分支前记录于此。版本号遵循 SemVer。

## [1.0.0] - 2026-08-05

### Added

- 微信一键登录 `POST /api/v1/auth/wechat-login`，支持 `app_type=user` 与 `app_type=coach`。
- 手机号验证码登录 `POST /api/v1/auth/phone-code-login`。
- 发送短信验证码 `POST /api/v1/auth/send-phone-code`。
- 管理员账号密码登录 `POST /api/v1/admin/auth/login`。
- 教练入驻资料提交 `POST /api/coach/application`。
- 教练入驻资料查询 `GET /api/coach/application`，支持 `entry_type` 与 `prompt_message`。
- 教练入驻资料草稿保存 `PUT /api/coach/application/draft`。

### Notes

- 教练端登录返回 `coach_status`，前端根据状态分流，不再返回 `redirect_page`。
- `GET /api/coach/application` 对 status=2 返回 `entry_type=rejected` 与驳回原因；对 status=3 返回 `entry_type=reapply` 与重新入驻说明。
