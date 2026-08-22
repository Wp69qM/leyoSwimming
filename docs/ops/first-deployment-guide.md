# leyoSwimming 生产环境初次部署及升级手册

> **文档定位**：第二批次开发完成并验证通过后，将 leyoSwimming 前后端部署到一台全新服务器的操作手册。  
> **目标读者**：运维人员 / 开发工程师。  
> **部署范围**：后端服务、管理后台 Web 端、MySQL、Redis。小程序端构建产物需上传至微信公众平台，不在服务器直接部署。  
> **版本**：v1.0  
> **日期**：2026-08-15

---

## 目录

1. [部署架构概览](#1-部署架构概览)
2. [前置准备](#2-前置准备)
3. [服务器环境安装](#3-服务器环境安装)
4. [初次部署步骤](#4-初次部署步骤)
5. [微信小程序端发布](#5-微信小程序端发布)
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
域名（HTTPS）
    │
    ▼
Nginx（容器或宿主机）
    ├── /api/*  ───────────► 后端 Spring Boot（容器，端口 8080）
    ├── /admin/* ──────────► 管理后台静态资源（dist）
    └── 小程序 API ────────► 后端 Spring Boot（同上）
    │
    ├── MySQL 8.0（容器，持久化卷）
    └── Redis 7（容器，持久化卷）
```

**技术栈**

| 模块 | 技术 | 说明 |
|------|------|------|
| 后端 | Spring Boot 3.2 + Java 21 + Maven | 提供 `/api/**` 接口 |
| 管理后台 | Vue 3 + Vite + TypeScript + Element Plus | 构建为静态页面，Nginx 托管 |
| 用户端小程序 | Taro 4 + React 18 + TypeScript | 构建后上传微信公众平台 |
| 教练端小程序 | Taro 4 + React 18 + TypeScript | 构建后上传微信公众平台 |
| 数据库 | MySQL 8.0 | 主数据存储 |
| 缓存 | Redis 7 | 会话、缓存、分布式锁 |
| 反向代理 | Nginx | HTTPS、静态资源、负载均衡 |

---

## 2. 前置准备

### 2.1 服务器要求

- **系统**：Ubuntu 22.04 LTS / Debian 12 / CentOS 7+（推荐 Ubuntu 22.04 LTS）
- **配置**：至少 2 核 CPU / 4GB 内存 / 50GB 磁盘（生产建议 4 核 8GB）
- **网络**：固定公网 IP，安全组/防火墙开放必要端口
- **域名**：已备案域名（国内服务器）并解析到服务器 IP
- **SSL 证书**：可使用 Let's Encrypt 免费证书或云厂商证书

### 2.2 必须开放的端口

| 端口 | 用途 | 开放对象 |
|------|------|----------|
| 22 | SSH | 仅管理员 IP |
| 80 | HTTP（自动跳转 HTTPS） | 公网 |
| 443 | HTTPS | 公网 |
| 3306 | MySQL | 建议仅内网/本机，不暴露公网 |
| 6379 | Redis | 建议仅内网/本机，不暴露公网 |

> **安全建议**：生产环境不要将 3306、6379 暴露到公网。若必须远程管理，请使用 SSH 隧道或 VPN。

### 2.3 外部账号与资料

| 项目 | 用途 | 获取方式 |
|------|------|----------|
| 用户端小程序 AppID / AppSecret | 微信登录、手机号登录 | [微信公众平台](https://mp.weixin.qq.com/) |
| 教练端小程序 AppID / AppSecret | 微信登录、手机号登录 | [微信公众平台](https://mp.weixin.qq.com/) |
| 短信服务 | 发送验证码 | 阿里云 / 腾讯云 / 网易云信 |
| 对象存储 OSS/COS | 头像、证书等图片存储 | 阿里云 / 腾讯云 |
| 微信支付（MVP 后可接入） | 真实支付 | 微信支付商户平台 |

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

### 3.7 安装 Certbot（Let's Encrypt 证书，可选）

```bash
# Ubuntu / Debian
apt install -y certbot python3-certbot-nginx

# 申请证书（需先配置好 DNS 解析）
certbot --nginx -d your-domain.com -d www.your-domain.com
```

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

#### 4.1.2 本地构建管理后台 dist

```bash
cd /path/to/leyoSwimming/web-admin
yarn install
yarn build
```

构建产物：`web-admin/dist/` 目录

#### 4.1.3 本地构建微信小程序

```bash
# 用户端
cd /path/to/leyoSwimming/miniapp-user
yarn install
yarn build:weapp
# 产物在 miniapp-user/dist/ 目录

# 教练端
cd /path/to/leyoSwimming/miniapp-coach
yarn install
yarn build:weapp
# 产物在 miniapp-coach/dist/ 目录
```

#### 4.1.4 上传构建产物到服务器

```bash
# 上传 jar 包
scp backend/target/leyo-swimming-backend-0.1.0-SNAPSHOT.jar root@<服务器IP>:/opt/leyo-swimming/

# 上传管理后台静态资源
scp -r web-admin/dist root@<服务器IP>:/opt/leyo-swimming/web-admin-dist/

# 上传部署配置（后续在服务器上维护）
scp -r deploy root@<服务器IP>:/opt/leyo-swimming/
```

#### 4.1.5 在服务器上准备生产环境配置

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

# 微信小程序
MINIAPP_USER_APPID=wx_user_appid_here
MINIAPP_USER_SECRET=wx_user_secret_here
MINIAPP_COACH_APPID=wx_coach_appid_here
MINIAPP_COACH_SECRET=wx_coach_secret_here

# 短信服务
SMS_PROVIDER=aliyun
SMS_ACCESS_KEY_ID=your_key_id
SMS_ACCESS_KEY_SECRET=your_key_secret
SMS_SIGN_NAME=your_sign_name
SMS_TEMPLATE_CODE_LOGIN=your_template_code

# 对象存储
OSS_PROVIDER=aliyun
OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET=leyo-swimming
OSS_ACCESS_KEY_ID=your_key_id
OSS_ACCESS_KEY_SECRET=your_key_secret
```

#### 4.1.6 生产化改造 Nginx 配置

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
        server_name your-domain.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name your-domain.com;

        ssl_certificate     /etc/nginx/ssl/your-domain.com.crt;
        ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;
        ssl_protocols       TLSv1.2 TLSv1.3;
        ssl_ciphers         HIGH:!aNULL:!MD5;

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

        # 后端本地文件上传访问（如使用本地存储）
        location /uploads/ {
            alias /usr/share/nginx/html/uploads/;
        }

        location / {
            return 404;
        }
    }
}
```

> 注意：将 `your-domain.com` 替换为真实域名，SSL 证书路径替换为实际路径。

#### 4.1.7 生产化改造 docker-compose.yml

当前 `deploy/docker-compose.yml` 为开发环境配置，生产环境需要调整。将 [附录示例](#11-附录生产环境-docker-composeyml-示例) 保存为 `deploy/docker-compose.prod.yml`。

核心调整点：

- 后端容器使用本地 jar 包挂载运行，而不是重新构建（加快部署）
- MySQL/Redis 设置强密码、持久化卷
- 后端使用 `prod` 环境配置
- 挂载管理后台 dist 目录到 Nginx 容器
- 不将 MySQL/Redis 端口暴露到宿主机（除非必要）

#### 4.1.8 创建数据库初始化目录

```bash
mkdir -p /opt/leyo-swimming/deploy/init-db
```

将 Flyway 迁移脚本复制到该目录（如后端 `src/main/resources/db/migration/` 下有脚本）：

```bash
cp -r /path/to/leyoSwimming/backend/src/main/resources/db/migration/* \
  /opt/leyo-swimming/deploy/init-db/
```

> 若后端启动时 Flyway 自动执行迁移，可跳过此手动复制步骤，只要确保 `application-prod.yml` 中 `flyway.enabled=true`。

#### 4.1.9 启动服务

```bash
cd /opt/leyo-swimming/deploy

# 加载环境变量
source .env

# 拉取镜像并启动（使用生产配置）
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

# 查看日志
docker compose -f docker-compose.prod.yml logs -f backend
```

#### 4.1.10 确认服务状态

```bash
# 查看容器运行状态
docker compose ps

# 测试后端健康检查
curl https://your-domain.com/api/actuator/health

# 测试 API
curl -X POST https://your-domain.com/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 4.2 方式二：在服务器上构建（不推荐，但可行）

如果必须在服务器上构建，需额外安装 JDK 21、Maven、Node.js。

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

#### 4.2.4 拉取代码并构建

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
```

#### 4.2.5 后续步骤

后续配置 `.env`、Nginx、`docker-compose.yml`、启动容器与方式一相同。

---

## 5. 微信小程序端发布

小程序不是部署到自有服务器，而是将构建产物上传到微信公众平台，审核通过后发布。

### 5.1 配置小程序 request 合法域名

登录 [微信公众平台](https://mp.weixin.qq.com/)，分别进入用户端和教练端小程序后台：

1. 开发 → 开发管理 → 开发设置 → 服务器域名
2. `request 合法域名` 添加：`https://your-domain.com`
3. `uploadFile 合法域名` 添加：对象存储上传域名（如使用 OSS）
4. `downloadFile 合法域名` 添加：对象存储下载域名

### 5.2 配置小程序 API Base URL

在发布前，确保小程序代码中的后端 API 地址指向生产域名：

- 用户端：`miniapp-user/src/api/request.ts` 或对应环境变量文件
- 教练端：`miniapp-coach/src/api/request.ts` 或对应环境变量文件

将 base URL 设置为：

```ts
const API_BASE_URL = 'https://your-domain.com/api';
```

> 小程序代码中访问 `process.env` 需要添加安全兜底，参考项目内存中的规范：
> ```ts
> const baseURL = (typeof process !== 'undefined' && process.env && process.env.TARO_APP_API_BASE_URL)
>   ? process.env.TARO_APP_API_BASE_URL
>   : 'https://your-domain.com/api';
> ```

### 5.3 使用微信开发者工具上传

1. 打开微信开发者工具
2. 导入项目：
   - 用户端选择 `miniapp-user/project.config.json`
   - 教练端选择 `miniapp-coach/project.config.json`
3. 确保已执行 `yarn build:weapp` 生成 `dist` 目录
4. 点击「上传」按钮，填写版本号和项目备注
5. 上传成功后，在微信公众平台「版本管理」中提交审核
6. 审核通过后点击「发布」

### 5.4 自动化上传（可选）

可配置 `miniprogram-ci` 在 CI 流程中自动上传预览版/体验版：

```bash
# 用户端
cd miniapp-user
yarn add -D miniprogram-ci

# 上传命令示例
npx miniprogram-ci upload \
  --pp ./ \
  --pkv 1.0.0 \
  --appid wx_user_appid_here \
  --uv 1.0.0 \
  --enable-es6 true
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
curl https://your-domain.com/api/actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

### 6.5 管理后台访问

浏览器访问：

```
https://your-domain.com/admin/
```

应能看到管理后台登录页面。

### 6.6 API 接口测试

```bash
# 测试管理员登录（使用真实账号）
curl -X POST https://your-domain.com/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}'
```

### 6.7 小程序验证

1. 微信开发者工具中设置不校验合法域名（开发设置 → 本地设置 → 不校验合法域名）
2. 修改 `miniapp-user/src/api/request.ts` 或环境变量中的 API base URL 为 `https://your-domain.com/api`
3. 重新编译，测试登录、首页、教练列表等关键流程

---

## 7. 日常运维

### 7.1 查看日志

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
```

### 7.2 备份数据库

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

### 7.3 重启服务

```bash
cd /opt/leyo-swimming/deploy
docker compose -f docker-compose.prod.yml restart backend
docker compose -f docker-compose.prod.yml restart nginx
```

### 7.4 清理旧镜像和日志

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

2. **确认升级范围**：后端、管理后台、小程序是否有变更。
3. **检查 Flyway 迁移脚本**：确认新版本的迁移脚本已准备好，且版本号递增。

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
curl https://your-domain.com/api/actuator/health
```

### 8.3 管理后台升级

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

### 8.4 小程序升级

1. 在本地执行 `yarn build:weapp` 重新构建
2. 使用微信开发者工具或 `miniprogram-ci` 上传新版本
3. 在微信公众平台提交审核并发布

### 8.5 配置变更升级

如果 `.env` 或 `nginx.conf` 有变更：

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming/deploy
source .env

# 重新加载配置
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

> 注意：`docker compose down` 会停止容器，但持久化卷中的数据不会丢失。

### 8.6 数据库结构升级

后端使用 Flyway 自动管理数据库迁移，只要确保：

- `application-prod.yml` 中 `flyway.enabled=true`
- 新版本的迁移脚本已打包进 jar
- 数据库用户有 DDL 执行权限

启动后端时会自动执行新迁移。如出现迁移失败，查看后端日志定位问题。

### 8.7 回滚方案

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

3. **管理后台回滚**：

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

### Q3：小程序请求报 500 或无法连接

- 确认小程序 request 合法域名已配置并发布
- 确认服务器 HTTPS 证书有效
- 查看后端日志定位具体错误

### Q4：Flyway 迁移失败

- 检查 `flyway_schema_history` 表中的失败记录
- 确认数据库用户有 DDL 权限
- 手动修复后执行 `repair` 或清理重复记录

### Q5：Redis 连接失败

- 检查 Redis 容器状态：`docker compose ps`
- 检查后端配置的 Redis 密码是否与 `.env` 一致
- 生产环境 Redis 不应暴露到公网

---

## 10. 安全检查清单

部署完成后，逐项确认：

- [ ] MySQL root 密码和用户密码已修改为强密码
- [ ] Redis 已设置密码且未暴露到公网
- [ ] JWT_SECRET 为强随机字符串，长度 >= 64 字符
- [ ] PHONE_ENCRYPTION_KEY 已设置且安全保管
- [ ] 微信小程序 AppSecret 未硬编码在代码中，仅通过环境变量注入
- [ ] 短信、OSS 等云服务的 AccessKey 未提交到 Git
- [ ] 服务器防火墙仅开放 22、80、443 端口（3306/6379 不开放公网）
- [ ] 已配置 HTTPS，HTTP 自动跳转 HTTPS
- [ ] Nginx 已配置安全响应头（X-Frame-Options、X-Content-Type-Options 等）
- [ ] 后端错误响应不暴露堆栈、SQL、内部路径
- [ ] 已配置数据库自动备份
- [ ] `.env` 文件已加入 `.gitignore`，不提交到版本控制
- [ ] 小程序 request 合法域名仅配置生产域名

---

## 11. 附录：生产环境 docker-compose.yml 示例

将以下配置保存为 `deploy/docker-compose.prod.yml`，生产环境使用：

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
    networks:
      - leyo-network

  backend:
    image: eclipse-temurin:21-jre-alpine
    container_name: leyo-backend
    restart: unless-stopped
    working_dir: /app
    volumes:
      - ../leyo-swimming-backend-0.1.0-SNAPSHOT.jar:/app/app.jar:ro
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
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    command: ["java", "-jar", "app.jar"]
    networks:
      - leyo-network

  nginx:
    image: nginx:alpine
    container_name: leyo-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ../web-admin-dist:/usr/share/nginx/html/admin:ro
      - /etc/letsencrypt:/etc/nginx/ssl:ro
    depends_on:
      - backend
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
