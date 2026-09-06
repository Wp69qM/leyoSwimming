# AI Service RAG / 联网搜索兜底升级部署文档

> 适用版本：leyoSwimming AI Service 知识库检索 + Tavily 联网搜索兜底功能升级
> 升级日期：2026-09-06

---

## 1. 升级背景与目标

本次升级为 AI 助手新增两项核心能力：

1. **知识库主动检索（RAG）**：当用户询问游泳安全、技巧、装备等知识类问题时，主动从 Chroma 向量库检索相关内容并注入 LLM 上下文。
2. **联网搜索兜底（Tavily）**：当知识库未命中时，自动调用 Tavily 搜索补充答案，避免错误返回教练/套餐推荐。

升级后应能解决类似以下问题：

> 用户问"速比涛这个品牌有推荐的新手入手的型号吗"，系统不再返回教练/套餐信息，而是基于知识库或网络搜索给出装备建议。

---

## 2. 前置条件

升级前请确认：

- [ ] 服务器已安装 Docker 与 Docker Compose
- [ ] 已备份现有 `deploy/.env` 和 `ai-service/data/chroma` 目录（如有）
- [ ] 已获取以下第三方服务 API Key：
  - **DashScope**（Embedding）：用于知识库向量生成与检索
  - **DashScope / DeepSeek / OpenAI 兼容服务商**（LLM）：用于对话生成
  - **Tavily**（联网搜索）：用于知识库未命中时的搜索兜底（建议配置）
- [ ] 后端 backend 已升级并正常运行（知识库文档通过 backend 管理并同步到 AI Service）

---

## 3. 需要修改/替换的文件清单

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `ai-service/app/services/chat_service.py` | 替换/合并 | 核心逻辑：主动检索知识库、web_search 兜底、ToolMessage 格式 |
| `ai-service/app/tools/knowledge_tools.py` | 替换/合并 | query_knowledge / web_search 工具读取配置 |
| `ai-service/app/config.py` | 替换/合并 | 默认相似度阈值调整为 0.7 |
| `ai-service/app/stores/vector_store.py` | 确认存在 | Chroma 向量存储实现 |
| `ai-service/requirements.txt` | 确认依赖 | 已包含 `langchain-chroma`、`chromadb`、`tavily-python` |
| `ai-service/.env.example` | 修改 | 补充 Tavily / Chroma 环境变量 |
| `backend` | 视情况 | 若已有知识库管理功能则无需修改；若 backend 有更新需重新部署 jar |
| `web-admin` | 视情况 | 若已有知识库管理页面则无需修改；若有更新需重新构建静态资源 |
| `deploy/docker-compose.yml` | 修改 | 开发环境 AI Service 配置 |
| `deploy/docker-compose.prod.yml` | 修改 | 生产环境 AI Service 配置 + nginx 挂载 |
| `deploy/.env` | 修改 | 服务器真实环境变量 |
| `deploy/.env.example` | 修改 | 服务器环境变量模板 |

> **注意**：
> - `ai-service/.env` 为本地开发文件，不应提交到服务器。服务器使用 `deploy/.env`。
> - 本次升级核心在 ai-service，backend 与 web-admin 主要负责知识库文档的管理入口，若版本已支持则无需替换代码。

---

## 4. 代码变更详情

### 4.1 `ai-service/app/services/chat_service.py`

新增/修改内容：

- 新增 `_bootstrap_knowledge_data` 方法：
  - 先调用 `knowledge_service.query()` 检索知识库
  - 若结果为空，自动查找 `web_search` 工具并调用 Tavily
  - 将结果以 `AIMessage(tool_calls) + ToolMessage` 形式注入 LLM 上下文
- `ToolMessage.content` 改为 JSON 格式，包含 `source_type` 字段，便于模型按系统提示标注来源
- `chat()` 中推荐意图与知识意图独立处理，避免重叠意图跳过推荐引导

关键变更后的调用示例：

```python
results = await self.knowledge_service.query(
    query=query,
    top_k=3,
    threshold=self.settings.knowledge_similarity_threshold,
)
```

### 4.2 `ai-service/app/tools/knowledge_tools.py`

修改内容：

- `query_knowledge` 的 `threshold` 不再硬编码 0.2，改为读取 `settings.knowledge_similarity_threshold`
- `web_search` 的 `max_results` 默认读取 `settings.tavily_max_results`

```python
# query_knowledge
raw_results = await service.query(
    query=query,
    top_k=top_k,
    threshold=settings.knowledge_similarity_threshold,
)

# web_search
if max_results is None:
    max_results = settings.tavily_max_results
```

### 4.3 `ai-service/app/config.py`

修改默认阈值：

```python
knowledge_similarity_threshold: float = Field(default=0.7)
```

