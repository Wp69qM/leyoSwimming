# G6 个人主页、参考单价与退出登录实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 完成第一批次 G6 组三个 US：US-012 教练管理个人主页与参考单价、US-052 用户退出登录、US-056 教练退出登录的后端 API 与小程序前端实现。

**Architecture:** 后端基于 Spring Boot 3.2 + Java 21 + MyBatis-Plus + Flyway，新增教练资料字段与证书/变更日志表；新增教练资料与参考单价管理接口，复用现有统一 POST /api/{module}/{resource}/{action} 风格。教练/用户端小程序基于 Taro 3 React + Zustand，新增「我的」页面与资料编辑页面，调用后端接口。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, MockMvc; Taro 3 React, Zustand, SCSS, Jest, React Testing Library.

---

## Design Decisions

1. **数据库变更拆分为 V15 + V16**
   - V15 仅扩展 coach 表字段（个人主页可编辑字段 + 参考单价相关字段），保持现有数据不变。
   - V16 新建 coach_certificate（证书图片，含 PORTRAIT/WECHAT_QR/QUALIFICATION/HEALTH/ID_CARD 等类型）与 coach_update_log（字段级变更审计日志）。

2. **个人形象照与微信二维码的存储位置**
   - 个人形象照以 cert_type = 'PORTRAIT' 存入 coach_certificate，查询时取该教练最新一条。
   - 微信二维码以 cert_type = 'WECHAT_QR' 存入 coach_certificate。
   - 身份证、教练资格证、健康证等入驻资质证书同样存入 coach_certificate，但本 US 只读、不修改。

3. **参考单价独立接口**
   - /api/coach/reference-price/update 单独处理参考单价，便于做范围校验、改价频率限制与缓存失效。

4. **退出登录实现策略**
   - 用户退出：后端 /api/user/auth/logout 已存在，本次只需在小程序「我的」页面接入。
   - 教练退出：后端需补 /api/coach/auth/logout，复用 CoachSessionService.logout() 删除当前 refresh_token 对应会话；前端在教练端「我的」页面接入。

5. **敏感词过滤**
   - MVP 使用内存敏感词列表（硬编码常见敏感词），封装 SensitiveWordFilter 工具类；后续可替换为配置中心或第三方服务。

6. **图片上传复用**
   - 复用项目已有文件上传能力（如 US-005/US-010 实现后）。本计划假设 /api/common/file/upload 已存在；若未实现，则先实现一个最小版本（本地存储 + 格式/大小校验）。

---

## Files to Create / Modify

### Backend

| 文件 | 类型 | 说明 |
|------|------|------|
| ackend/src/main/resources/db/migration/V15__coach_profile_fields.sql | 新增 | 扩展 coach 表 |
| ackend/src/main/resources/db/migration/V16__coach_certificate_and_update_log.sql | 新增 | 新建证书与审计日志表 |
| ackend/src/main/java/com/leyoswimming/entity/Coach.java | 修改 | 新增资料字段 |
| ackend/src/main/java/com/leyoswimming/entity/CoachCertificate.java | 新增 | 证书实体 |
| ackend/src/main/java/com/leyoswimming/entity/CoachUpdateLog.java | 新增 | 变更日志实体 |
| ackend/src/main/java/com/leyoswimming/enums/CertificateType.java | 新增 | 证书类型枚举 |
| ackend/src/main/java/com/leyoswimming/common/ErrorCode.java | 修改 | 新增业务错误码 |
| ackend/src/main/java/com/leyoswimming/dto/request/CoachProfileUpdateRequest.java | 新增 | 资料更新请求 |
| ackend/src/main/java/com/leyoswimming/dto/request/CoachReferencePriceUpdateRequest.java | 新增 | 参考单价更新请求 |
| ackend/src/main/java/com/leyoswimming/dto/response/CoachProfileDetailResponse.java | 新增 | 资料详情响应 |
| ackend/src/main/java/com/leyoswimming/dto/response/CoachReferencePriceResponse.java | 新增 | 参考单价响应 |
| ackend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java | 新增 | 证书 Mapper |
| ackend/src/main/java/com/leyoswimming/repository/CoachUpdateLogMapper.java | 新增 | 日志 Mapper |
| ackend/src/main/java/com/leyoswimming/service/CoachProfileService.java | 新增 | 资料业务逻辑 |
| ackend/src/main/java/com/leyoswimming/service/CoachSessionService.java | 修改 | 新增 logout 方法 |
| ackend/src/main/java/com/leyoswimming/controller/coach/CoachProfileController.java | 新增 | 资料与单价接口 |
| ackend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java | 修改 | 新增 logout 端点 |
| ackend/src/main/java/com/leyoswimming/util/SensitiveWordFilter.java | 新增 | 敏感词过滤工具 |
| ackend/src/test/java/com/leyoswimming/service/CoachProfileServiceTest.java | 新增 | 服务层单元测试 |
| ackend/src/test/java/com/leyoswimming/controller/coach/CoachProfileControllerIT.java | 新增 | 控制器集成测试 |
| ackend/src/test/java/com/leyoswimming/controller/coach/CoachAuthControllerIT.java | 修改 | 补充 logout 集成测试 |
| ackend/src/test/java/com/leyoswimming/service/CoachSessionServiceTest.java | 修改 | 补充 logout 单元测试 |

### Frontend

| 文件 | 类型 | 说明 |
|------|------|------|
| miniapp-coach/src/api/profile.ts | 新增 | 教练资料/单价 API |
| miniapp-coach/src/pages/mine/index/index.tsx | 新增 | C-教练中心页 |
| miniapp-coach/src/pages/mine/index/index.scss | 新增 | 教练中心页样式 |
| miniapp-coach/src/pages/profile/index/index.tsx | 新增 | C-个人主页编辑页 |
| miniapp-coach/src/pages/profile/index/index.scss | 新增 | 个人主页编辑页样式 |
| miniapp-coach/src/pages/reference-price/index/index.tsx | 新增 | C-参考单价设置页 |
| miniapp-coach/src/pages/reference-price/index/index.scss | 新增 | 参考单价设置页样式 |
| miniapp-coach/src/app.config.ts | 修改 | 注册新页面 |
| miniapp-coach/src/types/auth.ts | 修改 | 扩展 CoachInfo 类型 |
| miniapp-user/src/pages/mine/index/index.tsx | 新增 | U-个人中心页 |
| miniapp-user/src/pages/mine/index/index.scss | 新增 | 个人中心页样式 |
| miniapp-user/src/app.config.ts | 修改 | 注册 mine 页面 |

---

## Data Flow

### US-012 个人主页与参考单价

1. 教练进入 C-个人主页编辑页，前端调用 POST /api/coach/profile/detail。
2. 后端从 @AuthenticationPrincipal 获取 coachId，查询 coach 表与 coach_certificate 表，组装 CoachProfileDetailResponse。
3. 教练编辑字段并点击保存，前端调用 POST /api/coach/profile/update。
4. 后端校验 coach.status = 1，校验字段格式与敏感词，更新 coach 表；若有形象照/微信二维码 URL 变化，插入/更新 coach_certificate；为实际变更字段写入 coach_update_log。
5. 教练进入 C-参考单价设置页，调用 POST /api/coach/reference-price/update。
6. 后端校验范围 50-2000 与每日改价次数（3），更新 coach.reference_price、price_changed_at、price_change_count_today。
7. 更新成功后失效 Redis 教练详情与列表缓存（如已接入缓存）。

### US-052 / US-056 退出登录

1. 用户在 U-个人中心页 / 教练在 C-教练中心页 点击底部「退出登录」。
2. 前端弹窗确认，调用 POST /api/user/auth/logout 或 POST /api/coach/auth/logout，请求体携带 efreshToken。
3. 后端校验 access_token，删除 user_session / coach_session 中对应记录。
4. 前端无论接口成功与否，清除本地 token，刷新页面为未登录态；用户端停留在本页，教练端跳转回登录页。

---

## Build Sequence

1. **数据库层**：V15 + V16 迁移脚本。
2. **后端实体与错误码**：扩展 Coach、CoachCertificate、CoachUpdateLog、CertificateType、ErrorCode。
3. **后端 DTO 与 Mapper**：请求/响应 DTO、MyBatis-Plus Mapper。
4. **后端业务逻辑**：CoachProfileService（含校验、敏感词、审计日志）、CoachSessionService.logout。
5. **后端控制器**：CoachProfileController、CoachAuthController.logout。
6. **后端测试**：服务单元测试、控制器集成测试。
7. **教练端 API 与状态**：miniapp-coach/src/api/profile.ts、扩展 CoachInfo。
8. **教练端页面**：C-教练中心页、C-个人主页编辑页、C-参考单价设置页。
9. **用户端页面**：U-个人中心页。
10. **前端测试与联调**。


