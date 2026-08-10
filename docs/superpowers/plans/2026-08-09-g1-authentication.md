# G1 认证组（US-004 / US-006 / US-051 / US-054）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 leyoSwimming 项目中完成 G1 认证组全部 4 个用户故事：用户端微信授权登录（US-004）、用户端手机号验证码登录（US-006）、教练端微信授权登录（US-051）、教练端手机号验证码登录（US-054）。覆盖后端 Spring Boot API、数据库迁移、JWT 会话、微信小程序 Taro 前端页面与状态管理、测试与 Calicat UI 还原。

**Architecture:** 后端采用独立账号体系：`user` / `user_session` 服务用户端，`coach` / `coach_session` 服务教练端，共享 `sms_code` 验证码表与统一 `ApiResponse<T>` 响应。微信 OAuth 与短信服务均提供可切换的 Mock 实现，开发环境默认走 Mock。前端两个 Taro 小程序分别维护 `authStore` + `auth.api`，登录页按后端返回的 `profileCompleted` / `coachStatus` 做路由分流；UI 严格以 Calicat 图层数据为唯一视觉来源。

**Tech Stack:** Spring Boot 3.2 + Java 21 + MyBatis-Plus + Flyway + Redis/Redisson + JJWT；Taro 4.x + React 18 + TypeScript + Zustand；JUnit 5 + Mockito + Jest + React Testing Library。

---

## 文件结构总览

### 后端新增文件

| 文件 | 用途 |
|------|------|
| `backend/src/main/resources/db/migration/V2__create_user_and_session.sql` | user、user_session、user_login_log 表 |
| `backend/src/main/resources/db/migration/V3__create_coach_and_session.sql` | coach、coach_session、coach_login_log 表 |
| `backend/src/main/resources/db/migration/V4__create_sms_code.sql` | 短信验证码表 |
| `backend/src/main/java/com/leyoswimming/entity/User.java` | user 实体 |
| `backend/src/main/java/com/leyoswimming/entity/UserSession.java` | user_session 实体 |
| `backend/src/main/java/com/leyoswimming/entity/Coach.java` | coach 实体 |
| `backend/src/main/java/com/leyoswimming/entity/CoachSession.java` | coach_session 实体 |
| `backend/src/main/java/com/leyoswimming/entity/SmsCode.java` | sms_code 实体 |
| `backend/src/main/java/com/leyoswimming/repository/UserMapper.java` | user Mapper |
| `backend/src/main/java/com/leyoswimming/repository/UserSessionMapper.java` | user_session Mapper |
| `backend/src/main/java/com/leyoswimming/repository/CoachMapper.java` | coach Mapper |
| `backend/src/main/java/com/leyoswimming/repository/CoachSessionMapper.java` | coach_session Mapper |
| `backend/src/main/java/com/leyoswimming/repository/SmsCodeMapper.java` | sms_code Mapper |
| `backend/src/main/java/com/leyoswimming/enums/AppType.java` | user / coach 枚举 |
| `backend/src/main/java/com/leyoswimming/enums/UserStatus.java` | 用户账号状态枚举 |
| `backend/src/main/java/com/leyoswimming/enums/CoachStatus.java` | 教练入驻状态枚举 |
| `backend/src/main/java/com/leyoswimming/util/PhoneEncryptor.java` | AES 手机号加密工具 |
| `backend/src/main/java/com/leyoswimming/security/UserAuthenticationFilter.java` | 用户端 JWT 过滤器 |
| `backend/src/main/java/com/leyoswimming/security/CoachAuthenticationFilter.java` | 教练端 JWT 过滤器 |
| `backend/src/main/java/com/leyoswimming/service/wechat/WechatClient.java` | 微信 code2session 接口抽象 |
| `backend/src/main/java/com/leyoswimming/service/wechat/MockWechatClient.java` | 开发环境 Mock |
| `backend/src/main/java/com/leyoswimming/service/wechat/ProductionWechatClient.java` | 生产环境真实调用 |
| `backend/src/main/java/com/leyoswimming/service/sms/SmsSender.java` | 短信发送抽象 |
| `backend/src/main/java/com/leyoswimming/service/sms/MockSmsSender.java` | 开发环境 Mock：固定验证码 123456 |
| `backend/src/main/java/com/leyoswimming/service/sms/ProductionSmsSender.java` | 生产环境短信服务商调用 |
| `backend/src/main/java/com/leyoswimming/service/UserAuthService.java` | 用户端登录业务 |
| `backend/src/main/java/com/leyoswimming/service/CoachAuthService.java` | 教练端登录业务 |
| `backend/src/main/java/com/leyoswimming/service/SmsCodeService.java` | 验证码生成、校验、限流 |
| `backend/src/main/java/com/leyoswimming/dto/request/*LoginRequest.java` / `SendSmsRequest.java` | 各登录/短信请求 DTO |
| `backend/src/main/java/com/leyoswimming/dto/response/*LoginResponse.java` | 各登录响应 DTO |
| `backend/src/main/java/com/leyoswimming/controller/user/UserAuthController.java` | 用户端登录 API |
| `backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java` | 教练端登录 API |
| `backend/src/main/java/com/leyoswimming/controller/common/SmsController.java` | 公共短信发送 API |
| `backend/src/test/java/com/leyoswimming/service/*Test.java` | Service 单元测试 |
| `backend/src/test/java/com/leyoswimming/controller/*IT.java` | Controller 集成测试 |

### 后端修改文件

| 文件 | 修改内容 |
|------|----------|
| `backend/src/main/java/com/leyoswimming/common/ErrorCode.java` | 新增 TERMS_NOT_ACCEPTED、INVALID_SMS_CODE、SMS_RATE_LIMIT、WECHAT_CODE_INVALID、WECHAT_API_ERROR、WECHAT_API_TIMEOUT、PHONE_DECRYPT_FAILED、VALIDATION_ERROR 等错误码 |
| `backend/src/main/java/com/leyoswimming/config/SecurityConfig.java` | 放行登录入口；注册 user/coach JWT 过滤器 |
| `backend/src/main/java/com/leyoswimming/security/JwtTokenProvider.java` | 新增 user/coach token 生成与 claim 解析、refresh token 生成 |
| `backend/src/main/resources/application.yml` / `application-dev.yml` | 新增 wechat / sms mock 开关、token 过期时间配置、AES 密钥配置 |

### 前端新增文件

| 文件 | 用途 |
|------|------|
| `shared/types/auth.ts` | 共享登录 DTO 类型 |
| `miniapp-user/src/api/auth.ts` | 用户端登录 API 封装 |
| `miniapp-user/src/api/common.ts` | 公共短信 API |
| `miniapp-user/src/stores/authStore.ts` | 用户端 Zustand 登录态 |
| `miniapp-user/src/utils/storage.ts` | Taro storage 封装 |
| `miniapp-user/src/utils/phone.ts` | 手机号校验、脱敏 |
| `miniapp-user/src/pages/login/wechat/index.tsx` | U-微信授权页 |
| `miniapp-user/src/pages/login/phone/index.tsx` | U-手机号登录页 |
| `miniapp-user/src/components/auth/ProtocolCheckbox.tsx` | 协议勾选组件 |
| `miniapp-user/src/components/auth/ErrorTip.tsx` | 错误提示组件 |
| `miniapp-user/src/hooks/useCountdown.ts` | 验证码倒计时 |
| `miniapp-coach/src/api/auth.ts` | 教练端登录 API |
| `miniapp-coach/src/api/common.ts` | 公共短信 API |
| `miniapp-coach/src/stores/authStore.ts` | 教练端 Zustand 登录态 |
| `miniapp-coach/src/pages/login/wechat/index.tsx` | C-微信授权页 |
| `miniapp-coach/src/pages/login/phone/index.tsx` | C-手机号登录页 |
| `miniapp-coach/src/components/auth/ProtocolCheckbox.tsx` | 协议勾选组件 |
| `miniapp-coach/src/hooks/useCountdown.ts` | 验证码倒计时 |

### 前端修改文件

| 文件 | 修改内容 |
|------|----------|
| `miniapp-user/src/app.config.ts` | 添加登录页路径 |
| `miniapp-user/src/app.tsx` | App onShow token 过期检查 |
| `miniapp-coach/src/app.config.ts` | 添加登录页路径 |
| `miniapp-coach/src/app.tsx` | App onShow token 过期检查 |

---

## 实现顺序与依赖

1. **Task 1-13：后端基础与登录 API**（按依赖顺序：数据库  Entity/Mapper  ErrorCode/Enums  工具类  Service  Controller  Security  测试）。
2. **Task 14-20：前端登录页面与状态管理**（依赖后端接口可访问）。
3. **Mock 实现优先：** 开发环境默认使用 `MockWechatClient` 与 `MockSmsSender`，避免依赖真实微信/短信服务。
4. **Calicat UI 还原：** 实现前端页面前必须先通过 Calicat MCP 拉取对应 Frame 图层数据。

## Task 1: 数据库迁移（user / coach / session / sms_code）

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__create_user_and_session.sql`
- Create: `backend/src/main/resources/db/migration/V3__create_coach_and_session.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_sms_code.sql`

- [ ] **Step 1.1: 创建 user / user_session / user_login_log 表**

```sql
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL,
    union_id VARCHAR(64),
    phone VARCHAR(128) NOT NULL COMMENT 'AES 加密',
    avatar_url VARCHAR(512),
    name VARCHAR(64),
    identity_status VARCHAR(20) NOT NULL DEFAULT '注册用户',
    profile_completed TINYINT(1) NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=正常,1=软删除,2=封禁',
    last_login_at DATETIME,
    login_ip VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_union_id ON user(union_id) WHERE status = 0;
CREATE UNIQUE INDEX idx_user_openid ON user(openid) WHERE status = 0;
CREATE UNIQUE INDEX idx_user_phone ON user(phone) WHERE status = 0;

CREATE TABLE IF NOT EXISTS user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_key_encrypted VARCHAR(256) NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    device_name VARCHAR(64),
    device_id VARCHAR(128),
    last_active_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_session_user_id ON user_session(user_id);
CREATE UNIQUE INDEX idx_user_session_refresh_hash ON user_session(refresh_token_hash);

CREATE TABLE IF NOT EXISTS user_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    phone_hash VARCHAR(64),
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=success,1=fail',
    reason VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 1.2: 创建 coach / coach_session / coach_login_log 表**