配置项完整列表：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `chroma_persist_directory` | `./data/chroma` | Chroma 数据持久化目录 |
| `knowledge_similarity_threshold` | `0.7` | 知识库相似度阈值 |
| `embedding_model` | `text-embedding-v2` | Embedding 模型 |
| `embedding_base_url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | Embedding 服务地址 |
| `embedding_api_key` | 空 | Embedding API Key（必填） |
| `tavily_api_key` | 空 | Tavily API Key（强烈建议） |
| `tavily_max_results` | `3` | Tavily 最大返回结果数 |

---

## 5. 依赖安装说明

### 5.1 Docker 部署（推荐）

Dockerfile 已包含所需依赖，无需手动安装：

```text
langchain-chroma>=0.2.0
chromadb>=0.5.0
tavily-python>=0.8.0
```

只需重新构建镜像：

```bash
cd deploy
docker-compose -f docker-compose.prod.yml build ai-service
```

### 5.2 本地直接运行（开发/测试）

如果服务器上不是 Docker 运行，而是直接运行 Python：

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate  # Linux/Mac
# 或 .venv\Scripts\activate  # Windows

pip install -r requirements.txt
```

确认关键依赖已安装：

```bash
python -c "import chromadb; import tavily; import langchain_chroma; print('ok')"
```

---

## 6. Docker Compose 配置变更

### 6.1 `deploy/docker-compose.yml`（开发环境）

在 `ai-service` 服务的 `environment` 中新增：

```yaml
      # 联网搜索配置（知识库未命中时兜底）
      TAVILY_API_KEY: ${TAVILY_API_KEY:-}
      TAVILY_MAX_RESULTS: ${TAVILY_MAX_RESULTS:-3}
      # 知识库向量存储配置
      CHROMA_PERSIST_DIRECTORY: ${CHROMA_PERSIST_DIRECTORY:-/app/data/chroma}
      KNOWLEDGE_SIMILARITY_THRESHOLD: ${KNOWLEDGE_SIMILARITY_THRESHOLD:-0.7}
```

在 `ai-service` 服务中新增 volumes：

```yaml
    volumes:
      - ai_service_chroma_data:${CHROMA_PERSIST_DIRECTORY:-/app/data/chroma}
```

在顶层 `volumes:` 中新增：

```yaml
volumes:
  mysql_data:
  redis_data:
  ai_service_chroma_data:
```

### 6.2 `deploy/docker-compose.prod.yml`（生产环境）

与开发环境相同，在 `ai-service` 服务中新增：

```yaml
      # 联网搜索配置（知识库未命中时兜底）
      TAVILY_API_KEY: ${TAVILY_API_KEY:-}
      TAVILY_MAX_RESULTS: ${TAVILY_MAX_RESULTS:-3}
      # 知识库向量存储配置
      CHROMA_PERSIST_DIRECTORY: ${CHROMA_PERSIST_DIRECTORY:-/app/data/chroma}
      KNOWLEDGE_SIMILARITY_THRESHOLD: ${KNOWLEDGE_SIMILARITY_THRESHOLD:-0.7}
```

新增 volumes：

```yaml
    volumes:
      - ai_service_chroma_data:${CHROMA_PERSIST_DIRECTORY:-/app/data/chroma}
```

顶层 `volumes:`：

```yaml
volumes:
  mysql_data:
  redis_data:
  ai_service_chroma_data:
```

---

## 7. 环境变量配置

### 7.1 服务器 `deploy/.env` 需要补充的内容

```bash
# 联网搜索配置（知识库未命中时通过 Tavily 兜底）
# 强烈建议配置，否则装备/品牌类问题无法联网补充答案
TAVILY_API_KEY=your-tavily-api-key-here
TAVILY_MAX_RESULTS=3

# 知识库向量存储配置
CHROMA_PERSIST_DIRECTORY=/app/data/chroma
KNOWLEDGE_SIMILARITY_THRESHOLD=0.7
```

### 7.2 必须确认已配置的环境变量

```bash
# AI 对话
LLM_MODEL=qwen-plus
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_API_KEY=your-llm-api-key-here

# 知识库 Embedding（必填）
EMBEDDING_MODEL=text-embedding-v2
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
EMBEDDING_API_KEY=your-embedding-api-key-here

# 内部接口鉴权（需与 backend 保持一致）
INTERNAL_API_TOKEN=your-internal-api-token-at-least-32-characters

# Redis
REDIS_PASSWORD=your-redis-password

# 生产域名/IP
DOMAIN=your-domain-or-ip
```

---

## 8. 知识库数据同步

### 8.1 新环境首次部署

Chroma 向量库初始为空，需要通过 web-admin 后台导入知识库文档：

1. 登录 web-admin：`http://<DOMAIN>/admin/`
2. 进入「知识库管理」
3. 点击「新增」或「上传文件」，添加游泳安全、技巧、装备等文档
4. 文档保存后会自动调用 ai-service 的内部接口建立向量索引

### 8.2 已有 MySQL 数据但向量库为空

如果 backend 的 MySQL 中已有知识库文档，但 Chroma 数据卷为空，需要手动触发重建：

**方式一：web-admin 逐个重建**

对每个已启用的文档执行「禁用」→「启用」，或「编辑」→「保存」，触发 `knowledgeRebuild`。

**方式二：直接调用 backend 内部重建（可选）**

如果文档数量较多，可联系开发团队提供一次性全量同步脚本，通过调用 backend 的 Admin API 批量触发重建。

### 8.3 数据备份