---

## Task 1: 数据库迁移 V15 - 扩展 coach 表字段

**Files:**
- Create: ackend/src/main/resources/db/migration/V15__coach_profile_fields.sql`n
- [ ] **Step 1: 创建 Flyway 迁移脚本**

```sql
-- backend/src/main/resources/db/migration/V15__coach_profile_fields.sql
ALTER TABLE coach
    ADD COLUMN gender TINYINT DEFAULT NULL COMMENT '性别：1-男，2-女',
    ADD COLUMN age INT DEFAULT NULL COMMENT '年龄 18-80',
    ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    ADD COLUMN wechat_qr_url VARCHAR(512) DEFAULT NULL COMMENT '微信二维码 URL',
    ADD COLUMN portrait_url VARCHAR(512) DEFAULT NULL COMMENT '个人形象照 URL',
    ADD COLUMN teaching_years INT DEFAULT 0 COMMENT '任教年限 0-60',
    ADD COLUMN teaching_strokes VARCHAR(64) DEFAULT NULL COMMENT '擅长泳姿，逗号分隔',
    ADD COLUMN bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介 10-500 字符',
    ADD COLUMN reference_price DECIMAL(10, 2) DEFAULT NULL COMMENT '参考单价 50-2000',
    ADD COLUMN price_changed_at DATETIME DEFAULT NULL COMMENT '上次改价时间',
    ADD COLUMN price_change_count_today INT DEFAULT 0 COMMENT '今日改价次数',
    ADD COLUMN price_change_date DATE DEFAULT NULL COMMENT '今日改价统计日期',
    ADD KEY idx_coach_status (status);
```

- [ ] **Step 2: 运行 Flyway 验证**

Run: `cd backend; ./mvnw flyway:info -Dflyway.configFiles=src/main/resources/application-dev.yml`
Expected: V15 状态为 `Pending`，无语法错误。

- [ ] **Step 3: 应用迁移**

Run: `cd backend; ./mvnw flyway:migrate -Dflyway.configFiles=src/main/resources/application-dev.yml`
Expected: 成功应用 V15， coach 表新增字段。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V15__coach_profile_fields.sql
git commit -m "feat(db): V15 add coach profile fields for US-012"
```

---

## Task 2: 数据库迁移 V16 - 证书与变更日志表