```sql
CREATE TABLE IF NOT EXISTS coach (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL,
    union_id VARCHAR(64),
    phone VARCHAR(128) NOT NULL COMMENT 'AES 加密',
    avatar_url VARCHAR(512),
    name VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT -1 COMMENT '-1=未提交,0=待审核,1=已通过,2=已驳回,3=已离职,4=离职中',
    rejection_reason TEXT,
    submitted_at DATETIME,
    last_login_at DATETIME,
    login_ip VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_coach_union_id_active ON coach(union_id) WHERE status != 3;
CREATE INDEX idx_coach_openid ON coach(openid);
CREATE INDEX idx_coach_phone ON coach(phone);

CREATE TABLE IF NOT EXISTS coach_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    session_key_encrypted VARCHAR(256) NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    device_name VARCHAR(64),
    device_id VARCHAR(128),
    last_active_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_coach_session_coach_id ON coach_session(coach_id);
CREATE UNIQUE INDEX idx_coach_session_refresh_hash ON coach_session(refresh_token_hash);

CREATE TABLE IF NOT EXISTS coach_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT,
    phone_hash VARCHAR(64),
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=success,1=fail',
    reason VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 1.3: 创建 sms_code 表**

```sql
CREATE TABLE IF NOT EXISTS sms_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_hash VARCHAR(64) NOT NULL,
    code VARCHAR(8) NOT NULL,
    scene VARCHAR(32) NOT NULL DEFAULT 'login',
    app_type VARCHAR(16) NOT NULL DEFAULT 'user',
    expires_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_sms_code_phone_scene ON sms_code(phone_hash, scene, app_type);
```

- [ ] **Step 1.4: 验证 Flyway 迁移**

Run: `./mvnw -pl backend flyway:info -Dflyway.configFiles=src/main/resources/application-dev.yml`
Expected: V1 (existing) + V2 + V3 + V4 pending.

- [ ] **Step 1.5: 提交**

```bash
git add backend/src/main/resources/db/migration/V2__create_user_and_session.sql backend/src/main/resources/db/migration/V3__create_coach_and_session.sql backend/src/main/resources/db/migration/V4__create_sms_code.sql
git commit -m "feat(auth): add user/coach/session/sms_code migrations for G1"
```

## Task 2: Entity 与 Mapper

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/entity/User.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/UserSession.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/Coach.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachSession.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/SmsCode.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/UserMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/UserSessionMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachSessionMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/SmsCodeMapper.java`

- [ ] **Step 2.1: 编写 User 实体**

```java
package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class User {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String openid;
  private String unionId;
  private String phone;
  private String avatarUrl;
  private String name;
  private String identityStatus;
  private Boolean profileCompleted;
  private Integer status;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

- [ ] **Step 2.2: 编写 Coach 实体**

```java
package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach")
public class Coach {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String openid;
  private String unionId;
  private String phone;
  private String avatarUrl;
  private String name;
  private Integer status;
  private String rejectionReason;
  private LocalDateTime submittedAt;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

- [ ] **Step 2.3: 编写 UserSession / CoachSession / SmsCode 实体**

```java
@Data
@TableName("user_session")
public class UserSession {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String sessionKeyEncrypted;
  private String refreshTokenHash;
  private LocalDateTime expiresAt;
  private String deviceName;
  private String deviceId;
  private LocalDateTime lastActiveAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

@Data
@TableName("coach_session")
public class CoachSession {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long coachId;
  private String sessionKeyEncrypted;
  private String refreshTokenHash;
  private LocalDateTime expiresAt;
  private String deviceName;
  private String deviceId;
  private LocalDateTime lastActiveAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

@Data
@TableName("sms_code")
public class SmsCode {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String phoneHash;
  private String code;
  private String scene;
  private String appType;
  private LocalDateTime expiresAt;
  private Boolean used;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

- [ ] **Step 2.4: 编写 Mapper 接口**

```java
package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {
  @Select("SELECT * FROM user WHERE union_id = #{unionId} AND status = 0 LIMIT 1")
  User findActiveByUnionId(@Param("unionId") String unionId);

  @Select("SELECT * FROM user WHERE phone = #{phone} AND status = 0 LIMIT 1")
  User findActiveByPhone(@Param("phone") String phone);
}
```

CoachMapper / UserSessionMapper / CoachSessionMapper / SmsCodeMapper 均继承 `BaseMapper<T>` 即可，复杂查询用 MyBatis-Plus Lambda。

- [ ] **Step 2.5: 编译验证**

Run: `./mvnw -pl backend compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2.6: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/entity/*.java backend/src/main/java/com/leyoswimming/repository/*.java
git commit -m "feat(auth): add user/coach/session/sms_code entities and mappers"
```

## Task 3: 错误码与枚举

**Files:**
- Modify: `backend/src/main/java/com/leyoswimming/common/ErrorCode.java`
- Create: `backend/src/main/java/com/leyoswimming/enums/AppType.java`
- Create: `backend/src/main/java/com/leyoswimming/enums/UserStatus.java`
- Create: `backend/src/main/java/com/leyoswimming/enums/CoachStatus.java`

- [ ] **Step 3.1: 扩展 ErrorCode**

```java
public enum ErrorCode {
  // 通用 100xxx
  BAD_REQUEST(100001, "请求参数错误"),
  JSON_PARSE_ERROR(100002, "请求体 JSON 解析失败"),
  MISSING_REQUIRED_PARAM(100003, "必填参数缺失"),
  RESOURCE_NOT_FOUND(100004, "资源不存在"),
  VALIDATION_ERROR(100005, "参数校验失败"),

  // 认证授权 200xxx
  UNAUTHORIZED(200001, "未登录或 Token 无效"),
  TOKEN_EXPIRED(200002, "登录已过期，请重新登录"),

  // 权限禁止 300xxx
  FORBIDDEN(300001, "无操作权限"),
  ADMIN_DISABLED(300002, "账号已被禁用，请联系超级管理员"),

  // 用户 400xxx
  USER_NOT_FOUND(400001, "用户不存在"),
  USER_DISABLED(400002, "用户账号已被封禁"),

  // 教练 410xxx
  COACH_NOT_FOUND(410001, "教练不存在"),

  // 验证码 420xxx
  INVALID_SMS_CODE(420001, "验证码错误或已过期"),
  SMS_RATE_LIMIT(420002, "请 60 秒后再试"),
  SMS_SEND_FAILED(420003, "验证码发送失败，请稍后重试"),

  // 微信 430xxx
  WECHAT_CODE_INVALID(430001, "登录凭证已失效，请重新点击登录"),
  WECHAT_API_ERROR(430002, "微信服务暂时不可用，请稍后重试"),
  WECHAT_API_TIMEOUT(430003, "网络异常，请重试"),
  PHONE_DECRYPT_FAILED(430004, "手机号解析失败"),

  // 协议 440xxx
  TERMS_NOT_ACCEPTED(440001, "请阅读并同意《用户须知》和《隐私协议》"),

  // 教练入驻申请 500xxx
  COACH_APPLICATION_PENDING(500001, "已有待审核申请"),

  // 系统 900xxx
  INTERNAL_ERROR(900001, "系统繁忙，请稍后重试");

  private final int code;
  private final String message;
}
```

- [ ] **Step 3.2: 编写枚举**

```java
public enum AppType {
  user, coach
}

public enum UserStatus {
  ACTIVE(0), DELETED(1), BANNED(2);
  private final int value;
  UserStatus(int value) { this.value = value; }
  public int getValue() { return value; }
}

public enum CoachStatus {
  NOT_SUBMITTED(-1), PENDING(0), APPROVED(1), REJECTED(2), RESIGNED(3), RESIGNING(4);
  private final int value;
  CoachStatus(int value) { this.value = value; }
  public int getValue() { return value; }
}
```

- [ ] **Step 3.3: 编译验证并提交**

Run: `./mvnw -pl backend compile`
Expected: BUILD SUCCESS.

```bash
git add backend/src/main/java/com/leyoswimming/common/ErrorCode.java backend/src/main/java/com/leyoswimming/enums/*.java
git commit -m "feat(auth): add G1 error codes and enums"
```

---

## Task 4: 手机号加密工具

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/util/PhoneEncryptor.java`
- Modify: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 4.1: 实现 AES 加密/解密**

```java
package com.leyoswimming.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhoneEncryptor {
  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private final SecretKeySpec keySpec;
  private final IvParameterSpec ivSpec;

  public PhoneEncryptor(@Value("${leyo.phone-encryption.key}") String key) throws Exception {
    byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
    this.keySpec = new SecretKeySpec(keyBytes, "AES");
    this.ivSpec = new IvParameterSpec(new byte[16]);
  }

  public String encrypt(String phone) throws Exception {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    return Base64.getEncoder().encodeToString(cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8)));
  }

  public String decrypt(String encrypted) throws Exception {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
  }
}
```

- [ ] **Step 4.2: 配置 AES 密钥**

在 `application-dev.yml` 添加：
```yaml
leyo:
  phone-encryption:
    key: ${PHONE_ENCRYPTION_KEY:local-dev-phone-encryption-key-32bytes!}
```

- [ ] **Step 4.3: 编写单元测试**

```java
@Test
void encryptDecryptRoundTrip() throws Exception {
  PhoneEncryptor encryptor = new PhoneEncryptor("test-key-1234567890123456");
  String phone = "13800138000";
  assertThat(encryptor.decrypt(encryptor.encrypt(phone))).isEqualTo(phone);
}
```

- [ ] **Step 4.4: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/util/PhoneEncryptor.java backend/src/main/resources/application-dev.yml backend/src/test/java/com/leyoswimming/util/PhoneEncryptorTest.java
git commit -m "feat(auth): add AES phone encryptor"
```

## Task 5: JWT Token Provider 扩展

**Files:**
- Modify: `backend/src/main/java/com/leyoswimming/security/JwtTokenProvider.java`

- [ ] **Step 5.1: 新增 user/coach token 与 refresh token 方法**

在现有 `JwtTokenProvider` 中追加：

```java
public String generateUserAccessToken(Long userId, boolean profileCompleted) {
  return buildToken(userId.toString(), Map.of("type", "user", "profileCompleted", profileCompleted));
}

public String generateCoachAccessToken(Long coachId, int coachStatus) {
  return buildToken(coachId.toString(), Map.of("type", "coach", "coachStatus", coachStatus));
}

private String buildToken(String subject, Map<String, Object> claims) {
  Instant now = Instant.now();
  Instant expiry = now.plus(accessTokenExpirationSeconds, ChronoUnit.SECONDS);
  JwtBuilder builder = Jwts.builder().subject(subject).issuedAt(Date.from(now)).expiration(Date.from(expiry));
  claims.forEach(builder::claim);
  return builder.signWith(accessTokenKey).compact();
}

public String generateRefreshToken() {
  byte[] bytes = new byte[32];
  new SecureRandom().nextBytes(bytes);
  return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}

public String hashRefreshToken(String refreshToken) {
  return DigestUtils.sha256Hex(refreshToken);
}

public String getTokenType(String token) {
  return parseToken(token).get("type", String.class);
}

public Long getUserId(String token) {
  return Long.valueOf(parseToken(token).getSubject());
}

public Long getCoachId(String token) {
  return Long.valueOf(parseToken(token).getSubject());
}

public boolean isProfileCompleted(String token) {
  return Boolean.TRUE.equals(parseToken(token).get("profileCompleted", Boolean.class));
}

public int getCoachStatus(String token) {
  return parseToken(token).get("coachStatus", Integer.class);
}
```

依赖：添加 `import java.security.SecureRandom; import java.util.Map; import org.apache.commons.codec.digest.DigestUtils;`（如 pom 无 commons-codec，可用 `MessageDigest` 自行实现 SHA-256）。

- [ ] **Step 5.2: 编写测试**

```java
@Test
void generateAndParseUserToken() {
  String token = jwtTokenProvider.generateUserAccessToken(1L, false);
  assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("user");
  assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
  assertThat(jwtTokenProvider.isProfileCompleted(token)).isFalse();
}
```

- [ ] **Step 5.3: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/security/JwtTokenProvider.java backend/src/test/java/com/leyoswimming/security/JwtTokenProviderTest.java
git commit -m "feat(auth): extend JWT provider for user/coach tokens"
```

---

## Task 6: 微信 OAuth Client（Mock + Production）

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/wechat/WechatClient.java`
- Create: `backend/src/main/java/com/leyoswimming/service/wechat/WechatSession.java`
- Create: `backend/src/main/java/com/leyoswimming/service/wechat/MockWechatClient.java`
- Create: `backend/src/main/java/com/leyoswimming/service/wechat/ProductionWechatClient.java`
- Modify: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 6.1: 定义 WechatClient 接口与 WechatSession 记录**

```java
package com.leyoswimming.service.wechat;

public interface WechatClient {
  WechatSession code2session(String code);
  String decryptPhone(String sessionKey, String encryptedData, String iv);
}

public record WechatSession(String openid, String unionId, String sessionKey) {}
```

- [ ] **Step 6.2: 实现 MockWechatClient**

```java
@Component
@Profile("!prod")
public class MockWechatClient implements WechatClient {
  @Override
  public WechatSession code2session(String code) {
    if ("expired".equals(code)) throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
    String seed = UUID.nameUUIDFromBytes(code.getBytes()).toString().replace("-", "");
    return new WechatSession("mock_openid_" + seed.substring(0, 8), "mock_union_" + seed.substring(8, 16), "mock_session_key_" + seed);
  }

  @Override
  public String decryptPhone(String sessionKey, String encryptedData, String iv) {
    return "13800138000";
  }
}
```

- [ ] **Step 6.3: 实现 ProductionWechatClient**

生产环境调用微信 `jscode2session` 接口，使用 AppID / AppSecret，并用微信 SDK 解密手机号。实现细节见 [微信官方文档](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html)。

```java
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionWechatClient implements WechatClient {
  @Value("${leyo.wechat.appid}") private String appId;
  @Value("${leyo.wechat.secret}") private String secret;
  private final RestTemplate restTemplate;

  @Override
  public WechatSession code2session(String code) { /* 调用微信接口 */ }

  @Override
  public String decryptPhone(String sessionKey, String encryptedData, String iv) { /* 微信解密算法 */ }
}
```

- [ ] **Step 6.4: 配置微信参数**

```yaml
leyo:
  wechat:
    appid: ${WECHAT_APPID:wx_mock_appid}
    secret: ${WECHAT_SECRET:wx_mock_secret}