Chroma 数据已挂载到 Docker 命名卷 `ai_service_chroma_data`，容器重启不会丢失。如需额外备份：

```bash
# 查看卷实际路径
docker volume inspect deploy_ai_service_chroma_data

# 备份
docker run --rm -v deploy_ai_service_chroma_data:/data -v $(pwd):/backup alpine \
  tar czvf /backup/chroma-backup-$(date +%Y%m%d).tar.gz -C /data .
```

### 8.4 使用脚本自动同步本地知识库到生产

如果你已在本地（或测试环境）通过 web-admin 维护好知识库文档，并希望在部署到生产时一次性把**文档元数据（MySQL 表 `ai_knowledge_document`）**和**向量索引（Chroma 数据目录）**同步到生产服务器，可以使用提供的 PowerShell 脚本，避免逐篇手动上传。

#### 脚本位置

```text
scripts/sync-knowledge-to-prod.ps1
```

#### 脚本功能

- 导出本地 MySQL 表 `ai_knowledge_document` 的 SQL 文件
- 打包本地 `ai-service/data/chroma` 向量库
- 通过 SSH/SCP 上传到生产服务器 `/tmp`
- 在生产端自动备份原 MySQL 表和 Chroma 数据
- 事务性替换 MySQL 表数据
- 原子性替换 Chroma 目录
- 自动重启 `leyo-ai-service` 并做健康检查

#### 使用前准备

1. 本地安装 MySQL 客户端（包含 `mysqldump`，且在 PATH 中）
2. 本地安装 PowerShell 5.1 或更高版本
3. 本地安装 OpenSSH 客户端（`ssh`、`scp`）和 `tar`
4. 安装 Posh-SSH 模块（脚本会自动安装）
5. 确认生产服务器已安装 `mysql`、`mysqldump`、`tar`、`systemctl`、`curl`
6. 确认生产服务器 `leyo-ai-service` 以 systemd 服务运行

#### 参数说明

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `-ServerIP` | 是 | - | 生产服务器 IP |
| `-ServerUser` | 否 | `root` | SSH 用户名 |
| `-LocalMySqlHost` | 否 | `localhost` | 本地 MySQL 主机 |
| `-LocalMySqlPort` | 否 | `3306` | 本地 MySQL 端口 |
| `-LocalMySqlUser` | 否 | `root` | 本地 MySQL 用户名 |
| `-LocalMySqlPassword` | 是 | - | 本地 MySQL 密码（SecureString） |
| `-LocalMySqlDatabase` | 否 | `leyo` | 本地数据库名 |
| `-ProdMySqlHost` | 否 | `localhost` | 生产 MySQL 主机（从服务器视角看） |
| `-ProdMySqlPort` | 否 | `3306` | 生产 MySQL 端口 |
| `-ProdMySqlUser` | 否 | `root` | 生产 MySQL 用户名 |
| `-ProdMySqlPassword` | 是 | - | 生产 MySQL 密码（SecureString） |
| `-ProdMySqlDatabase` | 否 | `leyo_prod` | 生产数据库名 |
| `-LocalChromaPath` | 否 | `ai-service/data/chroma` | 本地 Chroma 目录 |
| `-RemoteChromaPath` | 否 | `/opt/leyo-swimming/ai-service/data/chroma` | 生产 Chroma 目录 |
| `-RemoteAiServiceDir` | 否 | `/opt/leyo-swimming/ai-service` | 生产 ai-service 目录 |
| `-AiServiceSystemd` | 否 | `leyo-ai-service` | ai-service systemd 服务名 |
| `-BackupDir` | 否 | `/opt/backups` | 生产端备份存放目录 |
| `-AcceptHostKey` | 否 | - | 自动接受服务器 SSH 主机密钥 |
| `-HostKeyFingerprint` | 否 | - | 服务器 SSH 主机密钥指纹 |
| `-SkipBackup` | 否 | - | 跳过生产端备份（不建议） |
| `-DryRun` | 否 | - | 只预览，不实际执行 |

#### 使用示例

**方式一：交互式输入密码（推荐首次使用）**

```powershell
.\scripts\sync-knowledge-to-prod.ps1 `
  -ServerIP '14.103.149.202' `
  -AcceptHostKey `
  -LocalMySqlPassword (Read-Host -AsSecureString "本地 MySQL 密码") `
  -ProdMySqlPassword (Read-Host -AsSecureString "生产 MySQL 密码")
```

**方式二：DryRun 预览**

```powershell
.\scripts\sync-knowledge-to-prod.ps1 `
  -ServerIP '14.103.149.202' `
  -AcceptHostKey `
  -DryRun `
  -LocalMySqlPassword (Read-Host -AsSecureString "本地 MySQL 密码") `
  -ProdMySqlPassword (Read-Host -AsSecureString "生产 MySQL 密码")
```

**方式三：本地与生产使用非 root 用户或非默认端口**

```powershell
.\scripts\sync-knowledge-to-prod.ps1 `
  -ServerIP '14.103.149.202' `
  -AcceptHostKey `
  -LocalMySqlUser 'leyo' `
  -LocalMySqlPassword (Read-Host -AsSecureString "本地 MySQL 密码") `
  -ProdMySqlHost '127.0.0.1' `
  -ProdMySqlPort 3307 `
  -ProdMySqlUser 'leyo_prod' `
  -ProdMySqlPassword (Read-Host -AsSecureString "生产 MySQL 密码")
