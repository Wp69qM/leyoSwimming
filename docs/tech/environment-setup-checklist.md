# leyoSwimming 第一批开发环境准备清单

> **文档定位**：登录注册批次进入多 Agent 开发前，开发机器必须安装的环境、工具、外部账号。  
> **检查时间**：2026-08-05  
> **检查方式**：PowerShell 自动扫描 + 人工确认。

---

## 1. 环境检查结果（当前机器）

| # | 环境/工具 | 当前状态 | 版本 | 操作 |
|---|----------|---------|------|------|
| 1 | Node.js | 已安装 | v20.14.0 | 无需操作 |
| 2 | yarn | **未安装** | — | **必须安装** |
| 3 | Java JDK 21 | **未安装** | — | **必须安装** |
| 4 | Maven 3.9+ | **未安装** | — | **必须安装** |
| 5 | Docker Desktop | **未安装** | — | **强烈推荐安装** |
| 6 | Docker Compose | **未安装** | — | 随 Docker Desktop 安装 |
| 7 | Git | 已安装 | v2.53.0.windows.1 | 无需操作 |
| 8 | 微信开发者工具 | 待确认 | — | 必须安装 |
| 9 | VS Code / Trae / Cursor | 待确认 | — | 必须安装 |
| 10 | MySQL 客户端 | 待确认 | — | 推荐安装 |
| 11 | Redis 客户端 | 待确认 | — | 推荐安装 |

---

## 2. 必装环境（按安装顺序）

### 2.1 yarn

- **用途**：monorepo 包管理，前端三件套 + shared + tools 统一使用。
- **安装命令**：
  ```powershell
  npm install -g yarn
  ```
- **验证**：`yarn --version` 应输出 9.x。

### 2.2 Java JDK 21