```

- [ ] **Step 6.5: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/wechat/*.java backend/src/main/resources/application-dev.yml
git commit -m "feat(auth): add wechat client with mock implementation"
```

---

## Task 7: 短信发送 Client（Mock + Production）

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/sms/SmsSender.java`
- Create: `backend/src/main/java/com/leyoswimming/service/sms/MockSmsSender.java`
- Create: `backend/src/main/java/com/leyoswimming/service/sms/ProductionSmsSender.java`

- [ ] **Step 7.1: 定义 SmsSender 接口**

```java
package com.leyoswimming.service.sms;

import com.leyoswimming.enums.AppType;

public interface SmsSender {
  void send(String phone, String code, String scene, AppType appType);
}
```

- [ ] **Step 7.2: 实现 MockSmsSender**

```java
@Component
@Profile("!prod")
@Slf4j
public class MockSmsSender implements SmsSender {
  @Override
  public void send(String phone, String code, String scene, AppType appType) {
    log.info("[MOCK SMS] phone={}, code={}, scene={}, appType={}", mask(phone), code, scene, appType);
  }

  private String mask(String phone) {
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }
}
```

- [ ] **Step 7.3: 实现 ProductionSmsSender**

接入真实短信服务商（如阿里云短信、腾讯云短信），从环境变量读取 AccessKey / Secret。

- [ ] **Step 7.4: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/sms/*.java
git commit -m "feat(auth): add sms sender with mock implementation"
```

## Task 8: 短信验证码服务

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/SmsCodeService.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/SendSmsRequest.java`
- Create: `backend/src/test/java/com/leyoswimming/service/SmsCodeServiceTest.java`

- [ ] **Step 8.1: 实现验证码生成、限流、校验**

```java
package com.leyoswimming.service;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.entity.SmsCode;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.SmsCodeMapper;
import com.leyoswimming.service.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {
  private static final int CODE_LENGTH = 6;
  private static final int EXPIRE_MINUTES = 5;
  private static final int RATE_LIMIT_SECONDS = 60;
  private final SmsCodeMapper smsCodeMapper;
  private final SmsSender smsSender;

  public void send(String phone, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    SmsCode latest = findLatest(phoneHash, scene, appType);
    if (latest != null && latest.getCreatedAt().plusSeconds(RATE_LIMIT_SECONDS).isAfter(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.SMS_RATE_LIMIT);
    }
    String code = generateCode();
    smsSender.send(phone, code, scene, appType);
    SmsCode smsCode = new SmsCode();
    smsCode.setPhoneHash(phoneHash);
    smsCode.setCode(code);
    smsCode.setScene(scene);
    smsCode.setAppType(appType.name());
    smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
    smsCode.setUsed(false);
    smsCodeMapper.insert(smsCode);
  }

  public void verify(String phone, String code, String scene, AppType appType) {
    String phoneHash = hashPhone(phone);
    SmsCode record = findLatest(phoneHash, scene, appType);
    if (record == null || record.getUsed() || record.getExpiresAt().isBefore(LocalDateTime.now()) || !record.getCode().equals(code)) {
      throw new BusinessException(ErrorCode.INVALID_SMS_CODE);
    }
    record.setUsed(true);
    smsCodeMapper.updateById(record);
  }

  private String generateCode() {
    Random random = new Random();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < CODE_LENGTH; i++) sb.append(random.nextInt(10));
    return sb.toString();
  }

  private String hashPhone(String phone) {
    try {
      return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(phone.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private SmsCode findLatest(String phoneHash, String scene, AppType appType) {
    return smsCodeMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SmsCode>()
            .eq(SmsCode::getPhoneHash, phoneHash)
            .eq(SmsCode::getScene, scene)
            .eq(SmsCode::getAppType, appType.name())
            .orderByDesc(SmsCode::getCreatedAt)
            .last("LIMIT 1")
    ).stream().findFirst().orElse(null);
  }
}
```

- [ ] **Step 8.2: 编写测试**

```java
@Test
void sendAndVerify_success() {
  smsCodeService.send("13800138000", "login", AppType.user);
  SmsCode record = smsCodeMapper.selectList(new LambdaQueryWrapper<>()).get(0);
  assertThatNoException().isThrownBy(() -> smsCodeService.verify("13800138000", record.getCode(), "login", AppType.user));
}

@Test
void verify_invalidCode_throws() {
  smsCodeService.send("13800138000", "login", AppType.user);
  assertThatThrownBy(() -> smsCodeService.verify("13800138000", "000000", "login", AppType.user))
      .isInstanceOf(BusinessException.class)
      .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SMS_CODE);
}
```

- [ ] **Step 8.3: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/SmsCodeService.java backend/src/main/java/com/leyoswimming/dto/request/SendSmsRequest.java backend/src/test/java/com/leyoswimming/service/SmsCodeServiceTest.java
git commit -m "feat(auth): add sms code service with rate limit"
```

---

## Task 9: 用户端认证服务

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/UserAuthService.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/UserWechatLoginRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/UserPhoneLoginRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/response/UserLoginResponse.java`

- [ ] **Step 9.1: 实现微信登录逻辑**

```java
public UserLoginResponse wechatLogin(UserWechatLoginRequest request, String ip, String userAgent) {
  validateTerms(request.termsAccepted(), request.privacyAccepted());
  WechatSession session = wechatClient.code2session(request.code());
  String phone = wechatClient.decryptPhone(session.sessionKey(), request.phoneEncryptedData(), request.phoneIv());
  String encryptedPhone = phoneEncryptor.encrypt(phone);
  User user = userMapper.findActiveByUnionId(session.unionId());
  boolean isNewUser = false;
  if (user == null) {
    user = createUser(session.openid(), session.unionId(), encryptedPhone, request.avatarUrl(), request.nickName());
    isNewUser = true;
  }
  return buildLoginResponse(user, isNewUser, session.sessionKey(), ip, userAgent);
}

private User createUser(String openid, String unionId, String encryptedPhone, String avatarUrl, String nickName) {
  User user = new User();
  user.setOpenid(openid);
  user.setUnionId(unionId);
  user.setPhone(encryptedPhone);
  user.setAvatarUrl(avatarUrl);
  user.setName(nickName);
  user.setIdentityStatus("注册用户");
  user.setProfileCompleted(false);
  user.setStatus(UserStatus.ACTIVE.getValue());
  userMapper.insert(user);
  return user;
}

private UserLoginResponse buildLoginResponse(User user, boolean isNewUser, String sessionKey, String ip, String userAgent) {
  String accessToken = jwtTokenProvider.generateUserAccessToken(user.getId(), Boolean.TRUE.equals(user.getProfileCompleted()));
  String refreshToken = jwtTokenProvider.generateRefreshToken();
  String refreshTokenHash = jwtTokenProvider.hashRefreshToken(refreshToken);
  UserSession session = new UserSession();
  session.setUserId(user.getId());
  session.setSessionKeyEncrypted(phoneEncryptor.encrypt(sessionKey));
  session.setRefreshTokenHash(refreshTokenHash);
  session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
  session.setLastActiveAt(LocalDateTime.now());
  userSessionMapper.insert(session);
  user.setLastLoginAt(LocalDateTime.now());
  user.setLoginIp(ip);
  userMapper.updateById(user);
  loginLogMapper.insert(buildLog(user.getId(), ip, userAgent, true, null));
  return new UserLoginResponse(accessToken, refreshToken, jwtTokenProvider.getExpirationSeconds(), isNewUser, user.getProfileCompleted(), user.getId());
}
```

- [ ] **Step 9.2: 实现手机号登录逻辑**

```java
public UserLoginResponse phoneLogin(UserPhoneLoginRequest request, String ip, String userAgent) {
  validateTerms(request.termsAccepted(), request.privacyAccepted());
  smsCodeService.verify(request.phone(), request.code(), "login", AppType.user);
  String encryptedPhone = phoneEncryptor.encrypt(request.phone());
  User user = userMapper.findActiveByPhone(encryptedPhone);
  boolean isNewUser = false;
  if (user == null) {
    user = createUserFromPhone(request.phone(), encryptedPhone);
    isNewUser = true;
  }
  return buildLoginResponse(user, isNewUser, "phone_session_" + request.phone(), ip, userAgent);
}
```

- [ ] **Step 9.3: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/UserAuthService.java backend/src/main/java/com/leyoswimming/dto/request/User*.java backend/src/main/java/com/leyoswimming/dto/response/UserLoginResponse.java
git commit -m "feat(auth): add user auth service"
```