```

#### 注意事项

1. **首次使用务必加 `-DryRun`**，确认脚本识别到的本地/生产数据库、Chroma 路径符合预期。
2. 该脚本会**覆盖**生产环境 `ai_knowledge_document` 全表，请确保本地是要同步的完整数据。
3. 生产端备份默认存放在 `/opt/backups`，请确保磁盘空间充足。
4. MySQL 密码通过临时 `--defaults-extra-file` 文件传递给本地 `mysqldump`，远程通过 `MYSQL_PWD` 环境变量传递，脚本退出后会清理临时文件。
5. 同步过程中会停止 `leyo-ai-service`，服务中断时间取决于数据量，请避开业务高峰。
6. 同步完成后脚本会检查 `http://localhost:8000/health`，若服务未恢复会抛出错误。
7. 若生产 MySQL 与 ai-service 不在同一台服务器，请调整 `-ProdMySqlHost` 为生产 MySQL 实际地址，并确保生产服务器能通过该地址访问 MySQL。
8. 该脚本只同步知识库数据，不会同步 `.env`、代码包或模型文件。代码升级请使用 `scripts/upgrade-ai-service-rag.ps1`。

---

## 9. 本地构建与上传到服务器

> 本节说明如何在本地 Windows/Mac/Linux 开发机编译/构建三个服务，并将产物上传到服务器进行部署。

### 9.1 前置准备

确保本地已安装：

- Git
- Python 3.11+ 与 pip（用于 ai-service 宿主机部署）
- Docker 与 Docker Compose（可选，用于构建 ai-service Docker 镜像）
- JDK 21 + Maven（用于编译 backend）
- Node.js 20+ + npm/pnpm（用于构建 web-admin）
- 服务器 SSH 访问权限（用于上传产物）

> 本次部署提供两种方式：
> - **方式一：Docker 部署**（ai-service 运行在容器内）
> - **方式二：宿主机直接部署**（ai-service 直接在服务器操作系统上运行，使用 systemd 或启动脚本管理）
>
> 请根据实际生产环境选择。

### 9.2 本地准备 ai-service 部署产物

#### 方式 A：Docker 镜像（推荐服务器有 Docker 环境时使用）

本地构建镜像，导出 tar 上传到服务器：

```bash
# 1. 进入 ai-service 目录
cd ai-service

# 2. 构建镜像
docker build -t leyo-ai-service:v1.0.0 .

# 3. 导出镜像为 tar 文件
docker save -o leyo-ai-service-v1.0.0.tar leyo-ai-service:v1.0.0

# 4. 上传到服务器
scp leyo-ai-service-v1.0.0.tar root@<服务器IP>:/opt/leyoSwimming/

# 5. 在服务器上导入镜像
ssh root@<服务器IP> "cd /opt/leyoSwimming && docker load -i leyo-ai-service-v1.0.0.tar"
```

#### 方式 B：宿主机直接部署（ai-service 直接运行在服务器操作系统上）

宿主机部署不需要构建 Docker 镜像，只需将代码和依赖上传到服务器。

**本地准备：**

```bash
# 1. 确认代码已提交并推送
git add .
git commit -m "feat: ai-service RAG + Tavily fallback"
git push origin main

# 2. 将 ai-service 代码打包（排除 venv、logs、data 等目录）
cd ai-service
tar czvf ../ai-service-release.tar.gz \
  --exclude='.venv' \
  --exclude='logs' \
  --exclude='data' \
  --exclude='__pycache__' \
  --exclude='*.pyc' \
  .

# 3. 上传到服务器
cd ..
scp ai-service-release.tar.gz root@<服务器IP>:/opt/leyoSwimming/
```

**服务器上部署：**

```bash
ssh root@<服务器IP> << 'EOF'
cd /opt/leyoSwimming

# 1. 解压覆盖旧代码
mkdir -p ai-service
tar xzvf ai-service-release.tar.gz -C ai-service

# 2. 创建/激活虚拟环境
cd ai-service
python3 -m venv .venv
source .venv/bin/activate

# 3. 安装依赖
pip install --upgrade pip
pip install -r requirements.txt

# 4. 确认关键依赖
python -c "import chromadb; import tavily; import langchain_chroma; print('dependencies ok')"

# 5. 配置环境变量（按第 7 节编辑 deploy/.env 或 ai-service/.env）
vim .env
EOF
```

> 方式 B 适合服务器没有 Docker、或希望直接管理 Python 进程的场景。

### 9.3 本地编译 backend

```bash
# 1. 进入 backend 目录
cd backend

# 2. 执行 Maven 打包（跳过测试，服务器环境可能无法跑测试）
./mvnw clean package -DskipTests

# 3. 确认 jar 生成
ls target/*.jar
# 例如：target/leyo-swimming-0.0.1-SNAPSHOT.jar

# 4. 上传到服务器，替换 deploy 目录下的 jar
scp target/leyo-swimming-0.0.1-SNAPSHOT.jar root@<服务器IP>:/opt/leyoSwimming/leyo-swimming-backend.jar
```

