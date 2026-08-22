# 本地后端与数据库连接指南

> **文档定位**：本地开发环境启动后端服务，并通过 DBeaver 连接 MySQL 数据库的操作手册。  
> **目标读者**：需要在本地调试后端、查看数据库的开发人员。  
> **适用环境**：Windows + PowerShell + MySQL 8 + DBeaver。

---

## 1. 环境前提

- MySQL 8.0+ 已运行（本地或 Docker）
- Redis 7+ 已运行
- JDK 21 已安装
- Maven Wrapper（`backend/mvnw.cmd`）可用
- DBeaver（任意版本，建议 23+）已安装

### 1.1 启动 MySQL

如果你使用本地安装的 MySQL 服务，需要**管理员权限**的 PowerShell：

1. 开始菜单搜索 **PowerShell**，右键选择 **以管理员身份运行**
2. 执行：

```powershell
net start MySQL80
```

> 服务名可能因安装版本不同而变化，常见名称为 `MySQL`、`MySQL80`、`mysql`。如果 `MySQL80` 报错，可用 `services.msc` 查看实际服务名。
>
> 如果提示 `发生系统错误 5。拒绝访问。`，说明当前终端不是管理员权限，请重新以管理员身份打开 PowerShell。

如果你使用 Docker：

```powershell
docker run -d `
  --name leyo-mysql `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=leyo_dev `
  -e MYSQL_USER=leyo `
  -e MYSQL_PASSWORD=leyo_dev `
  mysql:8.0
```

### 1.2 启动 Redis

如果你使用本地 Redis 服务，需要**管理员权限**的 PowerShell：

1. 开始菜单搜索 **PowerShell**，右键选择 **以管理员身份运行**
2. 执行：

```powershell
net start Redis
```

> 服务名可能为 `Redis`、`redis` 或 `RedisServer`，可在 `services.msc` 中确认。
>
> 如果提示 `发生系统错误 5。拒绝访问。`，说明当前终端不是管理员权限，请重新以管理员身份打开 PowerShell。

如果你使用 Docker：

```powershell
docker run -d `
  --name leyo-redis `
  -p 6379:6379 `
  redis:7 `
  redis-server --requirepass leyo1234
```

### 1.3 验证 MySQL 与 Redis 是否启动

```powershell
# 检查 MySQL 进程
Get-Process mysqld

# 检查 Redis 进程
Get-Process redis-server

# 测试 MySQL 连接（需要 mysql 客户端在 PATH 中）
mysql -h localhost -P 3306 -u leyo -pleyo_dev -e "SELECT 1;"

# 测试 Redis 连接（需要 redis-cli 在 PATH 中）
redis-cli -h localhost -p 6379 -a leyo1234 ping
```

预期结果：

- `Get-Process` 返回进程信息
- MySQL 返回 `1`
- Redis 返回 `PONG`

如果上述任一命令失败，请先排查对应服务是否启动、端口是否正确、密码是否匹配 `deploy/.env` 中的配置。

### 1.4 Redis 无密码导致后端启动失败的修复

如果后端启动日志出现：

```
ERR Client sent AUTH, but no password is set.
```

说明 Redis 已运行但没有设置密码，需要设置为 `leyo1234`（与 `deploy/.env` 和 `ai-service/.env` 保持一致）。

#### 临时设置（Redis 重启后失效）

```powershell
redis-cli -h localhost -p 6379 CONFIG SET requirepass leyo1234
```

设置后重新验证：

```powershell
redis-cli -h localhost -p 6379 -a leyo1234 ping
```

#### 持久化设置（推荐）

Windows 服务版 Redis 配置文件通常位于：

```
C:\Program Files\Redis\redis.windows-service.conf
```

1. 以管理员身份打开文本编辑器，编辑上述文件
2. 找到 `# requirepass foobared`
3. 改为：

```conf
requirepass leyo1234
```

4. 保存后重启 Redis 服务：

```powershell
net stop Redis
net start Redis
```