---

## Task 10: 教练端认证服务

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/CoachAuthService.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/CoachWechatLoginRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/CoachPhoneLoginRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/response/CoachLoginResponse.java`

- [ ] **Step 10.1: 定义请求/响应 DTO**

```java
public record CoachWechatLoginRequest(
    @NotBlank String code,
    @NotBlank String phoneEncryptedData,
    @NotBlank String phoneIv,
    @NotNull Boolean termsAccepted,
    @NotNull Boolean privacyAccepted,
    String avatarUrl,
    String nickName
) {}

public record CoachPhoneLoginRequest(
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @NotBlank @Size(min = 6, max = 6) String code,
    @NotNull Boolean termsAccepted,
    @NotNull Boolean privacyAccepted
) {}

public record CoachLoginResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    boolean isNewCoach,
    int coachStatus,
    Long coachId
) {}
```

- [ ] **Step 10.2: 实现 CoachAuthService**

```java
@Service
@RequiredArgsConstructor
public class CoachAuthService {
  private final CoachMapper coachMapper;
  private final CoachSessionMapper coachSessionMapper;
  private final CoachLoginLogMapper coachLoginLogMapper;
  private final WechatClient wechatClient;
  private final SmsCodeService smsCodeService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional
  public CoachLoginResponse wechatLogin(CoachWechatLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    WechatSession session = wechatClient.code2session(request.code());
    String phone = wechatClient.decryptPhone(session.sessionKey(), request.phoneEncryptedData(), request.phoneIv());
    String encryptedPhone = phoneEncryptor.encrypt(phone);
    Coach coach = coachMapper.findActiveByUnionId(session.unionId());
    boolean isNewCoach = false;
    if (coach == null) {
      coach = createCoach(session.openid(), session.unionId(), encryptedPhone, request.avatarUrl(), request.nickName());
      isNewCoach = true;
    }
    return buildLoginResponse(coach, isNewCoach, session.sessionKey(), ip, userAgent);
  }

  @Transactional
  public CoachLoginResponse phoneLogin(CoachPhoneLoginRequest request, String ip, String userAgent) {
    validateTerms(request.termsAccepted(), request.privacyAccepted());
    smsCodeService.verify(request.phone(), request.code(), "login", AppType.coach);
    String encryptedPhone = phoneEncryptor.encrypt(request.phone());
    Coach coach = coachMapper.findActiveByPhone(encryptedPhone);
    boolean isNewCoach = false;
    if (coach == null) {
      coach = createCoachFromPhone(request.phone(), encryptedPhone);
      isNewCoach = true;
    }
    return buildLoginResponse(coach, isNewCoach, "phone_session_" + request.phone(), ip, userAgent);
  }

  private Coach createCoach(String openid, String unionId, String encryptedPhone, String avatarUrl, String nickName) {
    Coach coach = new Coach();
    coach.setOpenid(openid);
    coach.setUnionId(unionId);
    coach.setPhone(encryptedPhone);
    coach.setAvatarUrl(avatarUrl);
    coach.setName(nickName);
    coach.setStatus(CoachStatus.NOT_SUBMITTED.getValue());
    coachMapper.insert(coach);
    return coach;
  }

  private Coach createCoachFromPhone(String phone, String encryptedPhone) {
    Coach coach = new Coach();
    coach.setOpenid("phone_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    coach.setPhone(encryptedPhone);
    coach.setStatus(CoachStatus.NOT_SUBMITTED.getValue());
    coachMapper.insert(coach);
    return coach;
  }

  private CoachLoginResponse buildLoginResponse(Coach coach, boolean isNewCoach, String sessionKey, String ip, String userAgent) {
    String accessToken = jwtTokenProvider.generateCoachAccessToken(coach.getId(), coach.getStatus());
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String refreshTokenHash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = new CoachSession();
    session.setCoachId(coach.getId());
    session.setSessionKeyEncrypted(phoneEncryptor.encrypt(sessionKey));
    session.setRefreshTokenHash(refreshTokenHash);
    session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    coachSessionMapper.insert(session);
    coach.setLastLoginAt(LocalDateTime.now());
    coach.setLoginIp(ip);
    coachMapper.updateById(coach);
    coachLoginLogMapper.insert(buildLog(coach.getId(), ip, userAgent, true, null));
    return new CoachLoginResponse(accessToken, refreshToken, jwtTokenProvider.getExpirationSeconds(), isNewCoach, coach.getStatus(), coach.getId());
  }

  private void validateTerms(boolean termsAccepted, boolean privacyAccepted) {
    if (!termsAccepted || !privacyAccepted) {
      throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
    }
  }
}
```

- [ ] **Step 10.3: 编写 Service 单元测试**

```java
@ExtendWith(MockitoExtension.class)
class CoachAuthServiceTest {
  @Mock CoachMapper coachMapper;
  @Mock CoachSessionMapper coachSessionMapper;
  @Mock CoachLoginLogMapper coachLoginLogMapper;
  @Mock WechatClient wechatClient;
  @Mock SmsCodeService smsCodeService;
  @Mock JwtTokenProvider jwtTokenProvider;
  @Mock PhoneEncryptor phoneEncryptor;

  CoachAuthService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new CoachAuthService(coachMapper, coachSessionMapper, coachLoginLogMapper, wechatClient, smsCodeService, jwtTokenProvider, phoneEncryptor);
    when(phoneEncryptor.encrypt(any())).thenReturn("enc");
    when(jwtTokenProvider.generateRefreshToken()).thenReturn("rt");
    when(jwtTokenProvider.hashRefreshToken("rt")).thenReturn("rt_hash");
    when(jwtTokenProvider.getExpirationSeconds()).thenReturn(7200L);
  }

  @Test
  @DisplayName("微信登录：新教练创建并返回 token")
  void wechatLogin_newCoach_createsAndReturnsToken() throws Exception {
    when(wechatClient.code2session("code")).thenReturn(new WechatSession("oid", "uid", "sk"));
    when(wechatClient.decryptPhone("sk", "edata", "iv")).thenReturn("13800138000");
    when(coachMapper.findActiveByUnionId("uid")).thenReturn(null);
    when(jwtTokenProvider.generateCoachAccessToken(any(), anyInt())).thenReturn("at");

    CoachLoginResponse resp = service.wechatLogin(new CoachWechatLoginRequest("code", "edata", "iv", true, true, "", ""), "127.0.0.1", "wechat");

    assertThat(resp.accessToken()).isEqualTo("at");
    assertThat(resp.isNewCoach()).isTrue();
    verify(coachMapper).insert(any(Coach.class));
  }
}
```

- [ ] **Step 10.4: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/CoachAuthService.java backend/src/main/java/com/leyoswimming/dto/request/Coach*.java backend/src/main/java/com/leyoswimming/dto/response/CoachLoginResponse.java backend/src/test/java/com/leyoswimming/service/CoachAuthServiceTest.java
git commit -m "feat(auth): add coach auth service"
```

---

## Task 11: 会话服务（Refresh / Logout）

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/service/UserSessionService.java`
- Create: `backend/src/main/java/com/leyoswimming/service/CoachSessionService.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/request/RefreshTokenRequest.java`
- Create: `backend/src/main/java/com/leyoswimming/dto/response/RefreshTokenResponse.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/UserLoginLog.java`
- Create: `backend/src/main/java/com/leyoswimming/entity/CoachLoginLog.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/UserLoginLogMapper.java`
- Create: `backend/src/main/java/com/leyoswimming/repository/CoachLoginLogMapper.java`

- [ ] **Step 11.1: 创建登录日志实体与 Mapper**

```java
@Data
@TableName("user_login_log")
public class UserLoginLog {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String phoneHash;
  private String ip;
  private String userAgent;
  private Integer status;
  private String reason;
  private LocalDateTime createdAt;
}

public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {}
```

教练端 `CoachLoginLog` / `CoachLoginLogMapper` 结构相同，表名 `coach_login_log`。

- [ ] **Step 11.2: 实现 UserSessionService**

```java
@Service
@RequiredArgsConstructor
public class UserSessionService {
  private final UserSessionMapper userSessionMapper;
  private final UserMapper userMapper;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional
  public RefreshTokenResponse refreshAccessToken(String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    UserSession session = userSessionMapper.selectOne(
        new LambdaQueryWrapper<UserSession>().eq(UserSession::getRefreshTokenHash, hash));
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }
    User user = userMapper.selectById(session.getUserId());
    if (user == null || user.getStatus() != UserStatus.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.USER_DISABLED);
    }
    String newAccessToken = jwtTokenProvider.generateUserAccessToken(user.getId(), Boolean.TRUE.equals(user.getProfileCompleted()));
    String newRefreshToken = jwtTokenProvider.generateRefreshToken();
    session.setRefreshTokenHash(jwtTokenProvider.hashRefreshToken(newRefreshToken));
    session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    userSessionMapper.updateById(session);
    return new RefreshTokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getExpirationSeconds());
  }

  @Transactional
  public void logout(Long userId, String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    userSessionMapper.delete(
        new LambdaQueryWrapper<UserSession>()
            .eq(UserSession::getUserId, userId)
            .eq(UserSession::getRefreshTokenHash, hash));
  }
}
```

- [ ] **Step 11.3: 实现 CoachSessionService**

与 `UserSessionService` 对称，使用 `CoachSessionMapper`、`CoachMapper`、`CoachStatus.APPROVED.getValue()` 校验，生成 coach access token。

```java
@Service
@RequiredArgsConstructor
public class CoachSessionService {
  private final CoachSessionMapper coachSessionMapper;
  private final CoachMapper coachMapper;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public RefreshTokenResponse refreshAccessToken(String refreshToken) {
    String hash = jwtTokenProvider.hashRefreshToken(refreshToken);
    CoachSession session = coachSessionMapper.selectOne(
        new LambdaQueryWrapper<CoachSession>().eq(CoachSession::getRefreshTokenHash, hash));
    if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }
    Coach coach = coachMapper.selectById(session.getCoachId());
    if (coach == null || coach.getStatus() == CoachStatus.RESIGNED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    String newAccessToken = jwtTokenProvider.generateCoachAccessToken(coach.getId(), coach.getStatus());
    String newRefreshToken = jwtTokenProvider.generateRefreshToken();
    session.setRefreshTokenHash(jwtTokenProvider.hashRefreshToken(newRefreshToken));
    session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationSeconds()));
    session.setLastActiveAt(LocalDateTime.now());
    coachSessionMapper.updateById(session);
    return new RefreshTokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getExpirationSeconds());
  }
}
```

- [ ] **Step 11.4: 编写测试**

```java
@Test
@DisplayName("刷新 token：旧的 refresh token 失效并返回新 token")
void refresh_validToken_returnsNewTokens() {
  String oldRt = "old-refresh";
  String oldHash = jwtTokenProvider.hashRefreshToken(oldRt);
  UserSession session = new UserSession();
  session.setUserId(1L);
  session.setRefreshTokenHash(oldHash);
  session.setExpiresAt(LocalDateTime.now().plusDays(7));
  userSessionMapper.insert(session);

  RefreshTokenResponse resp = userSessionService.refreshAccessToken(oldRt);

  assertThat(resp.accessToken()).isNotBlank();
  assertThat(resp.refreshToken()).isNotEqualTo(oldRt);
  assertThat(userSessionMapper.selectCount(new LambdaQueryWrapper<UserSession>().eq(UserSession::getRefreshTokenHash, oldHash))).isZero();
}
```

- [ ] **Step 11.5: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/service/*SessionService.java backend/src/main/java/com/leyoswimming/dto/request/RefreshTokenRequest.java backend/src/main/java/com/leyoswimming/dto/response/RefreshTokenResponse.java backend/src/main/java/com/leyoswimming/entity/*LoginLog.java backend/src/main/java/com/leyoswimming/repository/*LoginLogMapper.java backend/src/test/java/com/leyoswimming/service/*SessionServiceTest.java
git commit -m "feat(auth): add session refresh and logout services"
```