**Files:**
- Create: `backend/src/main/resources/db/migration/V16__coach_certificate_and_update_log.sql`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachCertificate.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachUpdateLog.java`
- Create: `backend/src/main/java/com/leyoswimming/enums/CertificateType.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachUpdateLogMapper.java`

- [ ] **Step 1: 创建 V16 迁移脚本**

```sql
-- backend/src/main/resources/db/migration/V16__coach_certificate_and_update_log.sql
CREATE TABLE IF NOT EXISTS coach_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    cert_type VARCHAR(32) NOT NULL COMMENT 'PORTRAIT/WECHAT_QR/ID_CARD_FRONT/ID_CARD_BACK/QUALIFICATION/HEALTH',
    image_url VARCHAR(512) NOT NULL COMMENT '图片 URL',
    display_order INT DEFAULT 0 COMMENT '展示顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_coach_certificate_coach_id (coach_id),
    KEY idx_coach_certificate_type (coach_id, cert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练证书/图片表';

CREATE TABLE IF NOT EXISTS coach_update_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    field_name VARCHAR(32) NOT NULL COMMENT '变更字段',
    old_value TEXT DEFAULT NULL COMMENT '旧值',
    new_value TEXT DEFAULT NULL COMMENT '新值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coach_update_log_coach_id (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练资料变更日志表';
```

- [ ] **Step 2: 应用迁移**

Run: `cd backend; ./mvnw flyway:migrate`
Expected: V16 成功应用。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V16__coach_certificate_and_update_log.sql
git commit -m "feat(db): V16 add coach_certificate and coach_update_log for US-012"
```

---

## Task 3: 后端实体与错误码

**Files:**
- Modify: `backend/src/main/java/com/leyoswimming/entity/Coach.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachCertificate.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachUpdateLog.java`
- Create: `backend/src/main/java/com/leyoswimming/enums/CertificateType.java`
- Modify: `backend/src/main/java/com/leyoswimming/common/ErrorCode.java`

- [ ] **Step 1: 扩展 Coach 实体**

```java
// backend/src/main/java/com/leyoswimming/entity/Coach.java
// 在现有字段后追加
private Integer gender;
private Integer age;
private String email;
private String wechatQrUrl;
private String portraitUrl;
private Integer teachingYears;
private String teachingStrokes;
private String bio;
private BigDecimal referencePrice;
private LocalDateTime priceChangedAt;
private Integer priceChangeCountToday;
private LocalDate priceChangeDate;
```

- [ ] **Step 2: 新增证书与日志实体**

```java
// backend/src/main/java/com/leyoswimming/entity/CoachCertificate.java
package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_certificate")
public class CoachCertificate {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long coachId;
  private String certType;
  private String imageUrl;
  private Integer displayOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

// backend/src/main/java/com/leyoswimming/entity/CoachUpdateLog.java
package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_update_log")
public class CoachUpdateLog {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long coachId;
  private String fieldName;
  private String oldValue;
  private String newValue;
  private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 新增枚举与错误码**

```java
// backend/src/main/java/com/leyoswimming/enums/CertificateType.java
package com.leyoswimming.enums;

public enum CertificateType {
  PORTRAIT, WECHAT_QR, ID_CARD_FRONT, ID_CARD_BACK, QUALIFICATION, HEALTH
}

// backend/src/main/java/com/leyoswimming/common/ErrorCode.java
// 在教练 410xxx 区追加
COACH_STATUS_NOT_ALLOWED(410002, "您的账号状态异常，无法修改资料"),
INVALID_REFERENCE_PRICE(410003, "参考单价需在 50-2000 元之间"),
PRICE_CHANGE_LIMIT(410004, "今日参考单价修改次数已达上限"),
SENSITIVE_CONTENT(410005, "简介包含敏感内容，请修改"),

// 在通用或文件区追加
INVALID_IMAGE_FORMAT(100006, "图片格式仅支持 JPG/PNG"),
IMAGE_TOO_LARGE(100007, "图片大小不能超过 5MB");
```

- [ ] **Step 4: 编译检查**

Run: `cd backend; ./mvnw compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/leyoswimming/entity/Coach.java \
  backend/src/main/java/com/leyoswimming/entity/CoachCertificate.java \
  backend/src/main/java/com/leyoswimming/entity/CoachUpdateLog.java \
  backend/src/main/java/com/leyoswimming/enums/CertificateType.java \
  backend/src/main/java/com/leyoswimming/common/ErrorCode.java
git commit -m "feat(coach): add profile entities and error codes for US-012"
```

---

## Task 4: DTO 与 Mapper

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/dto/request/CoachProfileUpdateRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/CoachReferencePriceUpdateRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/response/CoachProfileDetailResponse.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/response/CoachReferencePriceResponse.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachUpdateLogMapper.java`

- [ ] **Step 1: 创建请求 DTO**

```java
// backend/src/main/java/com/leyoswimming/dto/request/CoachProfileUpdateRequest.java
package com.leyoswimming.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CoachProfileUpdateRequest(
    @Size(min = 1, max = 32, message = "姓名长度需在 1-32 字符之间") String name,
    @Min(value = 1, message = "性别参数错误") @Max(value = 2, message = "性别参数错误") Integer gender,
    @Min(value = 18, message = "年龄需在 18-80 岁之间") @Max(value = 80, message = "年龄需在 18-80 岁之间") Integer age,
    @Email(message = "请输入有效邮箱地址") @Size(max = 128, message = "邮箱长度不能超过 128 字符") String email,
    @Pattern(regexp = "^(https?://.*|)$", message = "微信二维码地址格式错误") String wechatQrUrl,
    @Pattern(regexp = "^(https?://.*|)$", message = "个人形象照地址格式错误") String portraitUrl,
    @Min(value = 0, message = "任教年限需在 0-60 年之间") @Max(value = 60, message = "任教年限需在 0-60 年之间") Integer teachingYears,
    @Size(max = 64, message = "擅长泳姿过长") String teachingStrokes,
    @Size(min = 10, max = 500, message = "个人简介需在 10-500 字符之间") String bio
) {}

// backend/src/main/java/com/leyoswimming/dto/request/CoachReferencePriceUpdateRequest.java
package com.leyoswimming.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CoachReferencePriceUpdateRequest(
    @NotNull(message = "参考单价不能为空")
    @DecimalMin(value = "50.00", message = "参考单价需在 50-2000 元之间")
    @DecimalMax(value = "2000.00", message = "参考单价需在 50-2000 元之间")
    @Digits(integer = 4, fraction = 2, message = "参考单价格式错误")
    BigDecimal referencePrice
) {}
```

- [ ] **Step 2: 创建响应 DTO**

```java
// backend/src/main/java/com/leyoswimming/dto/response/CoachProfileDetailResponse.java
package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachProfileDetailResponse(
    Long coachId,
    String name,
    Integer gender,
    Integer age,
    String email,
    String phone,
    String portraitUrl,
    String wechatQrUrl,
    String idCardNo,
    List<String> idCardFrontUrls,
    List<String> idCardBackUrls,
    List<String> qualificationUrls,
    List<String> healthUrls,
    Integer teachingYears,
    Long totalStudents,
    Long totalLessons,
    String teachingStrokes,
    String bio,
    BigDecimal referencePrice,
    Integer status,
    String statusLabel
) {}

// backend/src/main/java/com/leyoswimming/dto/response/CoachReferencePriceResponse.java
package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CoachReferencePriceResponse(
    BigDecimal referencePrice,
    LocalDateTime priceChangedAt,
    Integer priceChangeCountToday
) {}
```

- [ ] **Step 3: 创建 Mapper**

```java
// backend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java
package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachCertificate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoachCertificateMapper extends BaseMapper<CoachCertificate> {}

// backend/src/main/java/com/leyoswimming/repository/CoachUpdateLogMapper.java
package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachUpdateLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoachUpdateLogMapper extends BaseMapper<CoachUpdateLog> {}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/leyoswimming/dto/request/CoachProfileUpdateRequest.java \
  backend/src/main/java/com/leyoswimming/dto/request/CoachReferencePriceUpdateRequest.java \
  backend/src/main/java/com/leyoswimming/dto/response/CoachProfileDetailResponse.java \
  backend/src/main/java/com/leyoswimming/dto/response/CoachReferencePriceResponse.java \
  backend/src/main/java/com/leyoswimming/repository/CoachCertificateMapper.java \
  backend/src/main/java/com/leyoswimming/repository/CoachUpdateLogMapper.java
git commit -m "feat(coach): add profile DTOs and mappers for US-012"
```

---

## Task 5: CoachProfileService 业务逻辑

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/CoachProfileService.java`
- Create: `backend/src/main/java/com/leyoswimming/util/SensitiveWordFilter.java`

- [ ] **Step 1: 创建敏感词过滤工具**

```java
// backend/src/main/java/com/leyoswimming/util/SensitiveWordFilter.java
package com.leyoswimming.util;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SensitiveWordFilter {
  // MVP 硬编码常见敏感词，后续替换为配置中心或第三方服务
  private static final List<String> SENSITIVE_WORDS = Arrays.asList("暴力", "色情", "赌博", "毒品");

  public boolean containsSensitiveWord(String text) {
    if (text == null || text.isBlank()) return false;
    return SENSITIVE_WORDS.stream().anyMatch(text::contains);
  }
}
```

- [ ] **Step 2: 创建 CoachProfileService**

```java
// backend/src/main/java/com/leyoswimming/service/CoachProfileService.java
package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachProfileUpdateRequest;
import com.leyoswimming.dto.request.CoachReferencePriceUpdateRequest;
import com.leyoswimming.dto.response.CoachProfileDetailResponse;
import com.leyoswimming.dto.response.CoachReferencePriceResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.CoachUpdateLog;
import com.leyoswimming.enums.CertificateType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachUpdateLogMapper;
import com.leyoswimming.util.PhoneEncryptor;
import com.leyoswimming.util.SensitiveWordFilter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachProfileService {

  private static final int MAX_PRICE_CHANGES_PER_DAY = 3;
  private static final String STROKE_SEPARATOR = ",";

  private final CoachMapper coachMapper;
  private final CoachCertificateMapper certificateMapper;
  private final CoachUpdateLogMapper updateLogMapper;
  private final SensitiveWordFilter sensitiveWordFilter;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional(readOnly = true)
  public CoachProfileDetailResponse getDetail(Long coachId) {
    Coach coach = findApprovedCoach(coachId);
    Map<String, List<String>> certs = loadCertificates(coachId);
    return new CoachProfileDetailResponse(
        coach.getId(),
        coach.getName(),
        coach.getGender(),
        coach.getAge(),
        coach.getEmail(),
        maskPhone(coach.getPhone()),
        coach.getPortraitUrl(),
        coach.getWechatQrUrl(),
        maskIdCard(coach.getIdCardNo()),
        certs.getOrDefault(CertificateType.ID_CARD_FRONT.name(), List.of()),
        certs.getOrDefault(CertificateType.ID_CARD_BACK.name(), List.of()),
        certs.getOrDefault(CertificateType.QUALIFICATION.name(), List.of()),
        certs.getOrDefault(CertificateType.HEALTH.name(), List.of()),
        coach.getTeachingYears(),
        countTotalStudents(coachId),
        countTotalLessons(coachId),
        coach.getTeachingStrokes(),
        coach.getBio(),
        coach.getReferencePrice(),
        coach.getStatus(),
        CoachStatus.fromValue(coach.getStatus()).name());
  }

  @Transactional
  public CoachProfileDetailResponse updateProfile(Long coachId, CoachProfileUpdateRequest request) {
    Coach coach = findApprovedCoach(coachId);
    Coach original = snapshot(coach);

    if (sensitiveWordFilter.containsSensitiveWord(request.bio())) {
      throw new BusinessException(ErrorCode.SENSITIVE_CONTENT);
    }

    updateField(coach, "name", request.name());
    updateField(coach, "gender", request.gender());
    updateField(coach, "age", request.age());
    updateField(coach, "email", request.email());
    updateField(coach, "teachingYears", request.teachingYears());
    updateField(coach, "teachingStrokes", request.teachingStrokes());
    updateField(coach, "bio", request.bio());

    if (request.portraitUrl() != null) {
      upsertCertificate(coachId, CertificateType.PORTRAIT, request.portraitUrl());
      coach.setPortraitUrl(request.portraitUrl());
    }
    if (request.wechatQrUrl() != null) {
      upsertCertificate(coachId, CertificateType.WECHAT_QR, request.wechatQrUrl());
      coach.setWechatQrUrl(request.wechatQrUrl());
    }

    coachMapper.updateById(coach);
    logChanges(coachId, original, coach);
    return getDetail(coachId);
  }

  @Transactional
  public CoachReferencePriceResponse updateReferencePrice(
      Long coachId, CoachReferencePriceUpdateRequest request) {
    Coach coach = findApprovedCoach(coachId);
    BigDecimal newPrice = request.referencePrice();

    LocalDate today = LocalDate.now();
    if (!today.equals(coach.getPriceChangeDate())) {
      coach.setPriceChangeCountToday(0);
      coach.setPriceChangeDate(today);
    }
    if (coach.getPriceChangeCountToday() >= MAX_PRICE_CHANGES_PER_DAY) {
      throw new BusinessException(ErrorCode.PRICE_CHANGE_LIMIT);
    }

    coach.setReferencePrice(newPrice);
    coach.setPriceChangedAt(LocalDateTime.now());
    coach.setPriceChangeCountToday(coach.getPriceChangeCountToday() + 1);
    coachMapper.updateById(coach);

    updateLogMapper.insert(
        buildLog(coachId, "referencePrice", coach.getReferencePrice().toString(), newPrice.toString()));

    return new CoachReferencePriceResponse(
        coach.getReferencePrice(), coach.getPriceChangedAt(), coach.getPriceChangeCountToday());
  }

  private Coach findApprovedCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    if (coach.getStatus() != CoachStatus.APPROVED.getValue()
        && coach.getStatus() != CoachStatus.RESIGNING.getValue()) {
      throw new BusinessException(ErrorCode.COACH_STATUS_NOT_ALLOWED);
    }
    return coach;
  }

  private Map<String, List<String>> loadCertificates(Long coachId) {
    List<CoachCertificate> list =
        certificateMapper.selectList(
            new LambdaQueryWrapper<CoachCertificate>()
                .eq(CoachCertificate::getCoachId, coachId));
    return list.stream()
        .collect(Collectors.groupingBy(CoachCertificate::getCertType, Collectors.mapping(CoachCertificate::getImageUrl, Collectors.toList())));
  }

  private void upsertCertificate(Long coachId, CertificateType type, String imageUrl) {
    certificateMapper.delete(
        new LambdaQueryWrapper<CoachCertificate>()
            .eq(CoachCertificate::getCoachId, coachId)
            .eq(CoachCertificate::getCertType, type.name()));
    CoachCertificate cert = new CoachCertificate();
    cert.setCoachId(coachId);
    cert.setCertType(type.name());
    cert.setImageUrl(imageUrl);
    cert.setDisplayOrder(0);
    certificateMapper.insert(cert);
  }

  private void logChanges(Long coachId, Coach original, Coach updated) {
    logIfChanged(coachId, "name", original.getName(), updated.getName());
    logIfChanged(coachId, "gender", original.getGender(), updated.getGender());
    logIfChanged(coachId, "age", original.getAge(), updated.getAge());
    logIfChanged(coachId, "email", original.getEmail(), updated.getEmail());
    logIfChanged(coachId, "teachingYears", original.getTeachingYears(), updated.getTeachingYears());
    logIfChanged(coachId, "teachingStrokes", original.getTeachingStrokes(), updated.getTeachingStrokes());
    logIfChanged(coachId, "bio", original.getBio(), updated.getBio());
  }

  private void logIfChanged(Long coachId, String field, Object oldValue, Object newValue) {
    String oldStr = oldValue == null ? null : oldValue.toString();
    String newStr = newValue == null ? null : newValue.toString();
    if (!java.util.Objects.equals(oldStr, newStr)) {
      updateLogMapper.insert(buildLog(coachId, field, oldStr, newStr));
    }
  }

  private CoachUpdateLog buildLog(Long coachId, String field, String oldValue, String newValue) {
    CoachUpdateLog log = new CoachUpdateLog();
    log.setCoachId(coachId);
    log.setFieldName(field);
    log.setOldValue(oldValue);
    log.setNewValue(newValue);
    return log;
  }

  private Coach snapshot(Coach coach) {
    Coach copy = new Coach();
    copy.setName(coach.getName());
    copy.setGender(coach.getGender());
    copy.setAge(coach.getAge());
    copy.setEmail(coach.getEmail());
    copy.setTeachingYears(coach.getTeachingYears());
    copy.setTeachingStrokes(coach.getTeachingStrokes());
    copy.setBio(coach.getBio());
    return copy;
  }

  private String maskPhone(String encryptedPhone) {
    if (encryptedPhone == null) return null;
    try {
      String plain = phoneEncryptor.decrypt(encryptedPhone);
      return plain.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    } catch (Exception e) {
      return "****";
    }
  }

  private String maskIdCard(String idCardNo) {
    if (idCardNo == null || idCardNo.length() < 8) return idCardNo;
    return idCardNo.substring(0, 4) + "********" + idCardNo.substring(idCardNo.length() - 4);
  }

  private Long countTotalStudents(Long coachId) {
    // MVP 占位，后续接入 package / booking 统计
    return 0L;
  }

  private Long countTotalLessons(Long coachId) {
    // MVP 占位，后续接入 booking 统计
    return 0L;
  }

  private void updateField(Coach coach, String field, Object value) {
    if (value == null) return;
    switch (field) {
      case "name" -> coach.setName((String) value);
      case "gender" -> coach.setGender((Integer) value);
      case "age" -> coach.setAge((Integer) value);
      case "email" -> coach.setEmail((String) value);
      case "teachingYears" -> coach.setTeachingYears((Integer) value);
      case "teachingStrokes" -> coach.setTeachingStrokes((String) value);
      case "bio" -> coach.setBio((String) value);
    }
  }
}
```

- [ ] **Step 3: 编译检查**

Run: `cd backend; ./mvnw compile -q`
Expected: BUILD SUCCESS（如有 CoachStatus.fromValue 不存在，则在该枚举中补一个静态方法）。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/leyoswimming/service/CoachProfileService.java \
  backend/src/main/java/com/leyoswimming/util/SensitiveWordFilter.java
git commit -m "feat(coach): add CoachProfileService with validation and audit log for US-012"
```