> 注意：如果 `deploy/docker-compose.prod.yml` 中挂载的 jar 路径不同，请按实际路径上传。

### 9.4 本地构建 web-admin

```bash
# 1. 进入 web-admin 目录
cd web-admin

# 2. 安装依赖
npm install

# 3. 生产构建
npm run build

# 4. 确认构建产物
ls dist/

# 5. 上传到服务器 nginx 静态资源目录
rsync -avz --delete dist/ root@<服务器IP>:/opt/leyoSwimming/web-admin-dist/
```

> `rsync --delete` 会删除服务器上多余的旧文件，保持与本地构建产物一致。

### 9.5 一次性构建并上传三个服务

#### 脚本 A：Docker 方式

```bash
#!/bin/bash
# build-and-upload-docker.sh
set -e

SERVER_IP=$1
if [ -z "$SERVER_IP" ]; then
  echo "Usage: ./build-and-upload-docker.sh <服务器IP>"
  exit 1
fi

echo "=== 构建 ai-service Docker 镜像 ==="
cd ai-service
docker build -t leyo-ai-service:v1.0.0 .
docker save -o leyo-ai-service-v1.0.0.tar leyo-ai-service:v1.0.0
scp leyo-ai-service-v1.0.0.tar root@$SERVER_IP:/opt/leyoSwimming/
cd ..

echo "=== 编译 backend ==="
cd backend
./mvnw clean package -DskipTests
scp target/leyo-swimming-0.0.1-SNAPSHOT.jar root@$SERVER_IP:/opt/leyoSwimming/leyo-swimming-backend.jar
cd ..

echo "=== 构建 web-admin ==="
cd web-admin
npm install
npm run build
rsync -avz --delete dist/ root@$SERVER_IP:/opt/leyoSwimming/web-admin-dist/
cd ..

echo "=== 上传完成，请在服务器上执行 Docker 部署步骤 ==="
```

执行：

```bash
./build-and-upload-docker.sh 123.45.67.89
```

#### 脚本 B：宿主机方式

```bash
#!/bin/bash
# build-and-upload-host.sh
set -e

SERVER_IP=$1
if [ -z "$SERVER_IP" ]; then
  echo "Usage: ./build-and-upload-host.sh <服务器IP>"
  exit 1
fi

echo "=== 打包 ai-service 代码 ==="
cd ai-service
tar czvf ../ai-service-release.tar.gz \
  --exclude='.venv' \
  --exclude='logs' \
  --exclude='data' \
  --exclude='__pycache__' \
  --exclude='*.pyc' \
  .
cd ..
scp ai-service-release.tar.gz root@$SERVER_IP:/opt/leyoSwimming/

echo "=== 编译 backend ==="
cd backend
./mvnw clean package -DskipTests
scp target/leyo-swimming-0.0.1-SNAPSHOT.jar root@$SERVER_IP:/opt/leyoSwimming/leyo-swimming-backend.jar
cd ..

echo "=== 构建 web-admin ==="
cd web-admin
npm install
npm run build
rsync -avz --delete dist/ root@$SERVER_IP:/opt/leyoSwimming/web-admin-dist/
cd ..

echo "=== 上传完成，请在服务器上执行宿主机部署步骤 ==="
```

执行：

```bash
./build-and-upload-host.sh 123.45.67.89
```

上传完成后，在服务器上执行：

```bash
ssh root@123.45.67.89
cd /opt/leyoSwimming
chmod +x deploy/upgrade-ai-service.sh
./deploy/upgrade-ai-service.sh
```

> `deploy/upgrade-ai-service.sh` 是服务器端升级脚本，默认只升级 ai-service。

### Windows 一键升级脚本

如果你使用 Windows 开发机，可以直接运行已提供的 PowerShell 脚本：

```powershell
.\scripts\upgrade-ai-service-rag.ps1 -ServerIP '123.45.67.89' -AcceptHostKey
```

该脚本会自动完成：本地打包 ai-service → SSH 上传 → 备份服务器 .env 和 Chroma 数据 → 解压覆盖代码 → 更新 Python 依赖 → 重启服务 → 健康检查。

可选参数：

| 参数 | 说明 |
|------|------|
| `-ServerIP` | 目标服务器 IP（必填） |
| `-ServerUser` | SSH 用户名，默认 root |
| `-RemotePath` | 服务器部署路径，默认 `/opt/leyoSwimming` |
| `-AcceptHostKey` | 自动接受服务器 SSH 主机密钥 |
| `-ReinstallAIDeps` | 强制重新安装所有 Python 依赖 |
| `-SkipBackup` | 跳过备份 |
| `-SkipUpload` | 跳过上传（用于服务器已有人工上传的场景） |
| `-DryRun` | 只检查环境变量和连接，不执行实际操作 |

---

## 10. 服务器部署步骤

> 本节提供 Docker 与宿主机两种部署方式。请根据实际环境选择对应小节。

### 10.1 方式一：Docker 方式完整升级流程

#### 步骤 1：登录服务器并进入项目目录

```bash
cd /opt/leyoSwimming
```