- **用途**：Spring Boot 3.2 + Java 21 后端开发。
- **下载**：
  - [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
  - 或 [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)
- **环境变量**：
  - `JAVA_HOME` 指向 JDK 安装目录
  - `PATH` 添加 `%JAVA_HOME%\bin`
- **验证**：`java --version` 应输出 Java 21。

### 2.3 Maven 3.9+

- **用途**：Java 项目构建、依赖管理。
- **安装方式**：
  - Windows（Chocolatey）：`choco install maven`
  - 或下载二进制包解压并配置环境变量
- **环境变量**：
  - `MAVEN_HOME` 指向 Maven 安装目录
  - `PATH` 添加 `%MAVEN_HOME%\bin`
- **验证**：`mvn --version` 应输出 3.9.x 且 Java 版本为 21。

### 2.4 Docker Desktop

- **用途**：本地一键启动 MySQL 8 + Redis 7 + Backend + Nginx。
- **下载**：[Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
- **验证**：
  ```powershell
  docker --version
  docker-compose --version
  ```
- **启动后测试**：
  ```powershell
  cd deploy
  docker-compose up -d
  ```

### 2.5 微信开发者工具

- **用途**：小程序预览、调试、真机测试。
- **下载**：[微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
- **注意**：需要分别导入 `miniapp-user/` 和 `miniapp-coach/` 两个项目。

---

## 3. 外部服务账号（本批次必须）

| # | 服务 | 用途 | 状态 | 操作 |
|---|------|------|------|------|
| 1 | 微信公众平台 - 用户端小程序 | 获取 AppID / AppSecret | 待注册 | **必须注册**（第一版不认证，用体验版跑通） |
| 2 | 微信公众平台 - 教练端小程序 | 获取 AppID / AppSecret | 待注册 | **必须注册**（第一版不认证，用体验版跑通） |
| 3 | 短信服务（阿里云/腾讯云） | 发送登录验证码 | 待申请 | **必须**（可用测试/沙箱模式） |
| 4 | 对象存储 OSS/COS | 头像、证书图片上传 | 待申请 | 推荐（开发阶段可用后端本地存储替代） |
| 5 | 微信开放平台 | user/coach union_id 打通 | 待申请 | 可选（第一版不做账号打通） |
| 6 | 微信支付 | 后续支付订单 | 本批次不需要 | 不申请（体验版无法测试真实支付） |

### 3.1 微信小程序申请（第一版不认证，用体验版）

1. 访问 [微信公众平台](https://mp.weixin.qq.com/)。
2. 注册两个小程序：
   - 一个用于用户端（学员）
   - 一个用于教练端
3. **第一版不需要企业认证**，直接以对应主体（个人/企业）注册即可；用**体验版**跑全流程。
4. 进入「开发管理」→「开发设置」，获取：
   - AppID
   - AppSecret
5. 在后台「成员管理」中添加体验成员（每个小程序最多 100 人），体验成员可扫码使用体验版。
6. 将 AppID / AppSecret 填入 `deploy/.env`（复制 `.env.example` 后修改）。

> **注意**：体验版不支持微信支付真实扣款。本批次登录注册流程不涉及支付，后续支付功能需在正式上线（企业认证后）再接入。

### 3.2 短信服务申请

- 推荐阿里云短信：申请签名、模板（验证码模板）。
- 本批次只需一个登录验证码模板。
- 开发阶段可使用短信服务提供的沙箱/测试模式，避免真实扣费。

### 3.3 对象存储（可选但推荐）

- 头像、证书图片需要上传存储。
- 开发阶段可先用后端本地存储或 MinIO 替代。

---

## 4. 环境配置步骤

### Step 1：安装上述所有环境

按 2.1 → 2.5 顺序安装，并验证版本。

### Step 2：配置环境变量

```powershell
cd deploy
copy .env.example .env
# 用编辑器打开 .env，填写真实值
```

### Step 3：启动本地基础设施

```powershell
cd deploy
docker-compose up -d
```

预期结果：
- MySQL 运行在 `localhost:3306`
- Redis 运行在 `localhost:6379`
- Backend 运行在 `localhost:8080`
- Nginx 运行在 `localhost:80`

### Step 4：验证数据库连接

使用 MySQL 客户端连接：

```
Host: localhost:3306
User: leyo
Password: leyo_dev
Database: leyo_dev
```

或使用命令行：

```powershell
docker exec -it leyo-mysql mysql -uleyo -pleyo_dev leyo_dev
```

### Step 5：验证 Redis 连接

```powershell
docker exec -it leyo-redis redis-cli ping
# 预期输出：PONG
```

---

## 5. 开发前最终确认清单

- [ ] Node.js 20 LTS 已安装
- [ ] yarn 9.x 已安装
- [ ] Java JDK 21 已安装
- [ ] Maven 3.9+ 已安装
- [ ] Docker Desktop 已安装并可启动
- [ ] 微信开发者工具已安装
- [ ] 用户端小程序已注册，AppID / AppSecret 已获取（**第一版不认证**）
- [ ] 教练端小程序已注册，AppID / AppSecret 已获取（**第一版不认证**）
- [ ] 已添加核心体验成员到两个小程序后台（用于体验版测试）
- [ ] 短信服务账号已申请（或已配置沙箱模式）
- [ ] `deploy/.env` 已配置（复制自 `.env.example`）
- [ ] `docker-compose up -d` 可正常启动 MySQL + Redis
- [ ] 已阅读 [multi-agent-dev-guide.md](./multi-agent-dev-guide.md)
- [ ] 已阅读各代码目录 `AGENT.md`
- [ ] 用户已整理并确认第一批页面清单

---

## 6. 后续动作

完成上述环境准备后，即可进入多 Agent 并行开发：

1. 用户整理并提供第一批页面清单。
2. 主 Agent 根据清单拆解 Task，分配给各端 Sub-Agent。
3. 后端 Agent 优先产出/冻结 OpenAPI v1.0.0。
4. 四端 Agent 并行进入 TDD 实现。
5. 每日 17:00 主 Agent 组织联调，更新 `docs/tech/dev-log/`。

---

## 7. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-08-05 | AI Agent | 初稿：环境检查结果、安装步骤、外部账号、确认清单 |