---

## Task 6: 教练退出登录后端接口（US-056）

**Files:**
- Modify: `backend/src/main/java/com/leyoswimming/service/CoachSessionService.java`
- Modify: `backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java`

- [ ] **Step 1: 在 CoachSessionService 中新增 logout 方法**

```java
// backend/src/main/java/com/leyoswimming/service/CoachSessionService.java
// 在 refreshAccessToken 方法后追加

@Transactional
public void logout(Long coachId, String refreshToken) {
  String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
  coachSessionMapper.delete(
      new LambdaQueryWrapper<CoachSession>()
          .eq(CoachSession::getCoachId, coachId)
          .eq(CoachSession::getRefreshTokenHash, hash));
}
```

- [ ] **Step 2: 在 CoachAuthController 中新增 logout 端点**

```java
// backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java
// 在 refresh 方法后追加

@PostMapping("/logout")
public ApiResponse<Void> logout(
    @AuthenticationPrincipal Long coachId, @Valid @RequestBody RefreshTokenRequest request) {
  coachSessionService.logout(coachId, request.refreshToken());
  return ApiResponse.ok(null);
}
```

- [ ] **Step 3: 验证 SecurityConfig 放行规则**

`SecurityConfig.java` 中已存在：
```java
auth.requestMatchers("/api/user/auth/logout", "/api/coach/auth/logout")
    .authenticated()
```
无需修改。

- [ ] **Step 4: 编译与测试**

Run: `cd backend; ./mvnw test -Dtest=CoachAuthControllerIT#coachLogout -q`
Expected: 测试通过（测试用例见 Task 8）。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/leyoswimming/service/CoachSessionService.java \
  backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java
git commit -m "feat(auth): add coach logout endpoint for US-056"
```

---

## Task 7: CoachProfileController 资料与参考单价接口

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/controller/coach/CoachProfileController.java`

- [ ] **Step 1: 创建 Controller**

```java
// backend/src/main/java/com/leyoswimming/controller/coach/CoachProfileController.java
package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachProfileUpdateRequest;
import com.leyoswimming.dto.request.CoachReferencePriceUpdateRequest;
import com.leyoswimming.dto.response.CoachProfileDetailResponse;
import com.leyoswimming.dto.response.CoachReferencePriceResponse;
import com.leyoswimming.service.CoachProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/profile")
@RequiredArgsConstructor
public class CoachProfileController {

  private final CoachProfileService coachProfileService;

  @PostMapping("/detail")
  public ApiResponse<CoachProfileDetailResponse> detail(@AuthenticationPrincipal Long coachId) {
    return ApiResponse.ok(coachProfileService.getDetail(coachId));
  }

  @PostMapping("/update")
  public ApiResponse<CoachProfileDetailResponse> update(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachProfileUpdateRequest request) {
    return ApiResponse.ok(coachProfileService.updateProfile(coachId, request));
  }
}

// backend/src/main/java/com/leyoswimming/controller/coach/CoachReferencePriceController.java
package com.leyoswimming.controller.coach;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.CoachReferencePriceUpdateRequest;
import com.leyoswimming.dto.response.CoachReferencePriceResponse;
import com.leyoswimming.service.CoachProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach/reference-price")
@RequiredArgsConstructor
public class CoachReferencePriceController {

  private final CoachProfileService coachProfileService;

  @PostMapping("/update")
  public ApiResponse<CoachReferencePriceResponse> update(
      @AuthenticationPrincipal Long coachId,
      @Valid @RequestBody CoachReferencePriceUpdateRequest request) {
    return ApiResponse.ok(coachProfileService.updateReferencePrice(coachId, request));
  }
}
```

- [ ] **Step 2: 编译检查**

Run: `cd backend; ./mvnw compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/leyoswimming/controller/coach/CoachProfileController.java \
  backend/src/main/java/com/leyoswimming/controller/coach/CoachReferencePriceController.java
git commit -m "feat(coach): add profile and reference price controllers for US-012"
```

---

## Task 8: 后端测试

**Files:**
- Create: `backend/src/test/java/com/leyoswimming/service/CoachProfileServiceTest.java`
- Create: `backend/src/test/java/com/leyoswimming/controller/coach/CoachProfileControllerIT.java`
- Create: `backend/src/test/java/com/leyoswimming/controller/coach/CoachAuthControllerIT.java`（若不存在）
- Modify: `backend/src/test/java/com/leyoswimming/service/CoachSessionServiceTest.java`（若不存在则创建）

- [ ] **Step 1: CoachProfileService 单元测试**