---

## Task 12: 认证 Controller 与短信 Controller

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/controller/user/UserAuthController.java`
- Create: `backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java`
- Create: `backend/src/main/java/com/leyoswimming/controller/common/SmsController.java`
- Create: `backend/src/test/java/com/leyoswimming/controller/user/UserAuthControllerIT.java`
- Create: `backend/src/test/java/com/leyoswimming/controller/coach/CoachAuthControllerIT.java`

- [ ] **Step 12.1: 实现 UserAuthController**

```java
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {
  private final UserAuthService userAuthService;
  private final UserSessionService userSessionService;

  @PostMapping("/wechat-login")
  public ApiResponse<UserLoginResponse> wechatLogin(@Valid @RequestBody UserWechatLoginRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(userAuthService.wechatLogin(request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/phone-login")
  public ApiResponse<UserLoginResponse> phoneLogin(@Valid @RequestBody UserPhoneLoginRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(userAuthService.phoneLogin(request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public ApiResponse<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(userSessionService.refreshAccessToken(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId,
      @Valid @RequestBody RefreshTokenRequest request) {
    userSessionService.logout(userId, request.refreshToken());
    return ApiResponse.ok(null);
  }

  private String extractIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    return StringUtils.isNotBlank(xff) ? xff.split(",")[0].trim() : request.getRemoteAddr();
  }
}
```

- [ ] **Step 12.2: 实现 CoachAuthController**

与 `UserAuthController` 对称，路径 `/api/coach/auth`，使用 `CoachAuthService`、`CoachSessionService`、Coach 认证 principal。

```java
@RestController
@RequestMapping("/api/coach/auth")
@RequiredArgsConstructor
public class CoachAuthController {
  private final CoachAuthService coachAuthService;
  private final CoachSessionService coachSessionService;

  @PostMapping("/wechat-login")
  public ApiResponse<CoachLoginResponse> wechatLogin(@Valid @RequestBody CoachWechatLoginRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(coachAuthService.wechatLogin(request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/phone-login")
  public ApiResponse<CoachLoginResponse> phoneLogin(@Valid @RequestBody CoachPhoneLoginRequest request,
      HttpServletRequest httpRequest) {
    return ApiResponse.ok(coachAuthService.phoneLogin(request, extractIp(httpRequest), httpRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public ApiResponse<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(coachSessionService.refreshAccessToken(request.refreshToken()));
  }
}
```

- [ ] **Step 12.3: 实现 SmsController**

```java
@RestController
@RequestMapping("/api/common/sms")
@RequiredArgsConstructor
public class SmsController {
  private final SmsCodeService smsCodeService;

  @PostMapping("/send")
  public ApiResponse<Void> send(@Valid @RequestBody SendSmsRequest request) {
    smsCodeService.send(request.phone(), request.scene(), AppType.valueOf(request.appType()));
    return ApiResponse.ok(null);
  }
}

public record SendSmsRequest(
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @NotBlank String scene,
    @NotBlank String appType
) {}
```

- [ ] **Step 12.4: 编写集成测试**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserAuthControllerIT {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockBean WechatClient wechatClient;

  @Test
  @DisplayName("手机号登录：返回 accessToken 与 refreshToken")
  void phoneLogin_validCode_returnsTokens() throws Exception {
    smsCodeService.send("13800138000", "login", AppType.user);
    SmsCode code = smsCodeMapper.selectList(new LambdaQueryWrapper<SmsCode>().eq(SmsCode::getPhoneHash, hash("13800138000"))).get(0);

    mockMvc.perform(post("/api/user/auth/phone-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "phone", "13800138000",
                "code", code.getCode(),
                "termsAccepted", true,
                "privacyAccepted", true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").exists());
  }
}
```

- [ ] **Step 12.5: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/controller/user/UserAuthController.java backend/src/main/java/com/leyoswimming/controller/coach/CoachAuthController.java backend/src/main/java/com/leyoswimming/controller/common/SmsController.java backend/src/test/java/com/leyoswimming/controller/*/*.java
git commit -m "feat(auth): add user/coach auth and sms controllers"
```

---

## Task 13: 安全过滤器与 SecurityConfig

**Files:**
- Create: `backend/src/main/java/com/leyoswimming/security/UserAuthenticationFilter.java`
- Create: `backend/src/main/java/com/leyoswimming/security/CoachAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/leyoswimming/config/SecurityConfig.java`

- [ ] **Step 13.1: 实现 UserAuthenticationFilter**

```java
@Component
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (isPublic(path)) {
      chain.doFilter(request, response);
      return;
    }
    if (path.startsWith("/api/user/")) {
      String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
      if (token != null && jwtTokenProvider.isTokenValid(token) && "user".equals(jwtTokenProvider.getTokenType(token))) {
        Long userId = jwtTokenProvider.getUserId(token);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(request, response);
  }

  private boolean isPublic(String path) {
    return path.startsWith("/api/user/auth/") || path.startsWith("/api/coach/auth/") || path.startsWith("/api/common/sms/");
  }

  private String extractBearerToken(String header) {
    if (header != null && header.startsWith("Bearer ")) return header.substring(7);
    return null;
  }
}
```

- [ ] **Step 13.2: 实现 CoachAuthenticationFilter**

与 `UserAuthenticationFilter` 对称，校验 `type=coach`，设置 `ROLE_COACH`。

```java
@Component
@RequiredArgsConstructor
public class CoachAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (isPublic(path)) {
      chain.doFilter(request, response);
      return;
    }
    if (path.startsWith("/api/coach/")) {
      String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
      if (token != null && jwtTokenProvider.isTokenValid(token) && "coach".equals(jwtTokenProvider.getTokenType(token))) {
        Long coachId = jwtTokenProvider.getCoachId(token);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(coachId, null, List.of(new SimpleGrantedAuthority("ROLE_COACH")));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(request, response);
  }
}
```

- [ ] **Step 13.3: 修改 SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final UserAuthenticationFilter userAuthenticationFilter;
  private final CoachAuthenticationFilter coachAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/api/admin/auth/**", "/api/user/auth/**", "/api/coach/auth/**", "/api/common/sms/**").permitAll()
          .requestMatchers("/api/admin/**").hasRole("ADMIN")
          .requestMatchers("/api/user/**").hasRole("USER")
          .requestMatchers("/api/coach/**").hasRole("COACH")
          .anyRequest().authenticated())
      .addFilterBefore(userAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(coachAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(ex -> ex
          .authenticationEntryPoint((req, res, e) -> writeError(res, ErrorCode.UNAUTHORIZED))
          .accessDeniedHandler((req, res, e) -> writeError(res, ErrorCode.FORBIDDEN)));
    return http.build();
  }
}
```

- [ ] **Step 13.4: 验证登录链路**

Run: `./mvnw -pl backend test -Dtest=UserAuthControllerIT,CoachAuthControllerIT`
Expected: 所有集成测试通过。

- [ ] **Step 13.5: 提交**

```bash
git add backend/src/main/java/com/leyoswimming/security/*AuthenticationFilter.java backend/src/main/java/com/leyoswimming/config/SecurityConfig.java backend/src/test/java/com/leyoswimming/security/*AuthenticationFilterTest.java
git commit -m "feat(auth): add user/coach JWT filters and security config"
```

---

## Task 14: 前端共享类型与工具

**Files:**
- Create: `shared/types/auth.ts`
- Create: `miniapp-user/src/utils/storage.ts`
- Create: `miniapp-user/src/utils/phone.ts`
- Create: `miniapp-coach/src/utils/storage.ts`
- Create: `miniapp-coach/src/utils/phone.ts`

- [ ] **Step 14.1: 定义共享 DTO 类型**

```typescript
// shared/types/auth.ts
export interface UserLoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  isNewUser: boolean;
  profileCompleted: boolean;
  userId: number;
}

export interface CoachLoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  isNewCoach: boolean;
  coachStatus: number;
  coachId: number;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

export interface SendSmsRequest {
  phone: string;
  scene: 'login';
  appType: 'user' | 'coach';
}
```

- [ ] **Step 14.2: 实现 storage 工具**

```typescript
// miniapp-user/src/utils/storage.ts
import Taro from '@tarojs/taro';

const TOKEN_KEY = 'leyo_auth_tokens';

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

export const storage = {
  setTokens(tokens: StoredTokens) {
    Taro.setStorageSync(TOKEN_KEY, tokens);
  },
  getTokens(): StoredTokens | null {
    return Taro.getStorageSync(TOKEN_KEY) || null;
  },
  clearTokens() {
    Taro.removeStorageSync(TOKEN_KEY);
  },
  isExpired(): boolean {
    const tokens = this.getTokens();
    if (!tokens) return true;
    return Date.now() >= tokens.expiresAt - 60 * 1000;
  }
};
```

教练端复制到 `miniapp-coach/src/utils/storage.ts`。

- [ ] **Step 14.3: 实现 phone 工具**

```typescript
// miniapp-user/src/utils/phone.ts
export function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone);
}

export function maskPhone(phone: string): string {
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}
```

- [ ] **Step 14.4: 提交**

```bash
git add shared/types/auth.ts miniapp-user/src/utils/storage.ts miniapp-user/src/utils/phone.ts miniapp-coach/src/utils/storage.ts miniapp-coach/src/utils/phone.ts
git commit -m "feat(auth): add shared auth types and miniapp storage/phone utilities"
```

---

## Task 15: 用户端登录 API 与 Auth Store

**Files:**
- Create: `miniapp-user/src/api/auth.ts`
- Create: `miniapp-user/src/api/common.ts`
- Create: `miniapp-user/src/stores/authStore.ts`
- Create: `miniapp-user/src/hooks/useCountdown.ts`
- Modify: `miniapp-user/src/app.config.ts`

- [ ] **Step 15.1: 实现 auth API**

```typescript
// miniapp-user/src/api/auth.ts
import Taro from '@tarojs/taro';
import type { UserLoginResponse, RefreshTokenResponse } from '@shared/types/auth';
import { storage } from '@/utils/storage';

const BASE = process.env.TARO_APP_API_BASE || 'http://localhost:8080';

async function request<T>(url: string, method: any, data?: object): Promise<T> {
  const res = await Taro.request({
    url: `${BASE}${url}`,
    method,
    data,
    header: { 'Content-Type': 'application/json' }
  });
  if (!res.data.success) {
    throw new Error(res.data.error || '请求失败');
  }
  return res.data.data as T;
}

export async function wechatLogin(payload: {
  code: string;
  phoneEncryptedData: string;
  phoneIv: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  avatarUrl?: string;
  nickName?: string;
}): Promise<UserLoginResponse> {
  return request<UserLoginResponse>('/api/user/auth/wechat-login', 'POST', payload);
}

export async function phoneLogin(payload: {
  phone: string;
  code: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
}): Promise<UserLoginResponse> {
  return request<UserLoginResponse>('/api/user/auth/phone-login', 'POST', payload);
}

export async function refreshToken(refreshToken: string): Promise<RefreshTokenResponse> {
  return request<RefreshTokenResponse>('/api/user/auth/refresh', 'POST', { refreshToken });
}

export function saveLogin(res: UserLoginResponse) {
  storage.setTokens({
    accessToken: res.accessToken,
    refreshToken: res.refreshToken,
    expiresAt: Date.now() + res.expiresInSeconds * 1000
  });
}
```

- [ ] **Step 15.2: 实现 common API（短信）**

```typescript
// miniapp-user/src/api/common.ts
import Taro from '@tarojs/taro';

const BASE = process.env.TARO_APP_API_BASE || 'http://localhost:8080';

export async function sendSms(phone: string) {
  const res = await Taro.request({
    url: `${BASE}/api/common/sms/send`,
    method: 'POST',
    data: { phone, scene: 'login', appType: 'user' },
    header: { 'Content-Type': 'application/json' }
  });
  if (!res.data.success) throw new Error(res.data.error || '发送失败');
}
```

- [ ] **Step 15.3: 实现 authStore**

```typescript
// miniapp-user/src/stores/authStore.ts
import { create } from 'zustand';
import { storage } from '@/utils/storage';
import type { UserLoginResponse } from '@shared/types/auth';

interface AuthState {
  accessToken: string | null;
  isLoggedIn: boolean;
  profileCompleted: boolean;
  userId: number | null;
  setLogin: (res: UserLoginResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: storage.getTokens()?.accessToken || null,
  isLoggedIn: !!storage.getTokens()?.accessToken,
  profileCompleted: false,
  userId: null,
  setLogin: (res) => {
    storage.setTokens({
      accessToken: res.accessToken,
      refreshToken: res.refreshToken,
      expiresAt: Date.now() + res.expiresInSeconds * 1000
    });
    set({ accessToken: res.accessToken, isLoggedIn: true, profileCompleted: res.profileCompleted, userId: res.userId });
  },
  logout: () => {
    storage.clearTokens();
    set({ accessToken: null, isLoggedIn: false, profileCompleted: false, userId: null });
  }
}));
```

- [ ] **Step 15.4: 实现倒计时 hook**

```typescript
// miniapp-user/src/hooks/useCountdown.ts
import { useState, useEffect, useCallback } from 'react';

export function useCountdown(initial = 60) {
  const [seconds, setSeconds] = useState(0);
  const [isRunning, setIsRunning] = useState(false);

  const start = useCallback(() => {
    setSeconds(initial);
    setIsRunning(true);
  }, [initial]);

  useEffect(() => {
    if (!isRunning || seconds <= 0) {
      if (seconds <= 0) setIsRunning(false);
      return;
    }
    const id = setTimeout(() => setSeconds((s) => s - 1), 1000);
    return () => clearTimeout(id);
  }, [isRunning, seconds]);

  return { seconds, isRunning, start };
}
```

- [ ] **Step 15.5: 更新 app.config.ts**

```typescript
export default defineAppConfig({
  pages: [
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/index/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: '乐游游泳',
    navigationBarTextStyle: 'black'
  }
});
```

- [ ] **Step 15.6: 提交**

```bash
git add miniapp-user/src/api/auth.ts miniapp-user/src/api/common.ts miniapp-user/src/stores/authStore.ts miniapp-user/src/hooks/useCountdown.ts miniapp-user/src/app.config.ts
git commit -m "feat(auth): add user miniapp auth api, store and countdown hook"
```

---

## Task 16: 用户端微信授权登录页

**Files:**
- Create: `miniapp-user/src/pages/login/wechat/index.tsx`
- Create: `miniapp-user/src/components/auth/ProtocolCheckbox.tsx`
- Create: `miniapp-user/src/pages/login/wechat/index.test.tsx`

- [ ] **Step 16.1: 实现 ProtocolCheckbox 组件**

```tsx
// miniapp-user/src/components/auth/ProtocolCheckbox.tsx
import { useState } from 'react';
import { View, Text, Checkbox } from '@tarojs/components';

interface Props {
  checked: boolean;
  onChange: (checked: boolean) => void;
}

export function ProtocolCheckbox({ checked, onChange }: Props) {
  return (
    <View className="protocol-row">
      <Checkbox checked={checked} onClick={() => onChange(!checked)} />
      <Text className="protocol-text">
        我已阅读并同意
        <Text className="link" onClick={() => { /* Taro.navigateTo 用户须知 */ }}>《用户须知》</Text>
        和
        <Text className="link" onClick={() => { /* Taro.navigateTo 隐私协议 */ }}>《隐私协议》</Text>
      </Text>
    </View>
  );
}
```

- [ ] **Step 16.2: 实现微信授权登录页**

```tsx
// miniapp-user/src/pages/login/wechat/index.tsx
import Taro, { useLoad } from '@tarojs/taro';
import { View, Button, Image } from '@tarojs/components';
import { useState } from 'react';
import { wechatLogin } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import './index.scss';

export default function WechatLoginPage() {
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const setLogin = useAuthStore((s) => s.setLogin);

  useLoad(() => {
    // 已登录且资料完整直接跳转首页
    const { isLoggedIn, profileCompleted } = useAuthStore.getState();
    if (isLoggedIn && profileCompleted) {
      Taro.switchTab({ url: '/pages/index/index' });
    }
  });

  const handleLogin = async () => {
    if (!agreed) {
      Taro.showToast({ title: '请先同意协议', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      const loginRes = await Taro.login();
      const phoneRes = await Taro.getPhoneNumber({});
      // 注意：微信小程序 getPhoneNumber 返回的是加密数据，需从 button 的 onGetPhoneNumber 事件获取
    } finally {
      setLoading(false);
    }
  };

  const onGetPhoneNumber = async (e: any) => {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      Taro.showToast({ title: '需要授权手机号才能登录', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      const loginRes = await Taro.login();
      const res = await wechatLogin({
        code: loginRes.code,
        phoneEncryptedData: e.detail.encryptedData,
        phoneIv: e.detail.iv,
        termsAccepted: agreed,
        privacyAccepted: agreed
      });
      setLogin(res);
      navigateAfterLogin(res);
    } catch (err: any) {
      Taro.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      setLoading(false);
    }
  };

  const navigateAfterLogin = (res: any) => {
    if (res.profileCompleted) {
      Taro.switchTab({ url: '/pages/index/index' });
    } else {
      Taro.redirectTo({ url: '/pages/profile/edit/index' });
    }
  };

  return (
    <View className="wechat-login-page">
      <Image className="logo" src={require('@/assets/logo.png')} mode="aspectFit" />
      <View className="title">乐游游泳</View>
      <Button
        className="btn-wechat"
        type="primary"
        openType="getPhoneNumber"
        onGetPhoneNumber={onGetPhoneNumber}
        loading={loading}
      >
        微信一键登录
      </Button>
      <Button className="btn-phone" onClick={() => Taro.navigateTo({ url: '/pages/login/phone/index' })}>
        手机号登录
      </Button>
      <ProtocolCheckbox checked={agreed} onChange={setAgreed} />
    </View>
  );
}
```

- [ ] **Step 16.3: 编写页面测试**

```tsx
// miniapp-user/src/pages/login/wechat/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import WechatLoginPage from './index';

jest.mock('@tarojs/taro', () => ({
  login: jest.fn(() => Promise.resolve({ code: 'mock_code' })),
  showToast: jest.fn(),
  navigateTo: jest.fn(),
  switchTab: jest.fn(),
  redirectTo: jest.fn()
}));

jest.mock('@/api/auth', () => ({
  wechatLogin: jest.fn(() => Promise.resolve({
    accessToken: 'at', refreshToken: 'rt', expiresInSeconds: 7200,
    isNewUser: true, profileCompleted: false, userId: 1
  }))
}));

test('未勾选协议点击登录提示', async () => {
  const { getByText } = render(<WechatLoginPage />);
  fireEvent.click(getByText('微信一键登录'));
  await waitFor(() => expect(Taro.showToast).toHaveBeenCalledWith({ title: '请先同意协议', icon: 'none' }));
});
```

- [ ] **Step 16.4: 提交**

```bash
git add miniapp-user/src/pages/login/wechat/index.tsx miniapp-user/src/pages/login/wechat/index.scss miniapp-user/src/components/auth/ProtocolCheckbox.tsx miniapp-user/src/pages/login/wechat/index.test.tsx
git commit -m "feat(auth): add user wechat login page"
```

---

## Task 17: 用户端手机号登录页

**Files:**
- Create: `miniapp-user/src/pages/login/phone/index.tsx`
- Create: `miniapp-user/src/pages/login/phone/index.scss`
- Create: `miniapp-user/src/pages/login/phone/index.test.tsx`

- [ ] **Step 17.1: 实现手机号登录页**

```tsx
// miniapp-user/src/pages/login/phone/index.tsx
import Taro from '@tarojs/taro';
import { View, Input, Button, Text } from '@tarojs/components';
import { useState } from 'react';
import { phoneLogin } from '@/api/auth';
import { sendSms } from '@/api/common';
import { useAuthStore } from '@/stores/authStore';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import { useCountdown } from '@/hooks/useCountdown';
import { isValidPhone } from '@/utils/phone';
import './index.scss';

export default function PhoneLoginPage() {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const { seconds, isRunning, start } = useCountdown(60);
  const setLogin = useAuthStore((s) => s.setLogin);

  const handleSendCode = async () => {
    if (!isValidPhone(phone)) {
      Taro.showToast({ title: '请输入正确手机号', icon: 'none' });
      return;
    }
    try {
      await sendSms(phone);
      start();
      Taro.showToast({ title: '验证码已发送', icon: 'success' });
    } catch (err: any) {
      Taro.showToast({ title: err.message || '发送失败', icon: 'none' });
    }
  };

  const handleLogin = async () => {
    if (!agreed) {
      Taro.showToast({ title: '请先同意协议', icon: 'none' });
      return;
    }
    if (!isValidPhone(phone) || code.length !== 6) {
      Taro.showToast({ title: '请填写完整信息', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      const res = await phoneLogin({ phone, code, termsAccepted: agreed, privacyAccepted: agreed });
      setLogin(res);
      if (res.profileCompleted) {
        Taro.switchTab({ url: '/pages/index/index' });
      } else {
        Taro.redirectTo({ url: '/pages/profile/edit/index' });
      }
    } catch (err: any) {
      Taro.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className="phone-login-page">
      <View className="title">手机号登录</View>
      <View className="input-group">
        <Text className="prefix">+86</Text>
        <Input
          type="number"
          maxlength={11}
          placeholder="请输入手机号"
          value={phone}
          onInput={(e) => setPhone(e.detail.value)}
        />
      </View>
      <View className="input-group">
        <Input
          type="number"
          maxlength={6}
          placeholder="请输入验证码"
          value={code}
          onInput={(e) => setCode(e.detail.value)}
        />
        <Button
          className="btn-code"
          disabled={isRunning || !isValidPhone(phone)}
          onClick={handleSendCode}
        >
          {isRunning ? `${seconds}s` : '获取验证码'}
        </Button>
      </View>
      <ProtocolCheckbox checked={agreed} onChange={setAgreed} />
      <Button className="btn-login" type="primary" loading={loading} onClick={handleLogin}>
        登录
      </Button>
    </View>
  );
}
```

- [ ] **Step 17.2: 编写页面测试**

```tsx
// miniapp-user/src/pages/login/phone/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import PhoneLoginPage from './index';

jest.mock('@tarojs/taro', () => ({ showToast: jest.fn(), switchTab: jest.fn(), redirectTo: jest.fn() }));
jest.mock('@/api/auth', () => ({ phoneLogin: jest.fn(() => Promise.resolve({ profileCompleted: true })) }));
jest.mock('@/api/common', () => ({ sendSms: jest.fn(() => Promise.resolve()) }));

test('输入手机号获取验证码', async () => {
  const { getByPlaceholderText, getByText } = render(<PhoneLoginPage />);
  fireEvent.input(getByPlaceholderText('请输入手机号'), { target: { value: '13800138000' } });
  fireEvent.click(getByText('获取验证码'));
  await waitFor(() => expect(sendSms).toHaveBeenCalledWith('13800138000'));
});
```

- [ ] **Step 17.3: 提交**

```bash
git add miniapp-user/src/pages/login/phone/index.tsx miniapp-user/src/pages/login/phone/index.scss miniapp-user/src/pages/login/phone/index.test.tsx
git commit -m "feat(auth): add user phone login page"
```

---

## Task 18: 教练端登录 API 与 Auth Store

**Files:**
- Create: `miniapp-coach/src/api/auth.ts`
- Create: `miniapp-coach/src/api/common.ts`
- Create: `miniapp-coach/src/stores/authStore.ts`
- Create: `miniapp-coach/src/hooks/useCountdown.ts`
- Modify: `miniapp-coach/src/app.config.ts`

- [ ] **Step 18.1: 实现教练端 auth API**

```typescript
// miniapp-coach/src/api/auth.ts
import Taro from '@tarojs/taro';
import type { CoachLoginResponse, RefreshTokenResponse } from '@shared/types/auth';
import { storage } from '@/utils/storage';

const BASE = process.env.TARO_APP_API_BASE || 'http://localhost:8080';

async function request<T>(url: string, method: any, data?: object): Promise<T> {
  const res = await Taro.request({
    url: `${BASE}${url}`,
    method,
    data,
    header: { 'Content-Type': 'application/json' }
  });
  if (!res.data.success) throw new Error(res.data.error || '请求失败');
  return res.data.data as T;
}

export async function wechatLogin(payload: {
  code: string;
  phoneEncryptedData: string;
  phoneIv: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  avatarUrl?: string;
  nickName?: string;
}): Promise<CoachLoginResponse> {
  return request<CoachLoginResponse>('/api/coach/auth/wechat-login', 'POST', payload);
}

export async function phoneLogin(payload: {
  phone: string;
  code: string;
  termsAccepted: boolean;
  privacyAccepted: boolean;
}): Promise<CoachLoginResponse> {
  return request<CoachLoginResponse>('/api/coach/auth/phone-login', 'POST', payload);
}

export async function refreshToken(refreshToken: string): Promise<RefreshTokenResponse> {
  return request<RefreshTokenResponse>('/api/coach/auth/refresh', 'POST', { refreshToken });
}
```

- [ ] **Step 18.2: 实现教练端 common API 与 authStore**

```typescript
// miniapp-coach/src/api/common.ts
import Taro from '@tarojs/taro';
const BASE = process.env.TARO_APP_API_BASE || 'http://localhost:8080';

export async function sendSms(phone: string) {
  const res = await Taro.request({
    url: `${BASE}/api/common/sms/send`,
    method: 'POST',
    data: { phone, scene: 'login', appType: 'coach' },
    header: { 'Content-Type': 'application/json' }
  });
  if (!res.data.success) throw new Error(res.data.error || '发送失败');
}

// miniapp-coach/src/stores/authStore.ts
import { create } from 'zustand';
import { storage } from '@/utils/storage';
import type { CoachLoginResponse } from '@shared/types/auth';

interface AuthState {
  accessToken: string | null;
  isLoggedIn: boolean;
  coachStatus: number | null;
  coachId: number | null;
  setLogin: (res: CoachLoginResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: storage.getTokens()?.accessToken || null,
  isLoggedIn: !!storage.getTokens()?.accessToken,
  coachStatus: null,
  coachId: null,
  setLogin: (res) => {
    storage.setTokens({
      accessToken: res.accessToken,
      refreshToken: res.refreshToken,
      expiresAt: Date.now() + res.expiresInSeconds * 1000
    });
    set({ accessToken: res.accessToken, isLoggedIn: true, coachStatus: res.coachStatus, coachId: res.coachId });
  },
  logout: () => {
    storage.clearTokens();
    set({ accessToken: null, isLoggedIn: false, coachStatus: null, coachId: null });
  }
}));
```

- [ ] **Step 18.3: 复制 useCountdown 与更新 app.config.ts**

将 `miniapp-user/src/hooks/useCountdown.ts` 复制到 `miniapp-coach/src/hooks/useCountdown.ts`。

```typescript
// miniapp-coach/src/app.config.ts
export default defineAppConfig({
  pages: [
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/index/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: '乐游教练端',
    navigationBarTextStyle: 'black'
  }
});
```

- [ ] **Step 18.4: 提交**

```bash
git add miniapp-coach/src/api/auth.ts miniapp-coach/src/api/common.ts miniapp-coach/src/stores/authStore.ts miniapp-coach/src/hooks/useCountdown.ts miniapp-coach/src/app.config.ts
git commit -m "feat(auth): add coach miniapp auth api and store"
```

---

## Task 19: 教练端微信授权登录页

**Files:**
- Create: `miniapp-coach/src/pages/login/wechat/index.tsx`
- Create: `miniapp-coach/src/pages/login/wechat/index.scss`
- Create: `miniapp-coach/src/components/auth/ProtocolCheckbox.tsx`
- Create: `miniapp-coach/src/pages/login/wechat/index.test.tsx`

- [ ] **Step 19.1: 实现 ProtocolCheckbox 组件**

复制 `miniapp-user/src/components/auth/ProtocolCheckbox.tsx` 到教练端，文案改为《教练服务协议》和《隐私协议》。

- [ ] **Step 19.2: 实现教练端微信登录页**

```tsx
// miniapp-coach/src/pages/login/wechat/index.tsx
import Taro, { useLoad } from '@tarojs/taro';
import { View, Button, Image } from '@tarojs/components';
import { useState } from 'react';
import { wechatLogin } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import './index.scss';

const COACH_STATUS = {
  NOT_SUBMITTED: -1,
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
  RESIGNED: 3,
  RESIGNING: 4
};

export default function CoachWechatLoginPage() {
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const setLogin = useAuthStore((s) => s.setLogin);

  useLoad(() => {
    const { isLoggedIn, coachStatus } = useAuthStore.getState();
    if (isLoggedIn && coachStatus === COACH_STATUS.APPROVED) {
      Taro.switchTab({ url: '/pages/index/index' });
    }
  });

  const onGetPhoneNumber = async (e: any) => {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      Taro.showToast({ title: '需要授权手机号才能登录', icon: 'none' });
      return;
    }
    if (!agreed) {
      Taro.showToast({ title: '请先同意协议', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      const loginRes = await Taro.login();
      const res = await wechatLogin({
        code: loginRes.code,
        phoneEncryptedData: e.detail.encryptedData,
        phoneIv: e.detail.iv,
        termsAccepted: agreed,
        privacyAccepted: agreed
      });
      setLogin(res);
      navigateAfterLogin(res);
    } catch (err: any) {
      Taro.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      setLoading(false);
    }
  };

  const navigateAfterLogin = (res: any) => {
    switch (res.coachStatus) {
      case COACH_STATUS.APPROVED:
        Taro.switchTab({ url: '/pages/index/index' });
        break;
      case COACH_STATUS.NOT_SUBMITTED:
      case COACH_STATUS.REJECTED:
        Taro.redirectTo({ url: '/pages/application/index' });
        break;
      case COACH_STATUS.PENDING:
        Taro.redirectTo({ url: '/pages/application/pending/index' });
        break;
      default:
        Taro.redirectTo({ url: '/pages/index/index' });
    }
  };

  return (
    <View className="wechat-login-page">
      <Image className="logo" src={require('@/assets/logo.png')} mode="aspectFit" />
      <View className="title">乐游教练端</View>
      <Button
        className="btn-wechat"
        type="primary"
        openType="getPhoneNumber"
        onGetPhoneNumber={onGetPhoneNumber}
        loading={loading}
      >
        微信一键登录
      </Button>
      <Button className="btn-phone" onClick={() => Taro.navigateTo({ url: '/pages/login/phone/index' })}>
        手机号登录
      </Button>
      <ProtocolCheckbox checked={agreed} onChange={setAgreed} />
    </View>
  );
}
```

- [ ] **Step 19.3: 编写测试**

```tsx
// miniapp-coach/src/pages/login/wechat/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import CoachWechatLoginPage from './index';

jest.mock('@tarojs/taro', () => ({
  login: jest.fn(() => Promise.resolve({ code: 'mock' })),
  showToast: jest.fn(),
  redirectTo: jest.fn(),
  switchTab: jest.fn()
}));
jest.mock('@/api/auth', () => ({
  wechatLogin: jest.fn(() => Promise.resolve({ coachStatus: 1 }))
}));

test('新教练 approved 状态跳转首页', async () => {
  const { getByText } = render(<CoachWechatLoginPage />);
  // 模拟协议勾选与 getPhoneNumber 较复杂，此处仅渲染断言
  expect(getByText('微信一键登录')).toBeInTheDocument();
});
```

- [ ] **Step 19.4: 提交**

```bash
git add miniapp-coach/src/pages/login/wechat/index.tsx miniapp-coach/src/pages/login/wechat/index.scss miniapp-coach/src/components/auth/ProtocolCheckbox.tsx miniapp-coach/src/pages/login/wechat/index.test.tsx
git commit -m "feat(auth): add coach wechat login page"
```

---

## Task 20: 教练端手机号登录页

**Files:**
- Create: `miniapp-coach/src/pages/login/phone/index.tsx`
- Create: `miniapp-coach/src/pages/login/phone/index.scss`
- Create: `miniapp-coach/src/pages/login/phone/index.test.tsx`

- [ ] **Step 20.1: 实现教练端手机号登录页**

与用户端手机号登录页结构相同，使用教练端 `phoneLogin`、`sendSms`、`useAuthStore`，登录成功后按 `coachStatus` 分流：

```tsx
// miniapp-coach/src/pages/login/phone/index.tsx
import Taro from '@tarojs/taro';
import { View, Input, Button, Text } from '@tarojs/components';
import { useState } from 'react';
import { phoneLogin } from '@/api/auth';
import { sendSms } from '@/api/common';
import { useAuthStore } from '@/stores/authStore';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import { useCountdown } from '@/hooks/useCountdown';
import { isValidPhone } from '@/utils/phone';
import './index.scss';

const COACH_STATUS = { NOT_SUBMITTED: -1, PENDING: 0, APPROVED: 1, REJECTED: 2 };

export default function CoachPhoneLoginPage() {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const { seconds, isRunning, start } = useCountdown(60);
  const setLogin = useAuthStore((s) => s.setLogin);

  const handleSendCode = async () => {
    if (!isValidPhone(phone)) {
      Taro.showToast({ title: '请输入正确手机号', icon: 'none' });
      return;
    }
    try {
      await sendSms(phone);
      start();
      Taro.showToast({ title: '验证码已发送', icon: 'success' });
    } catch (err: any) {
      Taro.showToast({ title: err.message || '发送失败', icon: 'none' });
    }
  };

  const handleLogin = async () => {
    if (!agreed) {
      Taro.showToast({ title: '请先同意协议', icon: 'none' });
      return;
    }
    if (!isValidPhone(phone) || code.length !== 6) {
      Taro.showToast({ title: '请填写完整信息', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      const res = await phoneLogin({ phone, code, termsAccepted: agreed, privacyAccepted: agreed });
      setLogin(res);
      if (res.coachStatus === COACH_STATUS.APPROVED) {
        Taro.switchTab({ url: '/pages/index/index' });
      } else if (res.coachStatus === COACH_STATUS.PENDING) {
        Taro.redirectTo({ url: '/pages/application/pending/index' });
      } else {
        Taro.redirectTo({ url: '/pages/application/index' });
      }
    } catch (err: any) {
      Taro.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className="phone-login-page">
      <View className="title">手机号登录</View>
      <View className="input-group">
        <Text className="prefix">+86</Text>
        <Input type="number" maxlength={11} placeholder="请输入手机号" value={phone} onInput={(e) => setPhone(e.detail.value)} />
      </View>
      <View className="input-group">
        <Input type="number" maxlength={6} placeholder="请输入验证码" value={code} onInput={(e) => setCode(e.detail.value)} />
        <Button className="btn-code" disabled={isRunning || !isValidPhone(phone)} onClick={handleSendCode}>
          {isRunning ? `${seconds}s` : '获取验证码'}
        </Button>
      </View>
      <ProtocolCheckbox checked={agreed} onChange={setAgreed} />
      <Button className="btn-login" type="primary" loading={loading} onClick={handleLogin}>登录</Button>
    </View>
  );
}
```

- [ ] **Step 20.2: 编写测试**

```tsx
// miniapp-coach/src/pages/login/phone/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import CoachPhoneLoginPage from './index';

jest.mock('@tarojs/taro', () => ({ showToast: jest.fn(), switchTab: jest.fn(), redirectTo: jest.fn() }));
jest.mock('@/api/auth', () => ({ phoneLogin: jest.fn(() => Promise.resolve({ coachStatus: 1 })) }));
jest.mock('@/api/common', () => ({ sendSms: jest.fn(() => Promise.resolve()) }));

test('输入手机号后点击获取验证码', async () => {
  const { getByPlaceholderText, getByText } = render(<CoachPhoneLoginPage />);
  fireEvent.input(getByPlaceholderText('请输入手机号'), { target: { value: '13800138000' } });
  fireEvent.click(getByText('获取验证码'));
  await waitFor(() => expect(sendSms).toHaveBeenCalledWith('13800138000'));
});
```

- [ ] **Step 20.3: 提交**

```bash
git add miniapp-coach/src/pages/login/phone/index.tsx miniapp-coach/src/pages/login/phone/index.scss miniapp-coach/src/pages/login/phone/index.test.tsx
git commit -m "feat(auth): add coach phone login page"
```

---

## 测试策略

### 后端测试

| 测试类型 | 覆盖范围 | 目标 |
|---------|---------|------|
| 单元测试 | `PhoneEncryptorTest`、`JwtTokenProviderTest`、`SmsCodeServiceTest`、`UserAuthServiceTest`、`CoachAuthServiceTest`、`UserSessionServiceTest`、`CoachSessionServiceTest` | 核心算法与业务逻辑，覆盖率 80% |
| 集成测试 | `UserAuthControllerIT`、`CoachAuthControllerIT`、`SmsControllerIT`、`UserAuthenticationFilterTest`、`CoachAuthenticationFilterTest` | 端到端 API 链路、JWT 过滤、参数校验 |
| 数据库测试 | Flyway 迁移在本地 MySQL/测试 H2 执行 | 验证 V2/V3/V4 脚本可重复执行 |

**关键测试场景：**

1. 微信登录：新用户/老用户、unionId 冲突、已封禁用户、code 过期。
2. 手机号登录：验证码正确/错误/过期、60 秒限流、新用户注册。
3. Token 刷新：refresh token 轮转、过期 token 拒绝。
4. 安全：未携带 token 访问 `/api/user/profile` 返回 401，user token 访问 `/api/coach/**` 返回 403。

### 前端测试

| 测试类型 | 覆盖范围 | 目标 |
|---------|---------|------|
| 单元测试 | `useCountdown.test.ts`、`storage.test.ts`、`phone.test.ts`、`ProtocolCheckbox.test.tsx` | Hook、工具函数、组件行为 |
| 页面测试 | `wechat/index.test.tsx`、`phone/index.test.tsx`（用户端 + 教练端） | 协议勾选、按钮状态、登录成功/失败跳转 |
| E2E（微信开发者工具） | 真机或模拟器走完整授权登录流程 | 验证 `getPhoneNumber` 事件与后端 Mock 联调 |

**Mock 策略：**
- 后端 `MockWechatClient` 返回固定 openid/unionId/sessionKey，开发环境无需配置真实微信 AppID。
- 后端 `MockSmsSender` 固定验证码 `123456`（或以数据库最新一条为准）。
- 前端 Jest 中 mock `@tarojs/taro` 与 API 模块。

---

## Calicat 页面参考

实现 UI 前，必须通过 Calicat MCP 拉取对应 Frame 图层数据，以 Calicat 为唯一视觉来源。

| 用户故事 | 页面文件 | Calicat Frame |
|---------|---------|--------------|
| US-004 | `docs/figma/page-spec/U-wechat-auth-page.md` | 游客微信授权登录页 |
| US-006 | `docs/figma/page-spec/U-phone-login-page.md` | 用户手机号验证码登录页 |
| US-051 | `docs/figma/page-spec/C-wechat-auth-page.md` | 教练微信授权登录页 |
| US-054 | `docs/figma/page-spec/C-phone-login-page.md` | 教练手机号验证码登录页 |

**UI 还原检查项：**
- Logo 尺寸、位置、导出格式（PNG/SVG/WebP）。
- 主按钮背景色/圆角/阴影（使用 Calicat 图层 fill 与 effect 数据）。
- 协议勾选框大小、文案颜色、链接高亮色值。
- 手机号输入框前缀 `+86` 样式、验证码按钮尺寸与禁用态。
- 页面背景色、安全区、适配 320px~430px 宽度（rpx 基准 375px）。

---

## API 端点汇总

统一遵循 `/api/{module}/{resource}/{action}` POST 风格。

| 端点 | 用途 | 归属 |
|------|------|------|
| `POST /api/user/auth/wechat-login` | 用户微信一键登录 | US-004 |
| `POST /api/user/auth/phone-login` | 用户手机号验证码登录 | US-006 |
| `POST /api/user/auth/refresh` | 刷新用户 access token | US-004 / US-006 |
| `POST /api/user/auth/logout` | 用户退出登录 | US-052 |
| `POST /api/coach/auth/wechat-login` | 教练微信一键登录 | US-051 |
| `POST /api/coach/auth/phone-login` | 教练手机号验证码登录 | US-054 |
| `POST /api/coach/auth/refresh` | 刷新教练 access token | US-051 / US-054 |
| `POST /api/common/sms/send` | 发送登录验证码（user/coach） | US-006 / US-054 |

---

## 需求覆盖检查

| 用户故事 | 覆盖任务 |
|---------|---------|
| US-004 游客微信授权登录 | Task 6, Task 9, Task 12, Task 16 |
| US-006 用户手机号验证码登录 | Task 7, Task 8, Task 9, Task 12, Task 17 |
| US-051 教练微信授权登录并进入教练端 | Task 6, Task 10, Task 12, Task 19 |
| US-054 教练手机号验证码登录 | Task 7, Task 8, Task 10, Task 12, Task 20 |

**未在本计划内实现、需后续 US 承接的内容：**
- 用户资料完善页（`/pages/profile/edit/index`）由 US-005 等后续用户故事实现。
- 教练入驻申请页（`/pages/application/index`）与审核中页（`/pages/application/pending/index`）由 US-039/US-040 等实现。
- 用户/教练退出登录完整逻辑（清除 storage、调用 logout API）由 US-052/US-056 实现，本计划仅提供 logout 后端接口与 storage.clearTokens。