#### 步骤 2：拉取最新代码

```bash
git pull origin main
```

#### 步骤 3：备份关键数据

```bash
# 备份环境变量
cp deploy/.env deploy/.env.bak.$(date +%Y%m%d)

# 备份 Chroma 向量库数据（Docker 卷方式）
docker run --rm -v deploy_ai_service_chroma_data:/data -v $(pwd)/backup:/backup alpine \
  tar czvf /backup/chroma-backup-$(date +%Y%m%d).tar.gz -C /data .

# 备份 backend 上传文件（如适用）
tar czvf /backup/uploads-backup-$(date +%Y%m%d).tar.gz ./uploads 2>/dev/null || true

# 备份 MySQL 数据（强烈推荐）
docker exec leyo-mysql mysqldump -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} > /backup/mysql-backup-$(date +%Y%m%d).sql
```

#### 步骤 4：更新环境变量

```bash
vim deploy/.env
```

按第 7 节补充以下内容：

```bash
# ai-service 新增
TAVILY_API_KEY=your-tavily-api-key-here
TAVILY_MAX_RESULTS=3
CHROMA_PERSIST_DIRECTORY=/app/data/chroma
KNOWLEDGE_SIMILARITY_THRESHOLD=0.7

# 确认以下变量与 backend 保持一致
INTERNAL_API_TOKEN=your-internal-api-token-at-least-32-characters
AI_SERVICE_BASE_URL=http://leyo-ai-service:8000  # Docker 网络内
```

#### 步骤 5：升级 ai-service

```bash
cd deploy

# 停止旧版 ai-service
docker-compose -f docker-compose.prod.yml stop ai-service

# 重新构建镜像（拉取新依赖）
docker-compose -f docker-compose.prod.yml build --no-cache ai-service

# 启动新版 ai-service
docker-compose -f docker-compose.prod.yml up -d ai-service

# 查看启动日志
docker logs -f leyo-ai-service
```

等待日志出现类似以下内容，表示启动成功：

```text
Uvicorn running on http://0.0.0.0:8000
```

#### 步骤 6：升级 backend

```bash
cd deploy

# 停止 backend
docker-compose -f docker-compose.prod.yml stop backend

# 若有新的 backend jar，替换文件
cp ../leyo-swimming-backend.jar ./leyo-swimming-backend.jar

# 启动 backend
docker-compose -f docker-compose.prod.yml up -d backend

# 查看启动日志
docker logs -f leyo-backend
```

确认 backend 启动成功后，检查与 ai-service 的内部接口连通性：

```bash
docker exec leyo-backend wget --no-verbose --tries=1 --spider http://leyo-ai-service:8000/health
```

#### 步骤 7：升级 web-admin

**情况 A：web-admin 使用预编译静态资源**

```bash
# 已在本地构建并上传到 /opt/leyoSwimming/web-admin-dist/
# 确认 nginx 已挂载该目录
docker exec leyo-nginx ls -la /usr/share/nginx/html/admin
```

**情况 B：web-admin 使用 Docker 容器运行**

```bash
cd deploy
docker-compose -f docker-compose.prod.yml stop web-admin
docker-compose -f docker-compose.prod.yml build --no-cache web-admin
docker-compose -f docker-compose.prod.yml up -d web-admin
```

#### 步骤 8：重启 nginx（配置变更时）

```bash
cd deploy
docker-compose -f docker-compose.prod.yml restart nginx
```

#### 步骤 9：三服务状态检查

```bash
docker-compose -f docker-compose.prod.yml ps
```

预期所有服务状态为 `Up (healthy)`。

---

### 10.2 方式二：宿主机方式完整升级流程

适用于 ai-service、backend、web-admin 均直接运行在服务器操作系统上的场景。

> 如果你已经通过 `build-and-upload-host.sh` 上传了代码包，可以直接执行 `deploy/upgrade-ai-service.sh` 完成升级，无需手动执行以下步骤。

#### 步骤 1：登录服务器并进入项目目录

```bash
cd /opt/leyoSwimming
```

#### 步骤 2：备份关键数据

```bash
# 备份环境变量
cp ai-service/.env ai-service/.env.bak.$(date +%Y%m%d) 2>/dev/null || true
cp backend/application-prod.yml backend/application-prod.yml.bak.$(date +%Y%m%d) 2>/dev/null || true

# 备份 Chroma 数据
 tar czvf /backup/chroma-backup-$(date +%Y%m%d).tar.gz ./ai-service/data/chroma 2>/dev/null || true

# 备份 backend 上传文件
tar czvf /backup/uploads-backup-$(date +%Y%m%d).tar.gz ./uploads 2>/dev/null || true

# 备份 MySQL 数据（强烈推荐）
mysqldump -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} > /backup/mysql-backup-$(date +%Y%m%d).sql
```

#### 步骤 3：更新环境变量

```bash
vim ai-service/.env
```

按第 7 节补充：

```bash
TAVILY_API_KEY=your-tavily-api-key-here
TAVILY_MAX_RESULTS=3
CHROMA_PERSIST_DIRECTORY=./data/chroma
KNOWLEDGE_SIMILARITY_THRESHOLD=0.7

# 确认以下变量与 backend 保持一致
INTERNAL_API_TOKEN=your-internal-api-token-at-least-32-characters
```