```java
// backend/src/test/java/com/leyoswimming/service/CoachProfileServiceTest.java
package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachProfileUpdateRequest;
import com.leyoswimming.dto.request.CoachReferencePriceUpdateRequest;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachUpdateLogMapper;
import com.leyoswimming.util.PhoneEncryptor;
import com.leyoswimming.util.SensitiveWordFilter;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachProfileServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private CoachCertificateMapper certificateMapper;
  @Mock private CoachUpdateLogMapper updateLogMapper;
  @Mock private SensitiveWordFilter sensitiveWordFilter;
  @Mock private PhoneEncryptor phoneEncryptor;

  private CoachProfileService service;

  @BeforeEach
  void setUp() {
    service = new CoachProfileService(coachMapper, certificateMapper, updateLogMapper, sensitiveWordFilter, phoneEncryptor);
  }

  @Test
  void updateProfile_success_updatesFields() {
    Coach coach = activeCoach();
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(sensitiveWordFilter.containsSensitiveWord(any())).thenReturn(false);

    CoachProfileUpdateRequest request = new CoachProfileUpdateRequest(
        "张教练", 1, 30, "coach@example.com", null, null, 5, "蛙泳", "专注儿童游泳教学 5 年");

    service.updateProfile(1L, request);

    verify(coachMapper).updateById(coach);
    assertThat(coach.getName()).isEqualTo("张教练");
  }

  @Test
  void updateProfile_sensitiveBio_throws() {
    Coach coach = activeCoach();
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(sensitiveWordFilter.containsSensitiveWord("敏感内容")).thenReturn(true);

    CoachProfileUpdateRequest request = new CoachProfileUpdateRequest(
        null, null, null, null, null, null, null, null, "敏感内容");

    assertThatThrownBy(() -> service.updateProfile(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SENSITIVE_CONTENT);
  }

  @Test
  void updateReferencePrice_outOfRange_throws() {
    Coach coach = activeCoach();
    when(coachMapper.selectById(1L)).thenReturn(coach);

    CoachReferencePriceUpdateRequest request = new CoachReferencePriceUpdateRequest(BigDecimal.valueOf(5000));

    assertThatThrownBy(() -> service.updateReferencePrice(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REFERENCE_PRICE);
  }

  private Coach activeCoach() {
    Coach coach = new Coach();
    coach.setId(1L);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    return coach;
  }
}
```

- [ ] **Step 2: CoachProfileController 集成测试**

```java
// backend/src/test/java/com/leyoswimming/controller/coach/CoachProfileControllerIT.java
package com.leyoswimming.controller.coach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leyoswimming.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoachProfileControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  void detail_withValidToken_returnsOk() throws Exception {
    String token = jwtTokenProvider.generateCoachAccessToken(1L, 1);
    mockMvc.perform(post("/api/coach/profile/detail")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }
}
```

- [ ] **Step 3: CoachAuthController 集成测试补充 logout**

```java
// backend/src/test/java/com/leyoswimming/controller/coach/CoachAuthControllerIT.java
// 新增测试方法
@Test
void logout_withValidToken_deletesSession() throws Exception {
  String token = jwtTokenProvider.generateCoachAccessToken(1L, 1);
  mockMvc.perform(post("/api/coach/auth/logout")
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"refreshToken\":\"dummy_refresh_token\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(0));
}
```

- [ ] **Step 4: 运行测试**

Run: `cd backend; ./mvnw test -Dtest=CoachProfileServiceTest,CoachProfileControllerIT,CoachAuthControllerIT -q`
Expected: 所有测试通过。

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/leyoswimming/service/CoachProfileServiceTest.java \
  backend/src/test/java/com/leyoswimming/controller/coach/CoachProfileControllerIT.java \
  backend/src/test/java/com/leyoswimming/controller/coach/CoachAuthControllerIT.java
git commit -m "test(coach): add profile and logout tests for US-012/US-056"
```

---

## Task 9: 教练端 API 与类型扩展

**Files:**
- Create: `miniapp-coach/src/api/profile.ts`
- Modify: `miniapp-coach/src/types/auth.ts`
- Modify: `miniapp-coach/src/api/auth.ts`（补传 refreshToken）

- [ ] **Step 1: 扩展 CoachInfo 类型**

```typescript
// miniapp-coach/src/types/auth.ts
export type CoachStatus = -1 | 0 | 1 | 2 | 3 | 4

export interface CoachInfo {
  id: number
  name?: string
  phone?: string
  avatar?: string
  status: CoachStatus
  gender?: 1 | 2
  age?: number
  email?: string
  wechatQrUrl?: string
  portraitUrl?: string
  teachingYears?: number
  teachingStrokes?: string
  bio?: string
  referencePrice?: number
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  isNewCoach: boolean
  coachStatus: CoachStatus
  coachId: number
}
```

- [ ] **Step 2: 创建教练资料 API**

```typescript
// miniapp-coach/src/api/profile.ts
import { request } from './request'
import type { CoachInfo } from '@/types/auth'

export interface CoachProfileDetail {
  coachId: number
  name: string
  gender: 1 | 2 | null
  age: number | null
  email: string | null
  phone: string | null
  portraitUrl: string | null
  wechatQrUrl: string | null
  idCardNo: string | null
  idCardFrontUrls: string[]
  idCardBackUrls: string[]
  qualificationUrls: string[]
  healthUrls: string[]
  teachingYears: number
  totalStudents: number
  totalLessons: number
  teachingStrokes: string | null
  bio: string | null
  referencePrice: number | null
  status: CoachStatus
  statusLabel: string
}

export interface ReferencePriceInfo {
  referencePrice: number
  priceChangedAt: string
  priceChangeCountToday: number
}

export function getCoachProfileDetail(): Promise<CoachProfileDetail> {
  return request<CoachProfileDetail>({
    url: '/coach/profile/detail',
    method: 'POST'
  })
}

export interface UpdateCoachProfileParams {
  name?: string
  gender?: 1 | 2
  age?: number
  email?: string
  wechatQrUrl?: string
  portraitUrl?: string
  teachingYears?: number
  teachingStrokes?: string
  bio?: string
}

export function updateCoachProfile(params: UpdateCoachProfileParams): Promise<CoachProfileDetail> {
  return request<CoachProfileDetail>({
    url: '/coach/profile/update',
    method: 'POST',
    data: params
  })
}

export function updateReferencePrice(price: number): Promise<ReferencePriceInfo> {
  return request<ReferencePriceInfo>({
    url: '/coach/reference-price/update',
    method: 'POST',
    data: { referencePrice: price }
  })
}
```

- [ ] **Step 3: 修正 logoutCoach 传 refreshToken**

```typescript
// miniapp-coach/src/api/auth.ts
import { storage } from '@/utils/storage'
import { STORAGE_KEYS } from '@/constants'

