# G3 教练入驻与审核（US-010 / US-011 / US-040）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 实现教练端入驻资料提交（含草稿/正式提交/重新入驻）与管理后台入驻资质审核的完整后端 API + 教练端小程序页面 + web-admin 审核页面。

**Architecture:** 后端在现有 Spring Boot + MyBatis-Plus + Flyway 架构上新增 coach_application / coach_certificate_application / coach_certificate / coach_audit_log 四表，通过 CoachOnboardingService 处理入驻状态机与快照，通过 CoachAuditService 处理管理员审核；教练端小程序使用 Taro React + Zustand 新增 pages/onboarding/* 三页面；web-admin 使用 Vue 3 + Element Plus + Pinia 新增教练审核列表/详情页面。

**Tech Stack:** Java 21 / Spring Boot 3.2 / MyBatis-Plus / Flyway / H2 & MySQL / Taro React / Vue 3 / Element Plus / Pinia / Zustand

---

## 0. 上下文与约定

### 0.1 已存在的关键文件

| 文件 | 说明 |
|------|------|
| backend/src/main/java/com/leyoswimming/entity/Coach.java | 现有实体，仅含基础字段 |
| backend/src/main/java/com/leyoswimming/enums/CoachStatus.java | 已定义 -1/0/1/2/3/4 状态 |
| backend/src/main/java/com/leyoswimming/common/ErrorCode.java | 已有 COACH_APPLICATION_PENDING(500001) |
| backend/src/main/java/com/leyoswimming/service/CoachAuthService.java | 当前对已离职教练抛 COACH_NOT_FOUND |
| miniapp-coach/src/stores/authStore.ts | 已定义 getRedirectPageByStatus |
| miniapp-coach/src/app.config.ts | 当前仅注册首页与登录页 |
| web-admin/src/router/index.ts | 当前仅首页与登录页 |
| web-admin/src/components/layout/AdminLayout.vue | 当前仅首页菜单 |

### 0.2 状态机速查

| 触发条件 | coach.status | coach_application.status | previous_coach_status |
|----------|--------------|--------------------------|----------------------|
| 首次保存草稿 | -1 | draft | NULL |
| 首次提交审核 | -1 -> 0 | pending | -1 |
| 已驳回重新提交 | 2 -> 0 | pending | 2 |
| 已离职重新入驻 | 3 -> 0 | pending | 3 |
| 管理员通过 | 0 -> 1 | pending -> approved | NULL |
| 管理员驳回 | 0 -> previous_coach_status | pending -> rejected | -1->2 / 2 / 3 |

### 0.3 接口清单

| 接口 | 说明 |
|------|------|
| POST /api/coach/application/detail | 教练端查询入驻资料快照 |
| POST /api/coach/application/save-draft | 保存草稿 |
| POST /api/coach/application/submit | 提交审核 |
| POST /api/common/file/upload | 通用图片上传（已存在，复用） |
| POST /api/admin/coach/application/list | 审核列表 |
| POST /api/admin/coach/application/detail | 审核详情 |
| POST /api/admin/coach/application/approve | 通过 |
| POST /api/admin/coach/application/reject | 驳回 |

### 0.4 测试命令速查

`ash
cd backend
./mvnw test -Dtest=CoachOnboardingServiceTest,CoachAuditServiceTest,CoachApplicationControllerIT,AdminCoachApplicationControllerIT

cd ../miniapp-coach
npm test

cd ../web-admin
npm test
`

---

## 1. 文件结构总览

### 1.1 后端新增文件

| 文件 | 用途 |
|------|------|
| backend/src/main/resources/db/migration/V7__alter_coach_add_profile_fields.sql | Coach 表扩展字段 |
| backend/src/main/resources/db/migration/V8__create_coach_application_and_certificate.sql | 申请快照与证书表 |
| backend/src/main/resources/db/migration/V9__create_coach_audit_log.sql | 审核日志表 |
| backend/src/main/java/com/leyoswimming/entity/CoachApplication.java | 入驻申请快照实体 |
| backend/src/main/java/com/leyoswimming/entity/CoachCertificateApplication.java | 申请关联证书实体 |
| backend/src/main/java/com/leyoswimming/entity/CoachCertificate.java | 生效证书实体 |
| backend/src/main/java/com/leyoswimming/entity/CoachAuditLog.java | 审核日志实体 |
| backend/src/main/java/com/leyoswimming/repository/CoachApplicationMapper.java | 申请快照 Mapper |
| backend/src/main/java/com/leyoswimming/repository/CoachCertificateApplicationMapper.java | 申请证书 Mapper |
| backend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java | 生效证书 Mapper |
| backend/src/main/java/com/leyoswimming/repository/CoachAuditLogMapper.java | 审核日志 Mapper |
| backend/src/main/java/com/leyoswimming/dto/request/CoachApplicationRequest.java | 保存草稿/提交审核请求 DTO |
| backend/src/main/java/com/leyoswimming/dto/request/CoachCertificateItem.java | 证书条目 DTO |
| backend/src/main/java/com/leyoswimming/dto/request/AdminAuditRequest.java | 管理员通过/驳回请求 DTO |
| backend/src/main/java/com/leyoswimming/dto/request/AdminCoachApplicationListRequest.java | 审核列表请求 DTO |
| backend/src/main/java/com/leyoswimming/dto/response/CoachApplicationResponse.java | 教练端资料详情响应 |
| backend/src/main/java/com/leyoswimming/dto/response/AdminCoachApplicationListResponse.java | 审核列表响应 |
| backend/src/main/java/com/leyoswimming/dto/response/AdminCoachApplicationDetailResponse.java | 审核详情响应 |
| backend/src/main/java/com/leyoswimming/dto/response/CoachApplicationHistoryItem.java | 申请历史条目 |
| backend/src/main/java/com/leyoswimming/service/CoachOnboardingService.java | 入驻资料业务逻辑 |
| backend/src/main/java/com/leyoswimming/service/CoachAuditService.java | 管理员审核业务逻辑 |
| backend/src/main/java/com/leyoswimming/service/IdCardEncryptor.java | 身份证号 AES 加解密 |
| backend/src/main/java/com/leyoswimming/controller/coach/CoachApplicationController.java | 教练端接口 |
| backend/src/main/java/com/leyoswimming/controller/admin/AdminCoachApplicationController.java | 管理后台接口 |

### 1.2 后端修改文件

| 文件 | 修改内容 |
|------|----------|
| backend/src/main/java/com/leyoswimming/entity/Coach.java | 新增入驻资料字段，删除 rejection_reason |
| backend/src/main/java/com/leyoswimming/common/ErrorCode.java | 新增 INVALID_ID_CARD、IMAGE_TOO_LARGE、INVALID_IMAGE_FORMAT、INVALID_REFERENCE_PRICE、COACH_STATUS_NOT_ALLOWED |
| backend/src/main/java/com/leyoswimming/service/CoachAuthService.java | 已离职教练登录不再抛异常，允许正常登录后跳转入驻页 |
| backend/src/main/java/com/leyoswimming/security/JwtTokenProvider.java | 如 Coach 鉴权拒绝 status=-1/2/3，需放行 application 接口（或已在 JwtAuthenticationFilter 白名单处理） |

### 1.3 小程序前端新增文件

| 文件 | 用途 |
|------|------|
| miniapp-coach/src/api/onboarding.ts | 入驻资料 API 客户端 |
| miniapp-coach/src/types/onboarding.ts | 入驻资料类型定义 |
| miniapp-coach/src/pages/onboarding/index/index.tsx | 入驻资料填写页 |
| miniapp-coach/src/pages/onboarding/index/index.scss | 填写页样式 |
| miniapp-coach/src/pages/onboarding/success/index.tsx | 提交成功页 |
| miniapp-coach/src/pages/onboarding/success/index.scss | 成功页样式 |
| miniapp-coach/src/pages/onboarding/pending/index.tsx | 等待审核页 |
| miniapp-coach/src/pages/onboarding/pending/index.scss | 等待审核页样式 |
| miniapp-coach/src/components/onboarding/FormGroup.tsx | 表单分组容器 |
| miniapp-coach/src/components/onboarding/ImageUploader.tsx | 图片上传组件 |
| miniapp-coach/src/components/onboarding/StrokeSelector.tsx | 泳姿多选组件 |

### 1.4 小程序前端修改文件

| 文件 | 修改内容 |
|------|----------|
| miniapp-coach/src/app.config.ts | 注册 onboarding 三页面 |
| miniapp-coach/src/api/request.ts | 如需，补充错误码映射 |
| miniapp-coach/src/constants/index.ts | 补充 COACH_STATUS 枚举与错误码常量 |

### 1.5 Web-Admin 前端新增文件

| 文件 | 用途 |
|------|------|
| web-admin/src/api/coachApplication.ts | 审核 API 客户端 |
| web-admin/src/types/coachApplication.ts | 审核类型定义 |
| web-admin/src/views/coach-audit/CoachAuditListView.vue | 审核列表页 |
| web-admin/src/views/coach-audit/CoachAuditDetailView.vue | 审核详情页 |
| web-admin/src/components/coach-audit/RejectDialog.vue | 驳回原因弹窗 |
| web-admin/src/components/coach-audit/CertificateGallery.vue | 证书图片预览组件 |

### 1.6 Web-Admin 前端修改文件

| 文件 | 修改内容 |
|------|----------|
| web-admin/src/router/index.ts | 添加教练审核列表/详情路由 |
| web-admin/src/components/layout/AdminLayout.vue | 添加「教练审核」菜单入口 |

---