#### 步骤 4：升级 ai-service

假设已通过 9.2 方式 B 将 `ai-service-release.tar.gz` 上传到 `/opt/leyoSwimming/`。

```bash
cd /opt/leyoSwimming

# 1. 停止旧版 ai-service（根据实际进程管理方式调整）
systemctl stop leyo-ai-service
# 或 pkill -f "uvicorn app.main:app"

# 2. 解压覆盖旧代码
mkdir -p ai-service
tar xzvf ai-service-release.tar.gz -C ai-service

# 3. 进入 ai-service 目录
cd ai-service

# 4. 创建/激活虚拟环境
python3 -m venv .venv
source .venv/bin/activate

# 5. 安装/更新依赖
pip install --upgrade pip
pip install -r requirements.txt

# 6. 确认关键依赖
python -c "import chromadb; import tavily; import langchain_chroma; print('dependencies ok')"

# 7. 启动 ai-service
systemctl start leyo-ai-service
# 或使用启动脚本：./start-prod.sh
```

**systemd 服务文件示例** `/etc/systemd/system/leyo-ai-service.service`：

```ini
[Unit]
Description=Leyo AI Service
After=network.target

[Service]
Type=simple
User=leyo
Group=leyo
WorkingDirectory=/opt/leyoSwimming/ai-service
Environment=PATH=/opt/leyoSwimming/ai-service/.venv/bin
Environment=PYTHONUNBUFFERED=1
ExecStart=/opt/leyoSwimming/ai-service/.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

创建后执行：

```bash
systemctl daemon-reload
systemctl enable leyo-ai-service
systemctl start leyo-ai-service
```

#### 步骤 5：升级 backend

```bash
cd /opt/leyoSwimming

# 1. 停止 backend
systemctl stop leyo-backend

# 2. 替换 jar（已通过 scp 上传）
cp leyo-swimming-backend.jar backend/leyo-swimming-backend.jar

# 3. 启动 backend
systemctl start leyo-backend
```

**systemd 服务文件示例** `/etc/systemd/system/leyo-backend.service`：

```ini
[Unit]
Description=Leyo Backend
After=network.target mysql.service redis.service

[Service]
Type=simple
User=leyo
Group=leyo
WorkingDirectory=/opt/leyoSwimming/backend
ExecStart=/usr/bin/java -jar /opt/leyoSwimming/backend/leyo-swimming-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

#### 步骤 6：升级 web-admin

```bash
cd /opt/leyoSwimming

# 静态资源已通过 rsync 上传到 /opt/leyoSwimming/web-admin-dist/
# 确认 nginx 配置指向该目录
nginx -t
systemctl reload nginx
```

**nginx 配置示例** `/etc/nginx/conf.d/leyo-web-admin.conf`：

```nginx
server {
    listen 80;
    server_name your-domain-or-ip;

    location /admin/ {
        alias /opt/leyoSwimming/web-admin-dist/;
        try_files $uri $uri/ /admin/index.html;
    }
}
```

#### 步骤 7：三服务状态检查

```bash
systemctl status leyo-ai-service
systemctl status leyo-backend
systemctl status nginx
```

预期所有服务均为 `active (running)`。

---

### 10.3 仅升级 ai-service（Docker 方式）

若确认 backend 与 web-admin 无需更新，可仅升级 ai-service：

```bash
cd deploy

# 停止旧版 ai-service
docker-compose -f docker-compose.prod.yml stop ai-service

# 重新构建并启动
docker-compose -f docker-compose.prod.yml up -d --build --no-deps ai-service

# 查看日志
docker logs -f leyo-ai-service
```

> `--no-deps` 表示不重启依赖服务（redis、backend），减少停机影响。

---

### 10.4 仅升级 ai-service（宿主机方式）

```bash
cd /opt/leyoSwimming

# 1. 停止 ai-service
systemctl stop leyo-ai-service

# 2. 解压新代码
tar xzvf ai-service-release.tar.gz -C ai-service

# 3. 更新依赖
cd ai-service
source .venv/bin/activate
pip install -r requirements.txt

# 4. 启动
systemctl start leyo-ai-service

# 5. 查看日志
journalctl -u leyo-ai-service -f
```

## 11. 验证步骤

### 11.1 服务健康检查

```bash
# AI Service 健康接口
curl http://localhost:8000/health

# 预期返回 {"status":"ok"} 或类似
```

### 11.2 知识库问题测试

```bash
curl -X POST http://localhost:8000/api/ai-assistant/chat \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: <INTERNAL_API_TOKEN>" \
  -d '{
    "session_id": "sess_upgrade_test_1",
    "message": "游泳时抽筋怎么办"
  }'
```

预期：返回基于知识库内容的回答，开头标注来源。

### 11.3 联网搜索兜底测试

```bash
curl -X POST http://localhost:8000/api/ai-assistant/chat \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: <INTERNAL_API_TOKEN>" \
  -d '{
    "session_id": "sess_upgrade_test_2",
    "message": "速比涛这个品牌有推荐的新手入手的型号吗"
  }'
```