export function logoutCoach(): Promise<void> {
  const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN) || ''
  return request<void>({
    url: '/coach/auth/logout',
    method: 'POST',
    data: { refreshToken }
  })
}
```

- [ ] **Step 4: Commit**

```bash
git add miniapp-coach/src/api/profile.ts miniapp-coach/src/types/auth.ts miniapp-coach/src/api/auth.ts
git commit -m "feat(miniapp-coach): add profile API types and fix logout refreshToken for US-012/US-056"
```

---

## Task 10: 教练端个人主页编辑页（C-个人主页编辑页）

**Files:**
- Create: `miniapp-coach/src/pages/profile/index/index.tsx`
- Create: `miniapp-coach/src/pages/profile/index/index.scss`
- Modify: `miniapp-coach/src/app.config.ts`

- [ ] **Step 1: 注册页面**

```typescript
// miniapp-coach/src/app.config.ts
export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/mine/index/index',
    'pages/profile/index/index',
    'pages/reference-price/index/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'leyoSwimming 教练端',
    navigationBarTextStyle: 'black'
  }
})
```

- [ ] **Step 2: 创建页面组件（简化骨架）**

```tsx
// miniapp-coach/src/pages/profile/index/index.tsx
import { useEffect, useState } from 'react'
import { View, Button, Input, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { getCoachProfileDetail, updateCoachProfile, type CoachProfileDetail, type UpdateCoachProfileParams } from '@/api/profile'
import { uploadFile } from '@/api/common'
import { useAuthStore } from '@/stores/authStore'
import './index.scss'

const STROKES = ['蛙泳', '自由泳', '仰泳', '蝶泳']

export default function ProfileEditPage() {
  const { coachInfo } = useAuthStore()
  const [detail, setDetail] = useState<CoachProfileDetail | null>(null)
  const [form, setForm] = useState<UpdateCoachProfileParams>({})
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setLoading(true)
    getCoachProfileDetail()
      .then((data) => {
        setDetail(data)
        setForm({
          name: data.name,
          gender: data.gender ?? undefined,
          age: data.age ?? undefined,
          email: data.email ?? undefined,
          wechatQrUrl: data.wechatQrUrl ?? undefined,
          portraitUrl: data.portraitUrl ?? undefined,
          teachingYears: data.teachingYears ?? undefined,
          teachingStrokes: data.teachingStrokes ?? undefined,
          bio: data.bio ?? undefined
        })
      })
      .catch((err) => Taro.showToast({ title: err.message || '加载失败', icon: 'none' }))
      .finally(() => setLoading(false))
  }, [])

  const handleUpload = async (type: 'portrait' | 'wechatQr') => {
    const res = await Taro.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })
    const filePath = res.tempFilePaths[0]
    const size = res.tempFiles[0]?.size ?? 0
    if (size > 5 * 1024 * 1024) {
      Taro.showToast({ title: '图片不能超过 5MB', icon: 'none' })
      return
    }
    const url = await uploadFile(filePath)
    setForm((prev) => ({ ...prev, [type === 'portrait' ? 'portraitUrl' : 'wechatQrUrl']: url }))
  }

  const toggleStroke = (stroke: string) => {
    const current = form.teachingStrokes ? form.teachingStrokes.split(',') : []
    const next = current.includes(stroke) ? current.filter((s) => s !== stroke) : [...current, stroke]
    setForm((prev) => ({ ...prev, teachingStrokes: next.join(',') }))
  }

  const validate = (): string | null => {
    if (!form.name || form.name.length < 1 || form.name.length > 32) return '姓名长度需在 1-32 字符之间'
    if (form.age !== undefined && (form.age < 18 || form.age > 80)) return '年龄需在 18-80 岁之间'
    if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) return '请输入有效邮箱地址'
    if (form.teachingYears !== undefined && (form.teachingYears < 0 || form.teachingYears > 60)) return '任教年限需在 0-60 年之间'
    if (form.bio && (form.bio.length < 10 || form.bio.length > 500)) return '个人简介需在 10-500 字符之间'
    return null
  }

  const handleSave = async () => {
    const error = validate()
    if (error) {
      Taro.showToast({ title: error, icon: 'none' })
      return
    }
    setSaving(true)
    try {
      await updateCoachProfile(form)
      Taro.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => Taro.navigateBack(), 1000)
    } catch (err) {
      Taro.showToast({ title: (err as Error).message || '保存失败', icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  if (loading || !detail) return <View className="profile-edit-page">加载中...</View>

  return (
    <View className="profile-edit-page">
      <View className="header-preview">
        <Image className="avatar" src={form.portraitUrl || ''} />
        <Text className="name">{form.name}</Text>
        <Text className="years">{form.teachingYears} 年教龄</Text>
      </View>

      <View className="form-group">
        <View className="form-item" onClick={() => handleUpload('portrait')}>
          <Text>个人形象照</Text>
          <Image className="thumb" src={form.portraitUrl || ''} />
        </View>
        <View className="form-item">
          <Text>姓名/昵称</Text>
          <Input value={form.name} onInput={(e) => setForm((p) => ({ ...p, name: e.detail.value }))} placeholder="1-32 字符" />
        </View>
        <View className="form-item">
          <Text>任教年限</Text>
          <Input type="number" value={String(form.teachingYears ?? '')} onInput={(e) => setForm((p) => ({ ...p, teachingYears: Number(e.detail.value) }))} placeholder="0-60" />
        </View>
      </View>

      <View className="form-group">
        <View className="form-item">
          <Text>性别</Text>
          <View className="radio-group">
            <Text className={form.gender === 1 ? 'active' : ''} onClick={() => setForm((p) => ({ ...p, gender: 1 }))}>男</Text>
            <Text className={form.gender === 2 ? 'active' : ''} onClick={() => setForm((p) => ({ ...p, gender: 2 }))}>女</Text>
          </View>
        </View>
        <View className="form-item">
          <Text>年龄</Text>
          <Input type="number" value={String(form.age ?? '')} onInput={(e) => setForm((p) => ({ ...p, age: Number(e.detail.value) }))} placeholder="18-80" />
        </View>
        <View className="form-item">
          <Text>邮箱</Text>
          <Input value={form.email || ''} onInput={(e) => setForm((p) => ({ ...p, email: e.detail.value }))} placeholder="有效邮箱" />
        </View>
        <View className="form-item">
          <Text>手机号</Text>
          <Text>{detail.phone}</Text>
        </View>
        <View className="form-item" onClick={() => handleUpload('wechatQr')}>
          <Text>微信二维码</Text>
          <Image className="thumb" src={form.wechatQrUrl || ''} />
        </View>
      </View>

      <View className="form-group">
        <View className="form-item">
          <Text>擅长泳姿</Text>
          <View className="stroke-tags">
            {STROKES.map((s) => (
              <Text key={s} className={form.teachingStrokes?.includes(s) ? 'active' : ''} onClick={() => toggleStroke(s)}>
                {s}
              </Text>
            ))}
          </View>
        </View>
        <View className="form-item textarea">
          <Text>个人简介</Text>
          <Input type="textarea" value={form.bio || ''} onInput={(e) => setForm((p) => ({ ...p, bio: e.detail.value }))} placeholder="10-500 字符" />
          <Text className="counter">{(form.bio || '').length}/500</Text>
        </View>
      </View>

      <Button className="save-btn" loading={saving} disabled={saving} onClick={handleSave}>
        {saving ? '保存中...' : '保存'}
      </Button>
    </View>
  )
}
```

- [ ] **Step 3: 创建 SCSS（按 Calicat 图层实现，占位）**

```scss
// miniapp-coach/src/pages/profile/index/index.scss
.profile-edit-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 48px;
}
.header-preview {
  height: 360px;
  background: linear-gradient(180deg, #1890ff 0%, #0050b3 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .avatar {
    width: 160px;
    height: 160px;
    border-radius: 50%;
    border: 6px solid #fff;
  }
  .name {
    margin-top: 16px;
    font-size: 40px;
    font-weight: bold;
    color: #fff;
  }
  .years {
    margin-top: 8px;
    font-size: 28px;
    color: rgba(255, 255, 255, 0.8);
  }
}
.form-group {
  margin: 24px 32px;
  background: #fff;
  border-radius: 24px;
  padding: 0 24px;
}
.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 112px;
  border-bottom: 1px solid #f0f0f0;
}
.save-btn {
  margin: 64px 32px 0;
  height: 96px;
  line-height: 96px;
  background: linear-gradient(90deg, #1890ff, #0050b3);
  color: #fff;
  border-radius: 16px;
  font-size: 32px;
}
```

- [ ] **Step 4: 编译检查**

Run: `cd miniapp-coach; npm run build:weapp`
Expected: 编译成功，无 TypeScript 错误。

- [ ] **Step 5: Commit**

```bash
git add miniapp-coach/src/pages/profile/index/index.tsx \
  miniapp-coach/src/pages/profile/index/index.scss \
  miniapp-coach/src/app.config.ts
git commit -m "feat(miniapp-coach): add C-profile-edit-page for US-012"
```

---

## Task 11: 教练端参考单价设置页（C-参考单价设置页）

**Files:**
- Create: `miniapp-coach/src/pages/reference-price/index/index.tsx`
- Create: `miniapp-coach/src/pages/reference-price/index/index.scss`
- Modify: `miniapp-coach/src/app.config.ts`（已在 Task 10 注册，需确认路径存在）

- [ ] **Step 1: 创建参考单价设置页**

```tsx
// miniapp-coach/src/pages/reference-price/index/index.tsx
import { useEffect, useState } from 'react'
import { View, Button, Input, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { getCoachProfileDetail, updateReferencePrice, type ReferencePriceInfo } from '@/api/profile'
import './index.scss'

const MIN_PRICE = 50
const MAX_PRICE = 2000
const EXAMPLES = [1, 6, 10]

export default function ReferencePricePage() {
  const [price, setPrice] = useState<string>('')
  const [info, setInfo] = useState<ReferencePriceInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setLoading(true)
    getCoachProfileDetail()
      .then((detail) => {
        if (detail.referencePrice != null) {
          setPrice(String(detail.referencePrice))
        }
      })
      .finally(() => setLoading(false))
  }, [])

  const numericPrice = Number(price)
  const valid = !Number.isNaN(numericPrice) && numericPrice >= MIN_PRICE && numericPrice <= MAX_PRICE

  const handleSave = async () => {
    if (!valid) {
      Taro.showToast({ title: `参考单价需在 ${MIN_PRICE}-${MAX_PRICE} 元之间`, icon: 'none' })
      return
    }
    setSaving(true)
    try {
      const result = await updateReferencePrice(numericPrice)
      setInfo(result)
      Taro.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => Taro.navigateBack(), 1000)
    } catch (err) {
      Taro.showToast({ title: (err as Error).message || '保存失败', icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <View className="reference-price-page">加载中...</View>

  const remaining = info ? 3 - info.priceChangeCountToday : 3

  return (
    <View className="reference-price-page">
      <View className="tip-card">
        <Text className="tip-title">参考单价说明</Text>
        <Text className="tip-body">作为系统套餐定价和自定义套餐金额计算的基准。修改后不影响已购套餐。</Text>
      </View>

      <View className="price-card">
        <Text className="label">每节课参考单价</Text>
        <View className="input-wrap">
          <Input
            type="digit"
            value={price}
            onInput={(e) => setPrice(e.detail.value)}
            placeholder={`请输入 ${MIN_PRICE}-${MAX_PRICE}`}
          />
          <Text className="unit">元/节</Text>
        </View>
        <Text className="hint">平台建议范围 {MIN_PRICE}-{MAX_PRICE} 元/节</Text>
        <Text className="limit">今日还可修改 {Math.max(0, remaining)} 次</Text>
      </View>

      <View className="preview-card">
        <Text className="label">基于当前单价的新套餐示例</Text>
        {EXAMPLES.map((count) => (
          <View key={count} className="preview-row">
            <Text>{count} 节标准课</Text>
            <Text className="price">{(numericPrice * count).toFixed(2)} 元</Text>
          </View>
        ))}
      </View>

      <Button className="save-btn" disabled={!valid || saving} loading={saving} onClick={handleSave}>
        {saving ? '保存中...' : '保存'}
      </Button>
    </View>
  )
}
```

- [ ] **Step 2: 创建样式（按 Calicat 图层实现，占位）**

```scss
// miniapp-coach/src/pages/reference-price/index/index.scss
.reference-price-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 16px 48px;
}
.tip-card {
  margin-bottom: 24px;
  padding: 24px;
  background: #e6f7ff;
  border-radius: 24px;
  .tip-title {
    display: block;
    font-size: 28px;
    font-weight: bold;
    color: #0050b3;
    margin-bottom: 12px;
  }
  .tip-body {
    font-size: 24px;
    color: #262626;
    line-height: 1.5;
  }
}
.price-card,
.preview-card {
  margin-bottom: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  .label {
    display: block;
    font-size: 28px;
    font-weight: bold;
    color: #262626;
    margin-bottom: 16px;
  }
  .input-wrap {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f0f0f0;
    padding-bottom: 16px;
    input {
      flex: 1;
      font-size: 48px;
      color: #262626;
    }
    .unit {
      font-size: 28px;
      color: #8c8c8c;
    }
  }
  .hint,
  .limit {
    display: block;
    margin-top: 16px;
    font-size: 24px;
  }
  .hint { color: #8c8c8c; }
  .limit { color: #faad14; }
}
.preview-row {
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
  font-size: 28px;
  color: #262626;
  .price {
    font-weight: bold;
  }
}
.save-btn {
  margin-top: 48px;
  height: 96px;
  line-height: 96px;
  background: linear-gradient(90deg, #1890ff, #0050b3);
  color: #fff;
  border-radius: 16px;
  font-size: 32px;
  &[disabled] {
    opacity: 0.5;
  }
}
```

- [ ] **Step 3: 编译检查**

Run: `cd miniapp-coach; npm run build:weapp`
Expected: 编译成功，无 TypeScript 错误。

- [ ] **Step 4: Commit**

```bash
git add miniapp-coach/src/pages/reference-price/index/index.tsx \
  miniapp-coach/src/pages/reference-price/index/index.scss
# app.config.ts 已在 Task 10 提交，如未变更无需重复 add
git commit -m "feat(miniapp-coach): add C-reference-price-page for US-012"
```

---

## Task 12: 教练端「我的」页面与退出登录（C-教练中心页 / US-056）

**Files:**
- Create: `miniapp-coach/src/pages/mine/index/index.tsx`
- Create: `miniapp-coach/src/pages/mine/index/index.scss`
- Modify: `miniapp-coach/src/app.config.ts`（已在 Task 10 注册 `pages/mine/index/index`）

- [ ] **Step 1: 创建教练中心页**

```tsx
// miniapp-coach/src/pages/mine/index/index.tsx
import { useEffect, useState } from 'react'
import { View, Image, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { getCoachProfileDetail, type CoachProfileDetail } from '@/api/profile'
import { logoutCoach } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import './index.scss'

const STATUS_LABEL: Record<number, string> = {
  [-1]: '未提交入驻',
  0: '资料审核中',
  1: '在职',
  2: '入驻被驳回',
  3: '已离职',
  4: '离职申请中'
}

const STATUS_COLOR: Record<number, string> = {
  [-1]: '#8c8c8c',
  0: '#faad14',
  1: '#52c41a',
  2: '#ff4d4f',
  3: '#ff4d4f',
  4: '#faad14'
}

export default function MinePage() {
  const { coachInfo, logout } = useAuthStore()
  const [detail, setDetail] = useState<CoachProfileDetail | null>(null)
  const [loading, setLoading] = useState(false)

  const load = () => {
    setLoading(true)
    getCoachProfileDetail()
      .then(setDetail)
      .catch((err) => Taro.showToast({ title: err.message || '加载失败', icon: 'none' }))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  useDidShow(() => {
    load()
  })

  const handleLogout = () => {
    Taro.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await logoutCoach()
          } catch {
            // 后端失败仍继续清除本地态
          } finally {
            logout()
            Taro.reLaunch({ url: '/pages/login/wechat/index' })
          }
        }
      }
    })
  }

  const status = detail?.status ?? coachInfo?.status ?? -1

  return (
    <View className="mine-page">
      <View className="header-card" onClick={() => Taro.navigateTo({ url: '/pages/profile/index/index' })}>
        <Image className="avatar" src={detail?.portraitUrl || coachInfo?.avatar || ''} />
        <View className="meta">
          <Text className="name">{detail?.name || coachInfo?.name || '未设置昵称'}</Text>
          <Text className="status-tag" style={{ color: STATUS_COLOR[status], borderColor: STATUS_COLOR[status] }}>
            {STATUS_LABEL[status]}
          </Text>
        </View>
      </View>

      <View className="menu-card">
        {(status === 1 || status === 4) && (
          <>
            <View className="menu-item" onClick={() => Taro.navigateTo({ url: '/pages/profile/index/index' })}>
              <Text>个人主页编辑</Text>
              <Text className="arrow"></Text>
            </View>
            <View className="menu-item" onClick={() => Taro.navigateTo({ url: '/pages/reference-price/index/index' })}>
              <Text>参考单价设置</Text>
              <Text className="arrow"></Text>
            </View>
          </>
        )}
      </View>

      <Button className="logout-btn" onClick={handleLogout}>退出登录</Button>
    </View>
  )
}
```

- [ ] **Step 2: 创建样式（占位，按 Calicat 图层数据替换）**

```scss
// miniapp-coach/src/pages/mine/index/index.scss
.mine-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 16px 48px;
}
.header-card {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  .avatar {
    width: 128px;
    height: 128px;
    border-radius: 50%;
    background: #e6f7ff;
    margin-right: 24px;
  }
  .meta {
    flex: 1;
    .name {
      display: block;
      font-size: 40px;
      color: #262626;
      margin-bottom: 12px;
    }
    .status-tag {
      display: inline-block;
      padding: 4px 16px;
      font-size: 24px;
      border: 1px solid;
      border-radius: 999px;
    }
  }
}
.menu-card {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 112px;
    padding: 0 24px;
    border-bottom: 1px solid #f0f0f0;
    font-size: 32px;
    color: #262626;
    &:last-child { border-bottom: none; }
    .arrow {
      font-size: 40px;
      color: #bfbfbf;
    }
  }
}
.logout-btn {
  margin-top: 48px;
  height: 96px;
  line-height: 96px;
  background: #fff;
  color: #ff4d4f;
  border-radius: 16px;
  font-size: 32px;
}
```

- [ ] **Step 3: 编译检查**

Run: `cd miniapp-coach; npm run build:weapp`
Expected: 编译成功，无 TypeScript 错误。

- [ ] **Step 4: Commit**

```bash
git add miniapp-coach/src/pages/mine/index/index.tsx \
  miniapp-coach/src/pages/mine/index/index.scss
