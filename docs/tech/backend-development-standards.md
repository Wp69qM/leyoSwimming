# 后端开发规范

> **适用范围**：`backend/`
> **技术栈**：Spring Boot 3.2 + Java 21 + Maven + MyBatis-Plus
> **目标**：构建分层清晰、可测试、安全、易维护的后端服务。

---

## 1. 核心原则

### 1.1 分层架构

后端采用经典四层架构，每层职责单一：

```
controller/     # 接收请求、参数校验、返回响应
service/        # 业务逻辑编排
repository/     # 数据访问（MyBatis-Plus Mapper）
entity/         # 数据库实体
dto/            # 请求/响应数据传输对象
vo/             # 返回给前端的视图对象
mapper/         # MapStruct 对象转换
enums/          # 枚举常量
exception/      # 异常与全局处理
config/         # 配置类
security/       # JWT、权限相关
util/           # 纯工具类
```

**约束**：
- Controller 只负责接收和返回，不写业务逻辑。
- Service 只负责业务编排，不直接操作 SQL。
- Repository 只负责数据访问，不写业务判断。

### 1.2 面向接口编程

- Service 层建议先定义接口，再写实现。
- 便于单元测试时 Mock 依赖。

```java
public interface UserService {
    UserVO getById(Long userId);
}

@Service
public class UserServiceImpl implements UserService {
    // ...
}
```

### 1.3 构造函数注入

- 统一使用构造函数注入，禁止字段注入（`@Autowired` on field）。

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

---

## 2. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserService`、`OrderRepository` |
| 方法/字段 | camelCase | `findById`、`createdAt` |
| 常量 | SCREAMING_SNAKE_CASE | `MAX_RETRY_TIMES` |
| 包名 | 全小写 | `com.leyoswimming.user` |
| 数据库表 | 小写下划线 | `user_profile`、`coach_application` |
| 布尔变量 | is/has/should/can 前缀 | `isActive`、`hasPermission` |

---

## 3. API 规范

详细规则见 [api-convention.md](./api-convention.md)。核心要点：

- 统一使用 `POST`。
- URL 结构：`/api/{模块}/{资源单数}/{动作}`。
- 禁止 URL 路径参数，`{id}` 放到 JSON body 中。
- ID 字段命名使用 lowerCamelCase，如 `applicationId`、`coachId`。
- 统一响应体 `ApiResponse<T>`。

```java
@PostMapping("/api/user/profile/detail")
public ApiResponse<UserVO> detail(@RequestBody @Valid UserDetailRequest request) {
    return ApiResponse.ok(userService.getById(request.getUserId()));
}
```

---

## 4. 数据传输对象

### 4.1 DTO / VO / Entity 分离

| 对象 | 用途 | 位置 |
|------|------|------|
| Request DTO | 接收前端参数 | `dto.request` |
| Response DTO | 返回给前端的数据 | `dto.response` |
| VO | 视图层最终返回对象 | `vo/` |
| Entity | 数据库映射 | `entity/` |

### 4.2 使用 Record（推荐）

对于只读 DTO/VO，优先使用 Java 21 `record`：

```java
public record UserVO(Long id, String nickname, String avatarUrl) {}
```

### 4.3 参数校验

- 请求 DTO 使用 Jakarta Validation 注解：`@NotBlank`、`@Size`、`@Min`、`@Max`、`@Pattern`。
- Controller 方法参数加 `@Valid` 或 `@Validated`。
- 复杂业务规则校验放到 Service 层。

```java
public record SendPhoneCodeRequest(
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @NotNull AppType appType
) {}
```

---

## 5. 统一响应与异常处理

### 5.1 统一响应体

```java
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

### 5.2 错误码

- 错误码使用枚举 `ErrorCode`，集中管理。
- 业务错误码范围：1000 ~ 9999。
- 系统错误码范围：10000 以上。

```java
public enum ErrorCode {
    USER_NOT_FOUND(1001, "用户不存在"),
    INVALID_PHONE(1002, "手机号格式错误");