预期：

- 若配置了 `TAVILY_API_KEY`：返回基于网络搜索的回答
- 若未配置 `TAVILY_API_KEY`：返回"手头没有相关产品数据，不好凭空建议具体型号"等正常回答，**不再返回教练/套餐信息**

### 11.4 日志检查

```bash
docker logs leyo-ai-service | grep -E "bootstrap_knowledge|tool_web_search|tavily"
```

应能看到：

```text
bootstrap_knowledge_query
knowledge_query ... result_count=0
bootstrap_knowledge_empty
tool_web_search ...
```

### 11.5 测试汇总

升级完成后，应确认以下测试通过：

| 测试项 | 命令 | 预期结果 |
|--------|------|----------|
| AI Service 单元测试 | `pytest tests/test_chat_service_unit.py tests/test_knowledge_tools.py` | 31 passed |
| 健康检查 | `curl http://localhost:8000/health` | HTTP 200 |
| 知识库问题 | 调用 `/api/ai-assistant/chat` | 基于知识库回答 |
| 联网搜索兜底 | 调用 `/api/ai-assistant/chat` | 不返回教练/套餐 |

---

## 12. 常见问题排查

### 12.1 AI Service 启动失败：缺少 chromadb / tavily

**现象**：容器启动报错 `ModuleNotFoundError: No module named 'chromadb'`

**解决**：确认 `ai-service/requirements.txt` 包含 `chromadb` 和 `tavily-python`，并重新构建镜像：

```bash
cd deploy
docker-compose -f docker-compose.prod.yml build --no-cache ai-service
```

### 12.2 知识库检索始终为空

**排查步骤**：

1. 确认 `EMBEDDING_API_KEY` 有效
2. 确认 web-admin 中已添加并启用知识库文档
3. 查看 ai-service 日志是否有 `knowledge_ingest` 相关记录
4. 进入容器检查 Chroma 目录是否有数据：

```bash
docker exec -it leyo-ai-service ls -la /app/data/chroma
```

### 12.3 未触发 Tavily 搜索

**排查步骤**：

1. 确认 `TAVILY_API_KEY` 已配置且有效
2. 确认问题是知识类问题（日志中 `is_knowledge=true`）
3. 查看日志是否有 `bootstrap_knowledge_empty` 和 `tool_web_search`
4. 若日志显示 `tavily_api_key_missing`，说明环境变量未传入容器

### 12.4 返回教练/套餐信息而不是知识回答

**原因**：可能是旧版本镜像仍在运行，或 `is_knowledge` 未识别。

**解决**：

1. 确认 ai-service 镜像已更新
2. 确认 `chat_service.py` 已包含 `_bootstrap_knowledge_data` 方法
3. 测试问题时查看日志中的 `chat_intent_detected` 事件

---

## 13. 回滚方案

若升级后出现问题，可按以下步骤回滚：

```bash
cd deploy

# 1. 停止新版本
docker-compose -f docker-compose.prod.yml down

# 2. 恢复环境变量
cp deploy/.env.bak deploy/.env

# 3. 恢复旧版本代码
cd ..
git checkout <升级前的 commit/tag>

# 4. 使用旧版本启动
cd deploy
docker-compose -f docker-compose.prod.yml up -d
```

> 注意：若回滚前已写入新的 Chroma 数据格式，旧版本可能无法读取。建议在升级前备份 `ai_service_chroma_data` 卷。

---

## 14. 升级检查清单

### 通用准备

- [ ] 已备份 `deploy/.env`
- [ ] 已备份 MySQL 数据库
- [ ] 已备份 Chroma 数据卷或 `ai-service/data/chroma`
- [ ] 已备份 `uploads` 目录
- [ ] 已获取 `TAVILY_API_KEY`、`EMBEDDING_API_KEY`、`LLM_API_KEY`

### ai-service

- [ ] 已更新 `deploy/.env` 中的新环境变量
- [ ] 已确认 `deploy/docker-compose.prod.yml` 包含 Chroma 卷挂载
- [ ] 已重新构建 ai-service 镜像
- [ ] 已通过 `http://localhost:8000/health` 健康检查
- [ ] 已通过知识库问题测试
- [ ] 已通过联网搜索兜底测试

### backend

- [ ] 已确认 `INTERNAL_API_TOKEN` 与 ai-service 一致
- [ ] 已确认 `AI_SERVICE_BASE_URL` 指向正确的 ai-service 地址
- [ ] backend 已正常启动（`leyo-backend` 状态为 `Up (healthy)`）
- [ ] backend 能访问 ai-service 内部接口
- [ ] 知识库文档添加/修改后能同步到 ai-service

### web-admin

- [ ] web-admin 静态资源已部署到 nginx 挂载目录
- [ ] 能正常访问 `http://<DOMAIN>/admin/`
- [ ] 能正常进入「知识库管理」页面
- [ ] 能正常新增/编辑/启用/禁用知识库文档

### 最终确认

- [ ] `docker-compose -f docker-compose.prod.yml ps` 显示所有服务 `Up (healthy)`
- [ ] 已在 web-admin 中确认知识库文档正常同步