git commit -m "feat(miniapp-coach): add C-coach-center-page with logout for US-056"
```

---


## Task 13: 用户端「我的」页面与退出登录（U-个人中心页 / US-052）

**Files:**
- Create: `miniapp-user/src/pages/mine/index/index.tsx`
- Create: `miniapp-user/src/pages/mine/index/index.scss`
- Modify: `miniapp-user/src/app.config.ts`
- Modify: `miniapp-user/src/api/auth.ts`（退出接口需携带 refreshToken）

- [ ] **Step 1: 注册 mine 页面**

```typescript
// miniapp-user/src/app.config.ts
export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/login/protocol/index',
    'pages/profile/complete/index',
    'pages/mine/index/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'leyoSwimming',
    navigationBarTextStyle: 'black'
  }
})
```

- [ ] **Step 2: 修正用户端 logout 接口传 refreshToken**

```typescript
// miniapp-user/src/api/auth.ts
import { getStorageItem, STORAGE_KEYS } from '@/utils/storage'

export function logout() {
  const refreshToken = getStorageItem<string>(STORAGE_KEYS.REFRESH_TOKEN) || ''
  return request<unknown>({
    url: '/user/auth/logout',
    method: 'POST',
    data: { refreshToken },
  })
}
```

- [ ] **Step 3: 创建用户个人中心页**

```tsx
// miniapp-user/src/pages/mine/index/index.tsx
import { useEffect, useState } from 'react'
import { View, Image, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { logout } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import './index.scss'

const MENU_ITEMS = [
  { label: '我的套餐', url: '/pages/package/list/index' },
  { label: '我的订单', url: '/pages/order/list/index' },
  { label: '我的预约', url: '/pages/appointment/list/index' },
  { label: '隐私协议', url: '/pages/privacy/index' },
  { label: '设置', url: '/pages/settings/index' }
]

export default function MinePage() {
  const { userInfo, isLoggedIn, logout: clearAuth } = useAuthStore()
  const [loggingOut, setLoggingOut] = useState(false)

  useDidShow(() => {
    // 页面展示时自动根据 storage 刷新登录态
  })

  const handleLogin = () => {
    Taro.navigateTo({ url: '/pages/login/wechat/index' })
  }

  const handleLogout = () => {
    Taro.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: async (res) => {
        if (res.confirm) {
          setLoggingOut(true)
          try {
            if (isLoggedIn) await logout()
          } catch {
            // 后端失败仍清除本地态
          } finally {
            clearAuth()
            setLoggingOut(false)
            // 停留在本页，重新渲染为游客态
          }
        }
      }
    })
  }

  const handleMenu = (url: string) => {
    if (!isLoggedIn) {
      Taro.navigateTo({ url: '/pages/login/wechat/index' })
      return
    }
    Taro.navigateTo({ url })
  }

  return (
    <View className="u-mine-page">
      <View className="header-card" onClick={isLoggedIn ? undefined : handleLogin}>
        <Image className="avatar" src={userInfo?.avatarUrl || ''} />
        <View className="meta">
          {isLoggedIn ? (
            <>
              <Text className="name">{userInfo?.nickName || '用户'}</Text>
              <Text className="sub">{userInfo?.phone || ''}</Text>
            </>
          ) : (
            <Text className="name">请登录/注册</Text>
          )}
        </View>
      </View>

      <View className="menu-card">
        {MENU_ITEMS.map((item) => (
          <View key={item.label} className="menu-item" onClick={() => handleMenu(item.url)}>
            <Text>{item.label}</Text>
            <Text className="arrow"></Text>
          </View>
        ))}
      </View>

      {isLoggedIn && (
        <Button className="logout-btn" loading={loggingOut} disabled={loggingOut} onClick={handleLogout}>
          {loggingOut ? '退出中...' : '退出登录'}
        </Button>
      )}
    </View>
  )
}
```

- [ ] **Step 4: 创建样式（占位，按 Calicat 图层数据替换）**

```scss
// miniapp-user/src/pages/mine/index/index.scss
.u-mine-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 16px 48px;
}
.header-card {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  .avatar {
    width: 128px;
    height: 128px;
    border-radius: 50%;
    background: #e6f7ff;
    margin-right: 24px;
  }
  .meta {
    flex: 1;
    .name {
      display: block;
      font-size: 40px;
      color: #262626;
      margin-bottom: 8px;
    }
    .sub {
      font-size: 24px;
      color: #8c8c8c;
    }
  }
}
.menu-card {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 112px;
    padding: 0 24px;
    border-bottom: 1px solid #f0f0f0;
    font-size: 32px;
    color: #262626;
    &:last-child { border-bottom: none; }
    .arrow {
      font-size: 40px;
      color: #bfbfbf;
    }
  }
}
.logout-btn {
  margin-top: 48px;
  height: 96px;
  line-height: 96px;
  background: #fff;
  color: #ff4d4f;
  border-radius: 16px;
  font-size: 32px;
}
```

- [ ] **Step 5: 编译检查**

Run: `cd miniapp-user; npm run build:weapp`
Expected: 编译成功，无 TypeScript 错误。

- [ ] **Step 6: Commit**

```bash
git add miniapp-user/src/pages/mine/index/index.tsx \
  miniapp-user/src/pages/mine/index/index.scss \
  miniapp-user/src/app.config.ts \
  miniapp-user/src/api/auth.ts
