# leyoSwimming 增量升级部署演练手册

> **文档定位**：服务器已完成首次部署后，使用自动化脚本进行日常增量升级的操作演练。  
> **目标读者**：运维人员 / 开发工程师。  
> **前置条件**：已按《leyoSwimming 生产环境初次部署及升级手册》完成首次部署。  
> **版本**：v1.0  
> **日期**：2026-08-29

---

## 目录

1. [演练目标](#1-演练目标)
2. [脚本与流程概览](#2-脚本与流程概览)
3. [前置准备](#3-前置准备)
4. [场景一：安装并手动触发升级](#4-场景一安装并手动触发升级)
5. [场景二：commit 后自动提醒升级](#5-场景二commit-后自动提醒升级)
6. [场景三：commit 后全自动部署](#6-场景三commit-后全自动部署)
7. [验证升级结果](#7-验证升级结果)
8. [回滚演练](#8-回滚演练)
9. [常见问题与排查](#9-常见问题与排查)
10. [安全检查清单](#10-安全检查清单)

---

## 1. 演练目标

完成以下三种升级场景的端到端验证：

| 场景 | 触发方式 | 人工确认 | 适用环境 |
|------|----------|----------|----------|
| 手动触发 | 直接执行 `upgrade-server.ps1` | 每次执行时输入密码 | 生产环境首次验证、紧急修复 |
| commit 后提醒 | 安装 `post-commit` hook | 需要输入 y/N | 日常开发分支 |
| commit 后自动部署 | 安装 hook 并启用 AutoDeploy | 无需确认 | 稳定的发布分支/CI 环境 |

---

## 2. 脚本与流程概览

```text
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ 本地代码变更     │────▶│ upgrade-hook.ps1 │────▶│upgrade-server.ps1│
└─────────────────┘     └──────────────────┘     └─────────────────┘
        │                        │                        │
        │                        ▼                        ▼
        │              检测变更模块并提醒           本地构建/上传/重启
        │                        │                        │
        ▼                        ▼                        ▼
   git commit              用户确认或              原子切换产物
                           AutoDeploy             健康检查/备份清理
```

### 脚本说明

| 脚本 | 作用 |
|------|------|
| `scripts/upgrade-server.ps1` | 增量升级核心脚本：构建、上传、原子切换、重启、健康检查 |
| `scripts/upgrade-hook.ps1` | 变更检测与触发器，可手动执行或作为 hook 调用 |
| `scripts/install-upgrade-hook.ps1` | 将 `upgrade-hook.ps1` 注册为 `.git/hooks/post-commit` |

---

## 3. 前置准备

### 3.1 本地环境

1. **PowerShell 5.1 或 PowerShell 7+**
2. **OpenSSH 客户端**（`ssh` / `scp` 可用）
3. **tar**（Windows 10/11 自带）
4. **Posh-SSH** 模块（脚本会自动安装）
5. **Node.js / Yarn 或 npm / pnpm**（前端构建需要）
6. **Java 21 + Maven**（后端构建需要）

### 3.2 获取服务器 SSH 主机密钥指纹

在本地 PowerShell 中执行：

```powershell
ssh-keyscan -t rsa <服务器IP> 2>$null | ForEach-Object { if ($_ -match 'sha256:') { $_ } }
```

或使用 ed25519：

```powershell
ssh-keyscan -t ed25519 <服务器IP> 2>$null | ForEach-Object { if ($_ -match 'ssh-ed25519') { $_ } }
```

记录指纹，例如 `SHA256:xxxxx`。后续所有升级操作均使用该指纹校验服务器身份。

### 3.3 确认服务器已首次部署

登录服务器，确认以下文件存在：

```bash
ssh root@<服务器IP>
test -f /opt/leyo-swimming/deploy/docker-compose.prod.yml && echo "已部署" || echo "未部署"
```

若未部署，请先执行 `scripts/deploy-first-stage.ps1`。

---

## 4. 场景一：安装并手动触发升级

### 4.1 安装交互式升级提醒 hook

在本地仓库根目录打开 PowerShell：

```powershell
cd d:\AI Agent\leyoSwimming
.\scripts\install-upgrade-hook.ps1 -HostKeyFingerprint 'SHA256:xxxxx'
```

预期输出：

```text
post-commit hook 已安装：d:\AI Agent\leyoSwimming\.git\hooks\post-commit
模式：交互式提醒（commit 后会提示输入服务器 IP 并确认升级）
```

### 4.2 修改代码并 commit

以修改后端代码为例：

```powershell
# 修改 backend/src/... 某文件
git add backend/
git commit -m "fix: 修复套餐状态判断"
```

commit 完成后，hook 会自动运行并提示：

```text
========================================
leyoSwimming 升级触发器
========================================
当前 commit：a1b2c3d...
对比范围：HEAD~1..HEAD

检测到变更文件（共 3 个）：
  backend/src/.../PackageService.java
  ...

需要升级的模块：
  - backend

未通过参数或环境变量 LEYO_UPGRADE_SERVER_IP 指定服务器 IP。
请输入要升级的服务器 IP（直接回车则仅提醒，不执行部署）：
```

### 4.3 手动执行升级脚本

如果 hook 中跳过了部署，可手动执行：

```powershell
.\scripts\upgrade-server.ps1 -ServerIP '<服务器IP>' -Modules backend -HostKeyFingerprint 'SHA256:xxxxx'
```

按提示输入 SSH 密码后，脚本会执行：

1. 本地 Maven 构建
2. 上传 jar 到服务器 `.tmp`
3. 原子切换产物并备份旧版本
4. 重启 backend 容器
5. 健康检查

---

## 5. 场景二：commit 后自动提醒升级

### 5.1 设置服务器 IP 环境变量

在当前 PowerShell 会话中：

```powershell
$env:LEYO_UPGRADE_SERVER_IP = '14.103.149.202'
```

或永久设置到用户环境变量：

```powershell
[System.Environment]::SetEnvironmentVariable('LEYO_UPGRADE_SERVER_IP', '14.103.149.202', 'User')
```

### 5.2 重新安装 hook

```powershell
.\scripts\install-upgrade-hook.ps1 -HostKeyFingerprint 'SHA256:xxxxx'
```

### 5.3 修改并 commit

```powershell
git add .
git commit -m "feat: 新增订单查询字段"
```

hook 会检测到变更模块并询问是否升级：

```text
准备使用升级脚本部署到服务器：14.103.149.202
是否立即执行升级？(y/N，默认 N):
```

输入 `y` 开始升级，输入 `N` 取消。

---

## 6. 场景三：commit 后全自动部署

> **警告**：此模式会在每次 commit 后自动部署到生产服务器，仅限稳定的发布分支或受信任的 CI 环境使用。

### 6.1 设置双重授权环境变量

```powershell
$env:LEYO_UPGRADE_SERVER_IP = '14.103.149.202'
$env:LEYO_UPGRADE_AUTO_DEPLOY = '1'
```

### 6.2 安装自动部署 hook

```powershell
.\scripts\install-upgrade-hook.ps1 -AutoDeploy -HostKeyFingerprint 'SHA256:xxxxx'
```

### 6.3 修改并 commit

```powershell
git add .
git commit -m "fix: 修复预约时间显示"
```

hook 检测到 `LEYO_UPGRADE_AUTO_DEPLOY=1` 后，自动调用 `upgrade-server.ps1` 完成构建、上传、重启，无需人工确认。

---

## 7. 验证升级结果

### 7.1 查看升级脚本输出

脚本最后会输出访问地址：

```text
========================================
升级完成
========================================
访问地址：
  管理后台： http://<服务器IP>/admin/
  用户端 H5： http://<服务器IP>/h5/user/
  教练端 H5： http://<服务器IP>/h5/coach/
  API 接口：  http://<服务器IP>/api/
```

### 7.2 验证后端健康检查

```bash
ssh root@<服务器IP>
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8000/health
```

预期返回：

```json
{"status":"UP"}
```

### 7.3 验证业务接口

```bash
curl -fsS http://<服务器IP>/api/actuator/health
curl -fsS http://<服务器IP>/api/package/list
```

### 7.4 验证前端页面

在浏览器访问：

- 管理后台：`http://<服务器IP>/admin/`
- 用户端 H5：`http://<服务器IP>/h5/user/`
- 教练端 H5：`http://<服务器IP>/h5/coach/`

---

## 8. 回滚演练

### 8.1 查找备份产物

每次升级都会生成带时间戳的备份目录/文件：

```bash
ssh root@<服务器IP>
ls -lt /opt/leyo-swimming/*.backup.* /opt/leyo-swimming/*-dist.backup.* 2>/dev/null | head -20
```

示例输出：

```text
drwxr-xr-x 5 root root 4096 Aug 29 14:32 /opt/leyo-swimming/web-admin-dist.backup.20260829143125
-rw-r--r-- 1 root root  45M Aug 29 14:31 /opt/leyo-swimming/leyo-swimming-backend.jar.backup.20260829143125
```

### 8.2 回滚后端 jar

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming

# 停止当前容器
cd deploy && docker compose -f docker-compose.prod.yml stop backend

# 回滚 jar
mv leyo-swimming-backend.jar leyo-swimming-backend.jar.failed
mv leyo-swimming-backend.jar.backup.20260829143125 leyo-swimming-backend.jar

# 重启
docker compose -f docker-compose.prod.yml up -d backend

# 验证
curl -fsS http://localhost:8080/actuator/health
```

### 8.3 回滚前端 dist

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming

mv web-admin-dist web-admin-dist.failed
mv web-admin-dist.backup.20260829143125 web-admin-dist

# 重载 Nginx
cd deploy
docker compose -f docker-compose.prod.yml exec -T nginx nginx -s reload
```

### 8.4 回滚 AI 服务

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming

mv ai-service ai-service.failed
mv ai-service-backup.20260829143125 ai-service

systemctl restart leyo-ai
curl -fsS http://localhost:8000/health
```

---

## 9. 常见问题与排查

### 9.1 hook 没有触发

检查 `.git/hooks/post-commit` 是否存在且可执行：

```bash
ls -la .git/hooks/post-commit
cat .git/hooks/post-commit
```

如果 hook 不存在，重新运行 `install-upgrade-hook.ps1`。

### 9.2 升级脚本提示 "未检测到代码变更"

可能是 `.last-deployed-commit` 文件中的 commit hash 无效或指向未来。脚本会自动回退到 `HEAD~1`，但如果希望重新全量对比，可删除该文件：

```powershell
Remove-Item .last-deployed-commit
```

### 9.3 健康检查失败

登录服务器查看日志：

```bash
ssh root@<服务器IP>
cd /opt/leyo-swimming/deploy

# 后端日志
docker compose -f docker-compose.prod.yml logs --tail=100 backend

# AI 服务日志
journalctl -u leyo-ai -n 100
```

### 9.4 产物切换后服务 500

可能是 jar 构建失败或前端构建产物不完整。检查：

```bash
# 后端 jar 是否可执行
java -jar /opt/leyo-swimming/leyo-swimming-backend.jar --version

# 前端 dist 是否存在 index.html
ls /opt/leyo-swimming/web-admin-dist/index.html
```

### 9.5 主机密钥指纹不匹配

如果服务器重装或更换了 SSH 密钥，需要重新获取指纹并更新 hook：

```powershell
ssh-keyscan -t ed25519 <服务器IP> 2>$null | ForEach-Object { if ($_ -match 'ssh-ed25519') { $_ } }
.\scripts\install-upgrade-hook.ps1 -HostKeyFingerprint 'SHA256:新指纹'
```

---

## 10. 安全检查清单

每次升级前确认：

- [ ] 已备份数据库
- [ ] 使用 `ssh-keyscan` 获取的主机密钥指纹正确
- [ ] 未使用 `-AcceptHostKey` 在生产环境自动接受未知密钥
- [ ] 自动部署模式仅设置了 `LEYO_UPGRADE_AUTO_DEPLOY=1` 在受信任环境
- [ ] 升级后已执行健康检查
- [ ] 旧备份产物未占用过多磁盘空间（脚本默认保留最近 3 个）
- [ ] 回滚路径已验证

---

## 附录：常用命令速查

```powershell
# 手动升级全部模块
.\scripts\upgrade-server.ps1 -ServerIP '<IP>' -HostKeyFingerprint 'SHA256:xxxxx'

# 手动升级指定模块
.\scripts\upgrade-server.ps1 -ServerIP '<IP>' -Modules backend,web-admin -HostKeyFingerprint 'SHA256:xxxxx'

# 强制覆盖远程产物（跳过是否存在的判断）
.\scripts\upgrade-server.ps1 -ServerIP '<IP>' -Modules backend -ForceUpload -HostKeyFingerprint 'SHA256:xxxxx'

# 只构建不上传
.\scripts\upgrade-server.ps1 -ServerIP '<IP>' -Modules backend -SkipUpload -HostKeyFingerprint 'SHA256:xxxxx'

# 仅运行变更检测
.\scripts\upgrade-hook.ps1

# 指定对比基准 commit
.\scripts\upgrade-hook.ps1 -SinceCommit 'a1b2c3d'

# 卸载 hook
Remove-Item .git/hooks/post-commit
```

---

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-29 | AI Agent | 初稿：增量升级脚本部署演练手册 |