> 修改 `C:\Program Files\Redis\` 下文件需要管理员权限。

---

## 2. 启动后端服务

### 2.1 加载环境变量并启动 Spring Boot

后端依赖 `deploy/.env` 中的数据库、Redis、JWT 等配置。**必须在启动前先加载环境变量，否则会出现 `Could not resolve placeholder 'JWT_SECRET'` 错误。**

在 PowerShell 中执行（假设项目根目录为 `D:\AI Agent\leyoSwimming`，请按实际路径修改）：

```powershell
# 切换到项目根目录
cd "D:\AI Agent\leyoSwimming"

# 加载环境变量
Get-Content .\deploy\.env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $parts = $line.Split('=', 2)
        [System.Environment]::SetEnvironmentVariable(
            $parts[0].Trim(),
            $parts[1].Trim(),
            'Process'
        )
    }
}

# 验证关键环境变量是否已加载
Write-Host "JWT_SECRET=" $env:JWT_SECRET
Write-Host "MYSQL_HOST=" $env:MYSQL_HOST
Write-Host "REDIS_PASSWORD=" $env:REDIS_PASSWORD

# 启动 Spring Boot
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> **注意**：加载环境变量和启动 Spring Boot 必须在**同一个 PowerShell 窗口**中执行。如果中间打开了新窗口，需要重新加载环境变量。

### 2.2 验证启动

- 日志出现 `Started LeyoSwimmingApplication`
- 访问 `http://localhost:8080/api/user/auth/wechat-login`（GET/POST 均可），返回 401/200001 即表示服务已启动
- Flyway 迁移会在首次启动时自动执行

---

## 3. 启动 AI 服务

### 3.1 环境前提

- Python 3.11+ 已安装
- AI 服务虚拟环境已初始化（`ai-service/.venv`）
- `ai-service/.env` 已配置 Redis、LLM API Key 等

### 3.2 启动 FastAPI 服务

```powershell
cd ai-service
.venv\Scripts\activate
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 3.3 验证启动

- 日志出现 `Uvicorn running on http://0.0.0.0:8000`
- 访问 `http://localhost:8000/docs` 可看到 Swagger 文档
- 访问 `http://localhost:8000/health` 返回服务健康状态

### 3.4 常用 AI 服务配置

`ai-service/.env` 关键项：

| 配置项 | 说明 | 示例 |
|---|---|---|
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `localhost` / `6379` / `leyo1234` |
| `INTERNAL_API_TOKEN` | 内部接口鉴权令牌，需与 `deploy/.env` 中 `INTERNAL_API_TOKEN` 保持一致 | `dev-internal-token-must-be-overridden-in-production` |
| `LLM_MODEL` | 大模型名称 | `deepseek-chat` |
| `LLM_BASE_URL` | 大模型 API 地址 | `https://api.deepseek.com/v1` |
| `LLM_API_KEY` | 大模型 API Key | `sk-xxxxxxxx` |

---

## 4. 启动前端项目

### 4.1 用户端小程序（miniapp-user）

#### H5 开发模式

```powershell
yarn workspace @leyo/miniapp-user dev:h5
```

启动后访问：

- 本机：`http://localhost:10086/`
- 局域网：`http://192.168.1.5:10086/`（IP 以实际为准）

#### 微信小程序开发模式

```powershell
yarn workspace @leyo/miniapp-user dev:weapp
```

编译完成后用微信开发者工具打开项目根目录（`D:\AI Agent\leyoSwimming\miniapp-user`），不是 `dist/` 目录。

### 4.2 教练端小程序（miniapp-coach）

#### H5 开发模式

```powershell
yarn workspace @leyo/miniapp-coach dev:h5
```

启动后访问：

- 本机：`http://localhost:10087/`
- 局域网：`http://192.168.1.5:10087/`（IP 以实际为准）

#### 微信小程序开发模式

```powershell
yarn workspace @leyo/miniapp-coach dev:weapp
```

### 4.3 管理后台（web-admin）

```powershell
yarn workspace @leyo/web-admin dev
```

启动后访问：

- 本机：`http://localhost:10088/`
- 局域网：`http://192.168.1.5:10088/`（IP 以实际为准）

### 4.4 前端项目通用说明

- 默认端口：
  - 用户端 H5：`10086`
  - 教练端 H5：`10087`
  - 管理后台：`10088`