git commit -m "feat(miniapp-user): add U-profile-center-page with logout for US-052"
```

---

## Task 14: 联调、测试与验收

- [ ] **Step 1: 后端单元与集成测试**

Run:
```bash
cd backend
./mvnw test -Dtest=CoachProfileServiceTest,CoachProfileControllerIT,CoachAuthControllerIT -q
```
Expected: BUILD SUCCESS，所有测试通过。

- [ ] **Step 2: 后端代码质量检查**

Run:
```bash
cd backend
./mvnw compile -q
./mvnw spotbugs:check || true
```
Expected: 编译通过；SpotBugs 无新增高危问题。

- [ ] **Step 3: 教练端小程序构建与测试**

Run:
```bash
cd miniapp-coach
npm run build:weapp
npm run test:unit -- --run
```
Expected: 构建成功、测试通过。

- [ ] **Step 4: 用户端小程序构建与测试**

Run:
```bash
cd miniapp-user
npm run build:weapp
npm run test:unit -- --run
```
Expected: 构建成功、测试通过。

- [ ] **Step 5: E2E 冒烟测试（微信开发者工具）**

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 教练登录，进入「我的」 | 展示教练中心页与状态标签 |
| 2 | 点击「个人主页编辑」 | 进入 C-个人主页编辑页，字段回填 |
| 3 | 修改姓名/简介并保存 | Toast「保存成功」，返回「我的」页 |
| 4 | 进入「参考单价设置」，输入 350 保存 | 返回上一页，价格已更新 |
| 5 | 当天重复修改 4 次 | 第 4 次提示「今日参考单价修改次数已达上限」 |
| 6 | 教练点击「退出登录」并确认 | 跳转回教练登录页，再次访问受保护接口返回 401 |
| 7 | 用户登录，进入「我的」 | 展示用户个人中心页 |
| 8 | 用户点击「退出登录」并确认 | 页面刷新为游客态，底部退出按钮隐藏 |

- [ ] **Step 6: 最终 Commit（可选）**

```bash
git add docs/superpowers/plans/2026-08-09-g6-profile-logout.md
git commit -m "docs(plan): complete G6 profile and logout implementation plan"
```

---

## Notes for Implementers

1. **Calicat 优先**：所有页面视觉实现以 Calicat 图层数据为准；当前 SCSS 仅为占位骨架，需替换为真实设计稿颜色、尺寸、间距与阴影 token。
2. **文件上传**：`uploadFile` 假设已实现于 `miniapp-coach/src/api/common.ts` 或 `miniapp-user/src/api/common.ts`；如未实现，需先补齐 `/api/common/file/upload` 接口。
3. **缓存失效**：教练资料更新成功后，如已接入 Redis 教练详情/列表缓存，需在 `CoachProfileService.updateProfile` 与 `updateReferencePrice` 中失效对应 key。
4. **状态机**：教练中心页功能入口显示矩阵必须严格遵循 [C-coach-center-page.md](../../figma/page-spec/C-coach-center-page.md) 4；本计划仅列出 MVP 常用入口，其余入口随后续 US 补充。
5. **测试覆盖**：服务层与控制器层需覆盖正常、字段校验、敏感词、参考单价越界、改价次数限制、教练状态异常等分支；目标行覆盖率 >= 80%。