    private final int code;
    private final String message;
}
```

### 5.3 异常处理

- 业务异常使用 unchecked exception，继承 `RuntimeException`。
- 全局异常处理器 `GlobalExceptionHandler` 统一捕获并转换为 `ApiResponse`。
- 禁止在 Controller/Service 中直接 `try-catch` 吞掉异常。

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

---

## 6. 数据库与 ORM

### 6.1 实体规范

- 实体类使用 `Long` 主键，`@TableId(type = IdType.AUTO)`。
- 时间字段使用 `LocalDateTime`。
- 删除标志使用 `deleted` 字段，配合 MyBatis-Plus 逻辑删除。
- 实体字段与数据库列名一致，使用驼峰命名，MyBatis-Plus 自动转下划线。

```java
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
```

### 6.2 SQL 规范

- 优先使用 MyBatis-Plus 提供的 Lambda 查询 API。
- 复杂 SQL 使用 XML Mapper，禁止在 Java 代码中拼接 SQL 字符串。
- 所有查询必须带条件限制，禁止无 `WHERE` 的全表查询。
- 分页查询统一使用 MyBatis-Plus `Page`。

### 6.3 数据库迁移

- 使用 Flyway 管理数据库变更。
- 脚本放在 `src/main/resources/db/migration/`。
- 命名规范：`V{版本号}__{描述}.sql`，如 `V1__init_schema.sql`。

---

## 7. 安全规范

### 7.1 凭证管理

- **禁止在代码中硬编码** AppID、AppSecret、JWT Secret、数据库密码等。
- 统一从环境变量或 `application.yml` 中读取。
- `deploy/.env` 已加入 `.gitignore`，不会提交。

### 7.2 输入校验

- 所有外部输入必须校验：请求参数、路径、文件、第三方回调。
- 不信任任何客户端数据。

### 7.3 JWT 安全

- JWT Secret 生产环境必须使用 >= 256 bit 的强随机字符串。
- Token 过期时间合理设置：access token 15 分钟 ~ 2 小时，refresh token 7 ~ 30 天。
- 登出时将 token 加入 Redis 黑名单。

### 7.4 敏感数据

- 日志中禁止输出密码、Token、手机号完整号码等敏感信息。
- 错误响应不暴露内部堆栈或 SQL 细节。

---

## 8. 日志规范

- 使用 SLF4J + Logback。
- 日志级别使用规范：
  - `ERROR`：系统异常、需要人工介入
  - `WARN`：业务异常、可预期错误
  - `INFO`：业务流程关键节点
  - `DEBUG`：调试信息
- 日志内容包含上下文：用户 ID、请求 ID、操作对象 ID 等。

```java
log.info("User login success: userId={}", userId);
log.warn("Phone code send too frequently: phone={}", phone);
log.error("Database query failed: userId={}", userId, exception);
```

---

## 9. 测试规范

### 9.1 测试驱动开发（TDD）

- 新功能优先写测试，再写实现。
- 测试结构使用 Arrange-Act-Assert。

### 9.2 测试分层

| 测试类型 | 框架 | 覆盖目标 |
|----------|------|----------|
| 单元测试 | JUnit 5 + Mockito + AssertJ | Service、Utils |
| 集成测试 | Spring Boot Test + Testcontainers | Repository、Controller |

### 9.3 覆盖率

- 核心业务逻辑覆盖率目标 **80%+**。
- 使用 JaCoCo 生成报告。

---

## 10. 代码质量

### 10.1 现代 Java 特性

- 使用 `record` 定义 DTO/VO。
- 使用 `Optional` 处理可能为空的结果，禁止裸 `get()`。
- 使用 Stream 做数据转换，但避免过度复杂。
- 使用 `var` 提高局部变量可读性。

### 10.2 不可变性

- DTO/VO 优先不可变。
- 返回集合时使用 `List.copyOf()` 或不可变集合。

### 10.3 注释

- 不写「做了什么」的注释，写「为什么」。
- 复杂算法、业务规则必须注释。

---

## 11. 禁止事项

- ❌ 在代码中硬编码密钥、密码、Token
- ❌ 字段注入（`@Autowired` on field）
- ❌ 拼接 SQL 字符串
- ❌ Controller 中写业务逻辑
- ❌ Service 中直接操作 SQL
- ❌ 吞掉异常不处理
- ❌ 返回裸异常信息给前端
- ❌ 日志中打印敏感信息
- ❌ 无 WHERE 条件的全表查询
- ❌ 使用 `null` 返回表示「未找到」，应使用 `Optional` 或抛业务异常

---

## 12. 检查清单

提交代码前确认：

- [ ] 分层清晰，职责单一
- [ ] 使用构造函数注入
- [ ] DTO/VO/Entity 分离
- [ ] 请求参数已校验
- [ ] 异常已统一处理
- [ ] 无硬编码敏感信息
- [ ] 数据库操作使用 MyBatis-Plus 或参数化 XML
- [ ] 核心逻辑有单元测试
- [ ] 日志不泄露敏感信息

---

## 13. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-09 | PM | 初版：定义后端开发规范 |