- 各前端项目已配置 API 代理，开发时会将 `/api` 请求转发到 `http://127.0.0.1:8080`
- 用户端 H5 默认页面：`/pages/ai-assistant/index`

---

## 5. DBeaver 连接 MySQL

### 5.1 连接信息

| 配置项 | 值 |
|---|---|
| 主机 | `localhost` |
| 端口 | `3306` |
| 数据库 | `leyo_dev` |
| 用户名 | `leyo` |
| 密码 | `leyo_dev` |

### 5.2 驱动配置

MySQL 8 默认使用 `caching_sha2_password` 认证，DBeaver 若驱动过旧会报错，需升级为 `mysql-connector-j-8.0.33.jar`。

**驱动 jar 路径：**
```
D:\AI Agent\leyoSwimming\tmp\mysql-connector-j-8.0.33.jar
```

**DBeaver 驱动设置：**
1. 打开 **数据库 → 驱动管理器**
2. 双击 **MySQL**
3. 切换到 **库** 标签页
4. 删除所有旧 jar（如 `mysql-connector-java-5.x.jar`）
5. 点击 **添加文件**，选择上面的 `mysql-connector-j-8.0.33.jar`
6. 切换到 **设置** 标签页，确认：
   - 驱动类名：`com.mysql.cj.jdbc.Driver`
   - URL 模板：`jdbc:mysql://{host}:{port}/{database}`
7. 点击 **应用并关闭**

### 5.3 连接属性

在连接编辑 → **驱动属性** 中设置：

| 属性名 | 值 | 说明 |
|---|---|---|
| `allowPublicKeyRetrieval` | `true` | 允许公钥检索，解决 MySQL 8 认证问题 |
| `useSSL` | `false` | 本地开发关闭 SSL |
| `serverTimezone` | `Asia/Shanghai` | 时区对齐 |

或直接使用完整 URL：
```
jdbc:mysql://localhost:3306/leyo_dev?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

---

## 6. 常见问题

### 6.1 `Unable to load authentication plugin 'caching_sha2_password'`

**原因**：DBeaver 内置 MySQL 驱动版本太旧。  
**解决**：按 5.2 节升级为 `mysql-connector-j-8.0.33.jar`。

### 6.2 `Public Key Retrieval is not allowed`

**原因**：MySQL 8 默认要求 RSA 公钥交换，DBeaver 未开启。  
**解决**：在驱动属性中添加 `allowPublicKeyRetrieval=true`。

### 6.3 `can't load driver class 'com.mysql.cj.jdbc.Driver'`

**原因**：新驱动 jar 未生效，或旧 jar 仍有残留。  
**解决**：
1. 完全退出并重启 DBeaver
2. 删除 MySQL 驱动库中所有旧 jar
3. 仅保留 `mysql-connector-j-8.0.33.jar`
4. 确认驱动类名为 `com.mysql.cj.jdbc.Driver`

### 6.4 后端启动提示 Redis 连接失败

**原因**：`deploy/.env` 中的 Redis 配置未正确加载，或 Redis 未启动。  
**解决**：
1. 确认 Redis 已运行（默认端口 `6379`）
2. 检查 `.env` 中 `SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT`、`SPRING_DATA_REDIS_PASSWORD` 是否正确
3. 重新执行 2.1 加载环境变量后再启动

---

## 7. 常用验证命令

```powershell
# 测试后端是否启动
Invoke-RestMethod -Uri http://localhost:8080/api/user/auth/wechat-login -Method POST

# 查看后端日志（若在后台运行）
Get-Content backend\target\spring.log -Wait -Tail 50
```

---

## 8. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|---|---|---|---|
| v1.0 | 2026-08-17 | AI Agent | 初稿：后端启动、DBeaver 连接、常见问题 |
| v1.1 | 2026-08-20 | AI Agent | 新增 AI 服务启动、前端三个项目启动命令、MySQL 与 Redis 启动及验证命令、管理员权限说明、环境变量加载与启动合并为一键命令并加入验证输出、修复环境变量加载脚本兼容性、补充 Redis 无密码修复方案 |
