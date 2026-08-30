# leyoSwimming 生产环境初次部署及升级手册

> **文档定位**：第二批次开发完成并验证通过后，将 leyoSwimming 前后端部署到一台全新服务器的操作手册。  
> **目标读者**：运维人员 / 开发工程师。  
> **部署范围**：后端服务（Spring Boot）、AI 服务（leyo-ai-service）、管理后台 Web 端、用户端 H5、教练端 H5、MySQL、Redis。本阶段前端全部以 H5 形式部署到 Nginx，不依赖微信小程序，也无需申请微信小程序账号。  
> **版本**：v1.0  
> **日期**：2026-08-15

---

## 目录

1. [部署架构概览](#1-部署架构概览)
2. [前置准备](#2-前置准备)
3. [服务器环境安装](#3-服务器环境安装)
4. [初次部署步骤](#4-初次部署步骤)
5. [H5 端发布与验证](#5-h5-端发布与验证)
6. [验证方法](#6-验证方法)
7. [日常运维](#7-日常运维)
8. [后续升级部署](#8-后续升级部署)
9. [常见问题](#9-常见问题)
10. [安全检查清单](#10-安全检查清单)
11. [附录：生产环境 docker-compose.yml 示例](#11-附录生产环境-docker-composeyml-示例)

---

## 1. 部署架构概览

```
用户/管理员
    │
    ▼
服务器 IP 或域名（HTTP）
    │
    ▼
Nginx（容器或宿主机）
    ├── /api/*  ───────────► 后端 Spring Boot（容器，端口 8080）
    ├── /admin/* ──────────► 管理后台静态资源（dist）
    ├── /h5/user/* ────────► 用户端 H5（dist）
    ├── /h5/coach/* ───────► 教练端 H5（dist）
    └── /uploads/* ────────► 本地文件存储
                                │
                                ▼
                        AI 服务 leyo-ai-service（容器，端口 8000）
                                │
                                └── 回调 Java 内部接口 /api/internal/ai/*
    │
    ├── MySQL 8.0（容器，持久化卷）
    └── Redis 7（容器，持久化卷）
```

**技术栈**

| 模块 | 技术 | 说明 |
|------|------|------|
| 后端 | Spring Boot 3.2 + Java 21 + Maven | 提供 `/api/**` 接口 |
| AI 服务 | Python 3.11 + FastAPI + LangChain + Uvicorn | 提供 AI 推荐能力，Java 后端通过内部接口调用 |
| 管理后台 | Vue 3 + Vite + TypeScript + Element Plus | 构建为静态页面，Nginx 托管在 `/admin/` |
| 用户端 H5 | Taro 4 + React 18 + TypeScript | 构建为 H5，Nginx 托管在 `/h5/user/` |
| 教练端 H5 | Taro 4 + React 18 + TypeScript | 构建为 H5，Nginx 托管在 `/h5/coach/` |
| 数据库 | MySQL 8.0 | 主数据存储 |
| 缓存 | Redis 7 | 会话、缓存、分布式锁 |
| 反向代理 | Nginx | HTTP、静态资源、负载均衡 |

---

## 2. 前置准备

### 2.1 服务器要求

- **系统**：Ubuntu 22.04 LTS / Debian 12 / CentOS 7+（推荐 Ubuntu 22.04 LTS）
- **配置**：至少 2 核 CPU / 4GB 内存 / 50GB 磁盘（生产建议 4 核 8GB）
- **网络**：固定公网 IP，安全组/防火墙开放必要端口
- **域名/IP**：本阶段使用服务器 IP 直接访问即可；后续如需绑定域名，替换 `DOMAIN` 配置即可

### 2.2 必须开放的端口

| 端口 | 用途 | 开放对象 |
|------|------|----------|
| 22 | SSH | 仅管理员 IP |
| 80 | HTTP | 公网 |
| 3306 | MySQL | 建议仅内网/本机，不暴露公网 |
| 6379 | Redis | 建议仅内网/本机，不暴露公网 |

> **安全建议**：生产环境不要将 3306、6379 暴露到公网。若必须远程管理，请使用 SSH 隧道或 VPN。

### 2.3 外部账号与资料

| 项目 | 用途 | 获取方式 | 本阶段是否必填 |
|------|------|----------|----------------|
| LLM API Key | AI 服务调用大模型 | DeepSeek / 通义千问 / Kimi 等 | **必填** |
| 服务器公网 IP | 访问 H5 和管理后台 | 云服务器控制台 | **必填** |
| 微信支付（MVP 后可接入） | 真实支付 | 微信支付商户平台 | 否 |

> **说明**：本阶段前端以 H5 形式部署，不依赖微信小程序账号，因此不需要小程序 AppID/AppSecret；图片使用本地文件存储，不需要 OSS；短信服务保持开发环境 Mock 方式，不需要短信平台账号。

---

## 3. 服务器环境安装

> **推荐策略**：服务器只负责运行容器，所有构建（jar、dist）在本地或 CI 环境完成后再上传。这样服务器无需安装 JDK、Node.js、Maven，减少攻击面和维护成本。

### 3.1 登录服务器

```bash
ssh root@<服务器公网IP>
```

### 3.2 更新系统

```bash
# Ubuntu / Debian
apt update && apt upgrade -y

# CentOS
yum update -y
```

### 3.3 安装 Docker 与 Docker Compose

```bash
# Ubuntu / Debian 推荐安装方式（使用官方仓库）
apt install -y ca-certificates curl gnupg lsb-release
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 验证
docker --version
docker compose version
```

### 3.4 启动 Docker 并设置开机自启

```bash
systemctl start docker
systemctl enable docker
```

### 3.5 创建部署目录

```bash
mkdir -p /opt/leyo-swimming
cd /opt/leyo-swimming
```

### 3.6 安装 Nginx（宿主机方式，可选）

如果使用宿主机 Nginx 做反向代理（推荐与容器 Nginx 二选一）：

```bash
# Ubuntu / Debian
apt install -y nginx

# 验证
nginx -v
systemctl start nginx
systemctl enable nginx
```

> 若选择使用容器化 Nginx，可跳过本步，后续在 `docker-compose.yml` 中一并启动。

---

## 4. 初次部署步骤

### 4.1 方式一：本地构建后上传（推荐）

#### 4.1.1 本地构建后端 jar 包

在开发机器上执行：

```bash
cd /path/to/leyoSwimming/backend
./mvnw clean package -DskipTests -B
```

构建产物：`backend/target/leyo-swimming-backend-0.1.0-SNAPSHOT.jar`

#### 4.1.2 本地构建 AI 服务 Docker 镜像

AI 服务基于 Python/FastAPI，推荐在本地构建 Docker 镜像后上传到服务器（或推送到私有镜像仓库）。

```bash
cd /path/to/leyoSwimming/ai-service

# 构建镜像（注意镜像标签建议带版本号，如 v1.0.0）
docker build -t leyo-ai-service:v1.0.0 .

# 方式 A：保存为 tar 文件后上传到服务器
docker save leyo-ai-service:v1.0.0 -o leyo-ai-service-v1.0.0.tar
scp leyo-ai-service-v1.0.0.tar root@<服务器IP>:/opt/leyo-swimming/

# 方式 B：推送到私有镜像仓库（推荐生产环境使用）
# docker tag leyo-ai-service:v1.0.0 your-registry.com/leyo/ai-service:v1.0.0
# docker push your-registry.com/leyo/ai-service:v1.0.0
```

如果采用方式 A，在服务器上加载镜像：

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming
docker load -i leyo-ai-service-v1.0.0.tar
```

> **说明**：AI 服务镜像不依赖 JDK/Node，生产环境通过 `image: leyo-ai-service:v1.0.0` 直接运行即可。后续升级时重新构建并加载新版本镜像，再重启容器。

#### 4.1.3 本地构建管理后台 dist

```bash
cd /path/to/leyoSwimming/web-admin
yarn install
yarn build
```

构建产物：`web-admin/dist/` 目录

#### 4.1.4 本地构建用户端和教练端 H5

本阶段用户端和教练端均以 H5 形式部署到 Nginx，不发布微信小程序。

构建前需要分别修改 H5 publicPath：

- `miniapp-user/config/index.js`：
  ```js
  h5: {
    publicPath: '/h5/user/',
    // ... 其他配置保持不变
  }
  ```

- `miniapp-coach/config/index.js`：
  ```js
  h5: {
    publicPath: '/h5/coach/',
    // ... 其他配置保持不变
  }
  ```

然后执行构建：

```bash
# 用户端 H5
cd /path/to/leyoSwimming/miniapp-user
yarn install
yarn build:h5
# 产物在 miniapp-user/dist/ 目录
mv dist h5-user-dist

# 教练端 H5
cd /path/to/leyoSwimming/miniapp-coach
yarn install
yarn build:h5
# 产物在 miniapp-coach/dist/ 目录
mv dist h5-coach-dist
```

#### 4.1.5 上传构建产物到服务器

```bash
# 上传 jar 包
scp backend/target/leyo-swimming-backend-0.1.0-SNAPSHOT.jar root@<服务器IP>:/opt/leyo-swimming/

# 上传 AI 服务镜像 tar（如已在服务器加载则可跳过）
scp ai-service/leyo-ai-service-v1.0.0.tar root@<服务器IP>:/opt/leyo-swimming/

# 上传管理后台静态资源
scp -r web-admin/dist root@<服务器IP>:/opt/leyo-swimming/web-admin-dist/

# 上传用户端 H5 静态资源
scp -r miniapp-user/h5-user-dist root@<服务器IP>:/opt/leyo-swimming/h5-user-dist/

# 上传教练端 H5 静态资源
scp -r miniapp-coach/h5-coach-dist root@<服务器IP>:/opt/leyo-swimming/h5-coach-dist/

# 上传部署配置（后续在服务器上维护）
scp -r deploy root@<服务器IP>:/opt/leyo-swimming/
```

#### 4.1.6 在服务器上准备生产环境配置

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming

# 复制环境变量模板
cd deploy
cp .env.example .env

# 编辑 .env，填入生产环境真实值
nano .env
```

`.env` 关键项示例：

```bash
# MySQL（必须修改默认密码）
MYSQL_ROOT_PASSWORD=YourStrongRootPassword
MYSQL_DATABASE=leyo_prod
MYSQL_USER=leyo
MYSQL_PASSWORD=YourStrongUserPassword

# Redis（生产环境必须设置密码）
REDIS_PASSWORD=YourStrongRedisPassword

# JWT（必须使用强随机字符串，长度 >= 32 字节）
JWT_SECRET=your-very-strong-random-secret-at-least-64-characters-long

# 手机号加密密钥（长度建议 32 字节，一旦设定请勿修改）
PHONE_ENCRYPTION_KEY=your-32-byte-encryption-key-here

# 身份证号加密密钥（长度建议 32 字节，一旦设定请勿修改）
ID_CARD_ENCRYPTION_KEY=your-32-byte-id-card-encryption-key-here

# AI 服务（必填）
# 当 AI 服务在宿主机直接运行时，指向宿主机 IP:8000
AI_SERVICE_BASE_URL=http://<服务器IP>:8000
INTERNAL_API_TOKEN=your-internal-api-token-at-least-32-characters

# LLM 配置（默认 DeepSeek，按实际情况填写）
LLM_MODEL=deepseek-chat
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=your-llm-api-key-here
LLM_TEMPERATURE=0.3
LLM_MAX_TOKENS=2048
LLM_TIMEOUT_SECONDS=30

# LangSmith 配置（可选，默认关闭）
LANGCHAIN_TRACING_V2=false
LANGCHAIN_API_KEY=
LANGCHAIN_PROJECT=leyoSwimming
LANGCHAIN_ENDPOINT=https://api.smith.langchain.com

# AI 服务运行配置
APP_NAME=leyo-ai-service
APP_ENV=production
HOST=0.0.0.0
PORT=8000
LOG_LEVEL=INFO

# 会话与限流
SESSION_MESSAGE_TTL_SECONDS=604800
SESSION_MAX_MESSAGES=20
RATE_LIMIT_USER_PER_MINUTE=30
RATE_LIMIT_IP_PER_MINUTE=60

# 可信代理层数（Nginx 前置填 1）
TRUSTED_PROXY_COUNT=1

# AI 服务 Mock 模式（生产环境必须设为 false）
ENABLE_MOCK_DATA=false

# 生产访问地址：使用服务器 IP 或域名
# 示例：DOMAIN=123.45.67.89  或  DOMAIN=your-domain.com
DOMAIN=123.45.67.89
```

#### 4.1.7 生产化改造 Nginx 配置

编辑 `deploy/nginx.conf`，替换为生产配置（示例）：

```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    sendfile on;
    keepalive_timeout 65;
    client_max_body_size 50M;

    # 管理后台静态资源缓存
    map $sent_http_content_type $expires {
        default                    off;
        text/html                  epoch;
        text/css                   max;
        application/javascript     max;
        ~image/                    max;
    }

    server {
        listen 80;
        server_name _;  # 使用 _ 可匹配任意域名或直接使用 IP

        # API 反向代理到后端
        location /api/ {
            proxy_pass http://leyo-backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }

        # 管理后台静态资源
        location /admin/ {
            alias /usr/share/nginx/html/admin/;
            try_files $uri $uri/ /admin/index.html;
            expires $expires;
        }

        # 用户端 H5
        location /h5/user/ {
            alias /usr/share/nginx/html/h5-user-dist/;
            try_files $uri $uri/ /h5/user/index.html;
            expires $expires;
        }

        # 教练端 H5
        location /h5/coach/ {
            alias /usr/share/nginx/html/h5-coach-dist/;
            try_files $uri $uri/ /h5/coach/index.html;
            expires $expires;
        }

        # 后端本地文件上传访问（如使用本地存储）
        location /uploads/ {
            alias /usr/share/nginx/html/uploads/;
            expires 30d;
        }

        location / {
            return 404;
        }
    }
}
```

> 注意：`server_name _` 表示接受通过 IP 或任意域名访问。后续如需绑定域名，改为 `server_name your-domain.com;` 即可。
>
> AI 服务不直接面向客户端，前端 `/api/ai-assistant/*` 请求由 Java 后端统一代理到 `leyo-ai-service:8000`，因此 Nginx 中 `/api/` 反向代理到后端即可，无需为 AI 服务单独配置 location。

#### 4.1.8 生产化改造 docker-compose.yml

当前 `deploy/docker-compose.yml` 为开发环境配置，生产环境需要调整。将 [附录示例](#11-附录生产环境-docker-composeyml-示例) 保存为 `deploy/docker-compose.prod.yml`。

核心调整点：

- 后端容器使用本地 jar 包挂载运行，而不是重新构建（加快部署）
- AI 服务使用本地构建的镜像 `leyo-ai-service:v1.0.0` 直接运行
- MySQL/Redis 设置密码、持久化卷
- 后端使用 `prod` 环境配置，并注入 `AI_SERVICE_BASE_URL` 和 `INTERNAL_API_TOKEN`
- AI 服务注入 LLM、Redis、内部接口 Token 等环境变量
- 挂载管理后台、用户端 H5、教练端 H5 的 dist 目录以及文件上传目录到 Nginx 容器
- 为所有服务配置 Docker 日志轮转（`json-file` 驱动，单文件 100MB，保留 3-5 份）
- 不将 MySQL/Redis 端口暴露到宿主机（除非必要）

#### 4.1.9 创建持久化目录

```bash
# 数据库初始化目录
mkdir -p /opt/leyo-swimming/deploy/init-db

# 文件上传目录（头像、证书等图片持久化存储）
mkdir -p /opt/leyo-swimming/uploads
```

将 Flyway 迁移脚本复制到该目录（如后端 `src/main/resources/db/migration/` 下有脚本）：

```bash
cp -r /path/to/leyoSwimming/backend/src/main/resources/db/migration/* \
  /opt/leyo-swimming/deploy/init-db/
```

> 若后端启动时 Flyway 自动执行迁移，可跳过此手动复制步骤，只要确保 `application-prod.yml` 中 `flyway.enabled=true`。

#### 4.1.10 导入初始业务数据（可选）

若需要预置演示/测试数据（教练、套餐模板、用户、套餐、订单），可在 Flyway 迁移完成后导入 `scripts/seed/ai-assistant-seed.sql`。

```bash
# 将 seed SQL 上传到服务器
scp scripts/seed/ai-assistant-seed.sql root@<服务器IP>:/opt/leyo-swimming/deploy/init-db/

# 进入 MySQL 容器执行导入（等待后端启动完成、Flyway 迁移结束后再执行）
docker exec -i leyo-mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} < /opt/leyo-swimming/deploy/init-db/ai-assistant-seed.sql
```

> **注意事项**：
> - 该 seed 数据包含模拟 openid（`seed_*` 开头），**不能用于真实微信登录**，仅用于本地/演示环境测试 AI 助手推荐链路。
> - seed 中的图片 URL 为 `http://localhost:8080/uploads/...`，生产环境需批量替换为 `http://<服务器IP>/uploads/...`。
> - 正式生产环境首次上线建议仅导入必要的字典/配置数据，不导入演示用户和订单。

#### 4.1.11 启动服务

本阶段 AI 服务默认在宿主机直接运行（systemd），核心服务（MySQL/Redis/Backend/Nginx）通过 Docker Compose 启动。

```bash
cd /opt/leyo-swimming/deploy

# 加载环境变量
source .env

# 拉取镜像并启动核心服务（使用生产配置）
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d mysql redis backend nginx

# 启动宿主机 AI 服务（systemd）
systemctl start leyo-ai

# 查看核心服务日志
docker compose -f docker-compose.prod.yml logs -f backend

# 查看 AI 服务日志（宿主机 systemd）
journalctl -u leyo-ai -f
```

#### 4.1.12 确认服务状态

```bash
# 查看容器运行状态
docker compose ps

# 查看 AI 服务状态
systemctl status leyo-ai

# 测试后端健康检查
curl http://<服务器IP>/api/actuator/health

# 测试 API
curl -X POST http://<服务器IP>/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 测试 AI 服务健康检查（宿主机）
curl -s http://localhost:8000/health
```

#### 4.1.13 创建初始管理员账号

Flyway 迁移脚本只创建了管理员表结构，**不会自动插入管理员账号**。首次部署后需手动创建至少一个超级管理员。

```bash
# 生成 bcrypt 密码哈希（以密码 Admin@123 为例，生产环境请替换为强密码）
python3 -c "import bcrypt; print(bcrypt.hashpw(b'Admin@123', bcrypt.gensalt()).decode())"

# 示例输出
# $2b$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 进入 MySQL 容器创建管理员
docker exec -i leyo-mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} <<EOF
INSERT INTO admin_user (username, password_hash, name, role, status, created_at, updated_at)
VALUES ('admin', '$2b$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '系统管理员', 'super_admin', 0, NOW(), NOW());
EOF
```

> **安全提醒**：
> - 创建后立即通过管理后台修改默认密码。
> - 超级管理员账号应专人专用，避免多人共用。
> - 生产环境建议密码长度 >= 16 位，包含大小写字母、数字和特殊字符。

### 4.2 方式二：在服务器上构建（不推荐，但可行）

如果必须在服务器上构建，需额外安装 JDK 21、Maven、Node.js、Python 3.11 和 Docker 引擎（用于构建 AI 服务镜像）。

#### 4.2.1 安装 JDK 21

```bash
apt install -y wget
wget https://packages.adoptium.net/artifactory/deb/pool/main/t/temurin-21/temurin-21-jdk_21.0.4.0.0%2B7_amd64.deb
apt install -f -y ./temurin-21-jdk_21.0.4.0.0+7_amd64.deb
java -version
```

#### 4.2.2 安装 Maven

```bash
apt install -y maven
mvn -version
```

#### 4.2.3 安装 Node.js 20 LTS

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v

# 安装 yarn
npm install -g yarn
yarn -v
```

#### 4.2.4 安装 Python 3.11 与 Docker（用于构建 AI 服务镜像）

```bash
# 安装 Python 3.11 和 pip
apt install -y python3.11 python3.11-venv python3-pip
python3.11 --version

# Docker 已在 §3.3 安装，此处用于构建 AI 服务镜像
docker --version
```

#### 4.2.5 拉取代码并构建

```bash
cd /opt
git clone <你的代码仓库地址> leyoSwimming
cd leyoSwimming

# 构建后端
cd backend
./mvnw clean package -DskipTests -B
cd ..

# 构建管理后台
cd web-admin
yarn install
yarn build
cd ..

# 构建 AI 服务镜像
cd ai-service
docker build -t leyo-ai-service:v1.0.0 .
cd ..
```

#### 4.2.6 后续步骤

后续配置 `.env`、Nginx、`docker-compose.yml`、启动容器与方式一相同。

---

## 5. H5 端发布与验证

本阶段用户端、教练端、管理后台三个前端均以 H5/Web 形式部署到 Nginx，不发布微信小程序。

### 5.1 配置前端 API Base URL

在构建前，确保各前端项目中的后端 API 地址指向服务器 IP 或域名。

- 管理后台：`web-admin/.env.production`
  ```text
  VITE_API_BASE_URL=http://<服务器IP>/api
  ```

- 用户端 H5：`miniapp-user/config/dev.js` 或构建时环境变量
  ```js
  const API_BASE_URL = 'http://<服务器IP>/api';
  ```

- 教练端 H5：`miniapp-coach/config/dev.js` 或构建时环境变量
  ```js
  const API_BASE_URL = 'http://<服务器IP>/api';
  ```

> Taro 代码中访问 `process.env` 需要添加安全兜底：
> ```ts
> const baseURL = (typeof process !== 'undefined' && process.env && process.env.TARO_APP_API_BASE_URL)
>   ? process.env.TARO_APP_API_BASE_URL
>   : 'http://<服务器IP>/api';
> ```

### 5.2 本地构建 H5 产物

构建前请确保目录干净，避免 H5 与小程序产物混淆。

```bash
# 用户端 H5
cd miniapp-user
yarn install
yarn build:h5
mv dist h5-user-dist

# 教练端 H5
cd ../miniapp-coach
yarn install
yarn build:h5
mv dist h5-coach-dist

# 管理后台
cd ../web-admin
yarn install
yarn build
```

### 5.3 上传 H5 产物到服务器

```bash
scp -r web-admin/dist root@<服务器IP>:/opt/leyo-swimming/web-admin-dist/
scp -r miniapp-user/h5-user-dist root@<服务器IP>:/opt/leyo-swimming/h5-user-dist/
scp -r miniapp-coach/h5-coach-dist root@<服务器IP>:/opt/leyo-swimming/h5-coach-dist/
```

### 5.4 Nginx 配置

生产环境 Nginx 配置已包含三个前端 location：

- `/admin/` → 管理后台
- `/h5/user/` → 用户端 H5
- `/h5/coach/` → 教练端 H5

docker-compose.prod.yml 中也已挂载对应目录到 Nginx 容器：

```yaml
volumes:
  - ../web-admin-dist:/usr/share/nginx/html/admin:ro
  - ../h5-user-dist:/usr/share/nginx/html/h5-user-dist:ro
  - ../h5-coach-dist:/usr/share/nginx/html/h5-coach-dist:ro
```

### 5.5 H5 端访问地址

部署完成后，浏览器访问：

```
http://<服务器IP>/admin/
http://<服务器IP>/h5/user/
http://<服务器IP>/h5/coach/
```

---

## 6. 验证方法

### 6.1 容器状态验证

```bash
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml ps
```

预期所有服务状态为 `Up (healthy)` 或 `Up`。

### 6.2 数据库验证

```bash
cd /opt/leyo-swimming/deploy
source .env

# 进入 MySQL 容器
docker exec -it leyo-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD leyo_prod

# 在 MySQL 命令行中执行
SHOW TABLES;
SELECT * FROM flyway_schema_history;
```

### 6.3 Redis 验证

```bash
cd /opt/leyo-swimming/deploy
source .env

docker exec -it leyo-redis redis-cli -a $REDIS_PASSWORD ping
# 预期输出：PONG
```

### 6.4 后端健康检查

```bash
curl http://<服务器IP>/api/actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

### 6.5 AI 服务验证

> 本阶段 AI 服务默认在宿主机直接运行（systemd 服务 `leyo-ai`），端口 8000。

```bash
# 1. 宿主机健康检查
curl -s http://localhost:8000/health

# 2. 从后端容器测试 AI 服务可达性（后端通过 extra_hosts 将 leyo-ai-service 解析到宿主机）
docker exec leyo-backend curl -s http://leyo-ai-service:8000/health

# 3. 测试 AI 对话接口（通过 Java 后端网关）
# 先登录获取用户 token，再调用 /api/ai-assistant/chat
curl -X POST http://<服务器IP>/api/ai-assistant/session/create \
  -H "Authorization: Bearer <user-token>"

curl -X POST http://<服务器IP>/api/ai-assistant/chat \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "<上一步返回的 sessionId>",
    "message": "我想学自由泳，推荐一个教练"
  }'
```

> 如果 `ENABLE_MOCK_DATA=true`，AI 服务会返回 Mock 推荐数据，不调用 LLM 和 Java 内部接口。

### 6.6 管理后台访问

浏览器访问：

```
http://<服务器IP>/admin/
```

应能看到管理后台登录页面。

### 6.7 H5 端访问验证

浏览器访问以下地址，确认三个前端 H5 均可正常打开：

```
http://<服务器IP>/admin/
http://<服务器IP>/h5/user/
http://<服务器IP>/h5/coach/
```

### 6.8 API 接口测试

```bash
# 测试管理员登录（使用真实账号）
curl -X POST http://<服务器IP>/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}'
```

### 6.9 H5 端功能验证

1. 用户端 H5：`http://<服务器IP>/h5/user/`
   - 测试手机号登录/验证码登录
   - 测试首页、教练列表、教练详情
   - 测试 AI 助手对话
2. 教练端 H5：`http://<服务器IP>/h5/coach/`
   - 测试教练登录
   - 测试课程管理、预约管理

---

## 7. 日常运维

### 7.1 查看服务状态

项目提供了便捷脚本 `scripts/check-leyo-services.sh`，可一次性查看 Docker 服务、容器运行状态、健康状态、资源使用、端口占用和 Compose 服务状态。

**本地通过 SSH 直接运行（推荐）：**

```bash
ssh root@<服务器IP> 'bash -s' < scripts/check-leyo-services.sh
```

**上传到服务器后运行：**

```bash
scp scripts/check-leyo-services.sh root@<服务器IP>:/opt/leyo-swimming/
ssh root@<服务器IP>
chmod +x /opt/leyo-swimming/check-leyo-services.sh
./check-leyo-services.sh
```

脚本输出内容包括：
- Docker 服务状态
- 所有 `leyo-*` 容器的运行状态
- 各容器健康状态（`leyo-backend`、`leyo-mysql`、`leyo-redis`、`leyo-nginx`）
- 内存和磁盘资源使用情况
- 关键端口（80、8080、3306、6379、8000）占用情况
- Docker Compose 服务状态

也可以手动查看关键状态：

```bash
# 查看所有 leyo 容器
docker ps -a | grep leyo

# 查看指定容器状态和健康状态
docker inspect --format="{{.State.Status}} {{.State.Health.Status}}" leyo-backend
docker inspect --format="{{.State.Status}} {{.State.Health.Status}}" leyo-mysql

# 查看 Docker Compose 服务状态
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml ps
```

### 7.2 查看日志

```bash
cd /opt/leyo-swimming/deploy

# 查看所有服务日志
docker compose -f docker-compose.prod.yml logs -f

# 查看后端日志（最近 100 行）
docker compose -f docker-compose.prod.yml logs -f --tail=100 backend

# 查看 MySQL 日志
docker compose -f docker-compose.prod.yml logs -f mysql

# 查看 Nginx 日志
docker compose -f docker-compose.prod.yml logs -f nginx

# 查看 AI 服务日志（宿主机 systemd 运行）
journalctl -u leyo-ai -f
```

### 7.3 备份数据库

```bash
cd /opt/leyo-swimming/deploy
source .env

# 手动备份
mkdir -p /opt/backups
docker exec leyo-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD leyo_prod \
  > /opt/backups/leyo_prod_$(date +%Y%m%d_%H%M%S).sql

# 建议配置定时任务（crontab -e）
0 2 * * * cd /opt/leyo-swimming/deploy && source .env && /opt/leyo-swimming/deploy/backup-db.sh >> /var/log/leyo-backup.log 2>&1
```

backup-db.sh 示例：

```bash
#!/bin/bash
set -e

BACKUP_DIR="/opt/backups"
DB_NAME="${MYSQL_DATABASE:-leyo_prod}"
DB_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/leyo_prod_${DATE}.sql"

mkdir -p "${BACKUP_DIR}"
docker exec leyo-mysql mysqldump -uroot -p"${DB_ROOT_PASSWORD}" "${DB_NAME}" > "${BACKUP_FILE}"

# 保留最近 30 天的备份
find "${BACKUP_DIR}" -name "leyo_prod_*.sql" -mtime +30 -delete

echo "Backup completed: ${BACKUP_FILE}"
```

保存后赋予执行权限：

```bash
chmod +x /opt/leyo-swimming/deploy/backup-db.sh
```

### 7.4 后端服务一直 restarting 排查

当 `leyo-backend` 容器状态持续为 `restarting` 时，按以下步骤排查。

**方式一：使用一键排查脚本（推荐）**

项目提供了 `scripts/diagnose-leyo-backend.sh`，可一次性输出日志、容器状态、健康状态、`.env` 关键配置、端口占用和资源使用情况。

```bash
ssh root@<服务器IP> 'bash -s' < scripts/diagnose-leyo-backend.sh
```

或上传到服务器运行：

```bash
scp scripts/diagnose-leyo-backend.sh root@<服务器IP>:/opt/leyo-swimming/
ssh root@<服务器IP>
chmod +x /opt/leyo-swimming/diagnose-leyo-backend.sh
./diagnose-leyo-backend.sh
```

**方式二：手动逐步排查**

1. **查看后端报错日志：**
   ```bash
   docker logs -f --tail=200 leyo-backend
   docker logs leyo-backend 2>&1 | tail -n 100
   ```

2. **检查 MySQL/Redis 是否健康：**
   ```bash
   docker inspect --format="{{.State.Status}} {{.State.Health.Status}}" leyo-mysql
   docker inspect --format="{{.State.Status}} {{.State.Health.Status}}" leyo-redis
   ```

3. **检查 .env 关键配置：**
   ```bash
   cd /opt/leyo-swimming/deploy
   grep AI_SERVICE_BASE_URL .env   # 应为 http://<服务器IP>:8000
   grep DOMAIN .env
   ```

4. **检查 AI 服务是否可访问：**
   ```bash
   curl -s http://localhost:8000/health
   curl -s http://<服务器IP>:8000/health
   ```

5. **检查端口占用和资源：**
   ```bash
   ss -tlnp | grep -E ':(80|8080|3306|6379|8000)\b'
   free -h
   df -h /
   ```

6. **手动运行后端容器看完整错误：**
   ```bash
   docker run --rm -it \
     -v /opt/leyo-swimming/leyo-swimming-backend-0.1.0-SNAPSHOT.jar:/app/app.jar:ro \
     -v /opt/leyo-swimming/uploads:/app/uploads \
     --network leyo-network \
     -e SPRING_PROFILES_ACTIVE=prod \
     --env-file /opt/leyo-swimming/deploy/.env \
     eclipse-temurin:21-jre-alpine \
     java -jar /app/app.jar
   ```

常见原因：
- `.env` 中 `AI_SERVICE_BASE_URL` 指向 `http://leyo-ai-service:8000`（容器名），但 AI 服务实际在宿主机运行，应改为 `http://<服务器IP>:8000`。
- MySQL 或 Redis 未启动或健康检查失败。
- 80/8080/3306/6379/8000 端口被其他进程占用。
- 服务器内存不足导致 JVM 无法启动。

### 7.5 停止所有服务

```bash
cd /opt/leyo-swimming/deploy

# 停止宿主机 AI 服务
systemctl stop leyo-ai

# 停止所有 Docker 容器（数据卷保留）
docker compose -f docker-compose.prod.yml down --remove-orphans
# 如果使用了宿主机 AI 服务的 override 文件：
docker compose -f docker-compose.prod.yml -f docker-compose.ai-direct.yml down --remove-orphans
```

或者使用部署脚本一键停止（本地执行）：

```powershell
.\scripts\deploy-first-stage.ps1 -StopOnly
```

### 7.6 一键重新部署

使用部署脚本可以先停止所有服务，然后按正常流程重新构建/上传/部署：

```powershell
# 完全重新构建并部署（会询问每个产物是否重新构建）
.\scripts\deploy-first-stage.ps1 -Redeploy

# 使用已有构建产物重新部署（跳过本地构建）
.\scripts\deploy-first-stage.ps1 -Redeploy -SkipBuild

# 仅停止服务后重新启动容器（不重新上传、不重新构建）
.\scripts\deploy-first-stage.ps1 -Redeploy -SkipBuild -SkipUpload
```

### 7.7 重启服务

```bash
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml restart backend
docker compose -f docker-compose.prod.yml restart nginx
```

### 7.8 清理旧镜像和日志

```bash
# 清理未使用的镜像
docker image prune -f

# 清理容器日志（谨慎）
docker system prune -f
```

---

## 8. 后续升级部署

### 8.1 升级前准备

1. **备份数据库**：

   ```bash
   cd /opt/leyo-swimming/deploy
   source .env
   mkdir -p /opt/backups
   docker exec leyo-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD leyo_prod \
     > /opt/backups/leyo_prod_before_upgrade_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **确认升级范围**：后端、AI 服务、管理后台、小程序是否有变更。
3. **确认 AI 服务兼容性**：检查 AI 服务新版本是否依赖新的 Java 内部接口或环境变量，确保 `INTERNAL_API_TOKEN` 与后端一致。
4. **检查 Flyway 迁移脚本**：确认新版本的迁移脚本已准备好，且版本号递增。

### 8.2 后端升级

```bash
# 1. 在本地构建新的 jar 包
cd /path/to/leyoSwimming/backend
./mvnw clean package -DskipTests -B

# 2. 上传到服务器，保留旧版本备份
scp backend/target/leyo-swimming-backend-0.1.0-SNAPSHOT.jar \
  root@<服务器IP>:/opt/leyo-swimming/leyo-swimming-backend-0.1.0-SNAPSHOT-new.jar

ssh root@<服务器IP>
cd /opt/leyo-swimming
mv leyo-swimming-backend-0.1.0-SNAPSHOT.jar \
  leyo-swimming-backend-0.1.0-SNAPSHOT-backup-$(date +%Y%m%d).jar
mv leyo-swimming-backend-0.1.0-SNAPSHOT-new.jar leyo-swimming-backend-0.1.0-SNAPSHOT.jar

# 3. 重启后端容器（生产配置使用 jar 挂载，替换文件后重启即可）
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml restart backend

# 4. 验证
docker compose -f docker-compose.prod.yml logs -f backend
curl http://<服务器IP>/api/actuator/health
```

### 8.3 AI 服务升级

本阶段 AI 服务在宿主机直接运行（systemd），升级只需更新源码/依赖后重启服务。

```bash
# 1. 上传新的 AI 服务源码到服务器
scp -r /path/to/leyoSwimming/ai-service root@<服务器IP>:/opt/leyo-swimming/ai-service-new

ssh root@<服务器IP>
cd /opt/leyo-swimming

# 2. 备份旧版本并替换
mv ai-service ai-service-backup-$(date +%Y%m%d)
mv ai-service-new ai-service

# 3. 重新安装依赖（requirements.txt 有变更时）
cd /opt/leyo-swimming/ai-service
source /opt/leyo-swimming/ai-service-venv/bin/activate
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 4. 重启 AI 服务
systemctl restart leyo-ai

# 5. 验证
journalctl -u leyo-ai -f
curl -s http://localhost:8000/health
```

> **注意**：AI 服务升级前请确认 `INTERNAL_API_TOKEN` 未变更；若变更需同步修改后端 `.env` 并重启后端容器。

### 8.4 管理后台升级

```bash
# 1. 在本地构建新的 dist
cd /path/to/leyoSwimming/web-admin
yarn install
yarn build

# 2. 上传并替换
scp -r web-admin/dist root@<服务器IP>:/opt/leyo-swimming/web-admin-dist-new

ssh root@<服务器IP>
cd /opt/leyo-swimming
mv web-admin-dist web-admin-dist-backup-$(date +%Y%m%d)
mv web-admin-dist-new web-admin-dist

# 3. 重启 Nginx（如果已挂载卷，通常无需重启，但建议检查缓存）
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml restart nginx
```

### 8.5 小程序升级

1. 在本地执行 `yarn build:weapp` 重新构建
2. 使用微信开发者工具或 `miniprogram-ci` 上传新版本
3. 在微信公众平台提交审核并发布

### 8.6 配置变更升级

如果 `.env` 或 `nginx.conf` 有变更：

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming/deploy

# 1. 重新加载 systemd 服务以读取新 .env
systemctl daemon-reload
systemctl restart leyo-ai

# 2. 重新创建 Docker 容器以读取新 .env
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

> 注意：`docker compose down` 会停止容器，但持久化卷中的数据不会丢失。

### 8.7 数据库结构升级

后端使用 Flyway 自动管理数据库迁移，只要确保：

- `application-prod.yml` 中 `flyway.enabled=true`
- 新版本的迁移脚本已打包进 jar
- 数据库用户有 DDL 执行权限

启动后端时会自动执行新迁移。如出现迁移失败，查看后端日志定位问题。

### 8.8 回滚方案

如果升级后出现问题，按以下顺序回滚：

1. **后端回滚**：

   ```bash
   cd /opt/leyo-swimming
   mv leyo-swimming-backend-0.1.0-SNAPSHOT.jar leyo-swimming-backend-0.1.0-SNAPSHOT-bad.jar
   mv leyo-swimming-backend-0.1.0-SNAPSHOT-backup-YYYYMMDD.jar leyo-swimming-backend-0.1.0-SNAPSHOT.jar
   cd deploy
   source .env
   docker compose -f docker-compose.prod.yml restart backend
   ```

2. **数据库回滚**（如升级导致数据问题）：

   ```bash
   cd /opt/leyo-swimming/deploy
   source .env

   # 先停止后端
   docker compose -f docker-compose.prod.yml stop backend

   # 恢复数据库备份
   docker exec -i leyo-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD leyo_prod \
     < /opt/backups/leyo_prod_before_upgrade_YYYYMMDD_HHMMSS.sql

   # 重新启动后端
   docker compose -f docker-compose.prod.yml up -d backend
   ```

3. **AI 服务回滚**：

   ```bash
   cd /opt/leyo-swimming/deploy
   # 将 docker-compose.prod.yml 中的 image 改回旧版本标签
   docker compose -f docker-compose.prod.yml up -d --no-deps ai-service
   docker compose -f docker-compose.prod.yml logs -f ai-service
   ```

4. **管理后台回滚**：

   ```bash
   cd /opt/leyo-swimming
   mv web-admin-dist web-admin-dist-bad
   mv web-admin-dist-backup-YYYYMMDD web-admin-dist
   cd deploy
   docker compose -f docker-compose.prod.yml restart nginx
   ```

---

## 9. 常见问题

### Q1：后端启动报数据库连接失败

- 检查 MySQL 容器是否健康运行：`docker compose ps`
- 检查 `.env` 中的数据库密码是否正确
- 检查后端容器能否解析 `mysql` 主机名：`docker exec leyo-backend ping mysql`

### Q2：管理后台页面空白或 404

- 确认 `web-admin/dist` 已正确挂载到 Nginx 容器
- 确认 Nginx `try_files` 配置正确：`try_files $uri $uri/ /admin/index.html;`
- 浏览器清除缓存或强制刷新（Ctrl + F5）

### Q3：H5 请求报 500 或无法连接

- 确认浏览器访问地址为 `http://<服务器IP>/api/**` 或配置的域名
- 确认后端容器状态正常：`docker compose ps`
- 查看后端日志定位具体错误

### Q4：Flyway 迁移失败

- 检查 `flyway_schema_history` 表中的失败记录
- 确认数据库用户有 DDL 权限
- 手动修复后执行 `repair` 或清理重复记录

### Q5：Redis 连接失败

- 检查 Redis 容器状态：`docker compose ps`
- 检查后端配置的 Redis 密码是否与 `.env` 一致
- 生产环境 Redis 不应暴露到公网

### Q6：AI 服务启动报 `INTERNAL_API_TOKEN 长度不能少于 32 位`

- 检查 `.env` 中 `INTERNAL_API_TOKEN` 是否已设置且长度 >= 32
- 检查后端和 AI 服务的 `INTERNAL_API_TOKEN` 是否一致
- 重启后端和 AI 服务容器使配置生效

### Q7：AI 服务无法调用 Java 内部接口

- 确认后端 `/api/internal/ai/**` 接口已正常启动
- 检查 AI 服务 `JAVA_INTERNAL_BASE_URL` 是否指向 `http://leyo-backend:8080/api/internal/ai`
- 检查后端 `InternalAuthFilter` 中的 Token 是否与 AI 服务发送的 `X-Internal-Token` 一致
- 查看 AI 服务日志中的具体错误信息

### Q8：AI 对话返回「leyo 暂时走神了」

- 检查 AI 服务是否能正常访问 LLM：`docker exec leyo-ai-service curl -s http://localhost:8000/health`
- 检查 `.env` 中 `LLM_API_KEY`、`LLM_BASE_URL`、`LLM_MODEL` 是否正确有效
- 查看 AI 服务日志定位 LLM 调用错误
- 若 Java 内部接口未就绪，可临时开启 `ENABLE_MOCK_DATA=true` 调试，但生产环境必须关闭

---

## 10. 安全检查清单

部署完成后，逐项确认：

- [ ] MySQL root 密码和用户密码已修改为强密码
- [ ] Redis 已设置密码且未暴露到公网
- [ ] JWT_SECRET 为强随机字符串，长度 >= 64 字符
- [ ] PHONE_ENCRYPTION_KEY 已设置且安全保管
- [ ] ID_CARD_ENCRYPTION_KEY 已设置且安全保管
- [ ] 服务器防火墙仅开放 22、80 端口（3306/6379 不暴露公网）
- [ ] Nginx 已配置基础安全响应头（X-Frame-Options、X-Content-Type-Options 等）
- [ ] 后端错误响应不暴露堆栈、SQL、内部路径
- [ ] 已配置数据库自动备份
- [ ] `.env` 文件已加入 `.gitignore`，不提交到版本控制
- [ ] `INTERNAL_API_TOKEN` 已设置为长度 >= 32 的强随机字符串，且与 AI 服务保持一致
- [ ] `LLM_API_KEY` 未提交到 Git，仅通过 `.env` 注入
- [ ] 生产环境 `ENABLE_MOCK_DATA=false`
- [ ] AI 服务未将 8000 端口直接暴露到公网（仅容器网络内部可访问）
- [ ] Swagger 接口文档（`/swagger-ui.html`、`/v3/api-docs/**`）在生产环境已限制访问或关闭，避免接口信息泄露

---

## 11. 附录：生产环境 docker-compose.yml 示例

将以下配置保存为 `deploy/docker-compose.prod.yml`，生产环境使用。本阶段 AI 服务在宿主机直接运行（systemd），因此 compose 中**不包含** `ai-service` 容器。

实际使用的 `deploy/docker-compose.prod.yml` 已包含 `pull_policy: never`，避免 Docker Hub 拉取超时；镜像由脚本按需手动拉取。

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: leyo-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-db:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
    # 生产环境不暴露端口到宿主机
    # ports:
    #   - "127.0.0.1:3306:3306"
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"
    networks:
      - leyo-network

  redis:
    image: redis:7-alpine
    container_name: leyo-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    # 生产环境不暴露端口到宿主机
    # ports:
    #   - "127.0.0.1:6379:6379"
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"
    networks:
      - leyo-network

  backend:
    image: eclipse-temurin:21-jre-alpine
    container_name: leyo-backend
    restart: unless-stopped
    working_dir: /app
    volumes:
      - ../leyo-swimming-backend-0.1.0-SNAPSHOT.jar:/app/app.jar:ro
      - ../uploads:/app/uploads
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_DATABASE: 0
      JWT_SECRET: ${JWT_SECRET}
      PHONE_ENCRYPTION_KEY: ${PHONE_ENCRYPTION_KEY}
      ID_CARD_ENCRYPTION_KEY: ${ID_CARD_ENCRYPTION_KEY}
      AI_SERVICE_BASE_URL: ${AI_SERVICE_BASE_URL}
      INTERNAL_API_TOKEN: ${INTERNAL_API_TOKEN}
      APP_FILE_UPLOAD_DIR: /app/uploads
      APP_FILE_BASE_URL: http://${DOMAIN}/uploads/
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    command: ["java", "-jar", "app.jar"]
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "5"
    networks:
      - leyo-network

  nginx:
    image: nginx:alpine
    container_name: leyo-nginx
    restart: unless-stopped
    pull_policy: never
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ../web-admin-dist:/usr/share/nginx/html/admin:ro
      - ../h5-user-dist:/usr/share/nginx/html/h5-user-dist:ro
      - ../h5-coach-dist:/usr/share/nginx/html/h5-coach-dist:ro
      - ../uploads:/usr/share/nginx/html/uploads:ro
    depends_on:
      - backend
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "5"
    networks:
      - leyo-network

volumes:
  mysql_data:
  redis_data:

networks:
  leyo-network:
    driver: bridge
```

使用生产配置启动：

```bash
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml up -d
```

---

## 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI Agent | 初稿：初次部署与升级手册 |
| v1.1 | 2026-08-27 | AI Agent | 补充 AI 服务（leyo-ai-service）生产环境部署、升级、验证及安全相关内容 |
| v1.2 | 2026-08-27 | AI Agent | 按第一阶段需求调整：移除 HTTPS/SSL/域名要求，改为 HTTP + IP 访问；前端全部以 H5 部署，移除微信小程序发布、短信、OSS 相关内容 |
