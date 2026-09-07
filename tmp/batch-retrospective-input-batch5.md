# batch5 批次复盘原始数据

> 本文件由 scripts/update-batch-lessons.ps1 自动生成，供 atch-retrospective Agent 读取。
> 生成时间：2026-09-07 16:25:46

## 1. 时间范围

| 项目 | 值 |
|------|-----|
| 批次 | batch5 |
| 开始日期 | 2026-09-07 |
| 结束日期 | 2026-09-07 |
| Commit 数量 |  |

## 2. Commit 列表

| Hash | 日期 | 作者 | 消息 |
|------|------|------|------|
| 6f26f25 | 2026-09-07 12:57:53 | Wp69dM | chore(ai-service): add mcp sdk and config |

## 3. 变更文件统计

### 3.1 前端源码文件

无前端源码变更。

### 3.2 page-spec 文件

无 page-spec 变更。

## 4. Visual Review 报告

未找到 visual-review 报告。请确认是否已运行 visual-reviewer Agent 并输出到 	mp/visual-reviews/。

## 5. 现有经验手册内容

未找到 batch5 经验手册（D:\AI Agent\leyoSwimming\docs\figma\batch5-lessons-learned.md）。

## 6. 请 Agent 完成的复盘任务

请 \atch-retrospective Agent 基于以上数据：

1. 从 Commit 消息、变更文件、visual-review 报告中识别新的视觉还原问题、规范理解问题、流程执行问题、跨端一致性问题。
2. 将新增问题按 §2 的格式追加到 ${Batch}-lessons-learned.md 的「典型问题」章节。
3. 如有新的根因或预防措施，更新 §3 / §4 / §5。
4. 在变更日志中新增一行。
5. 输出「本次复盘新增/修改的内容摘要」到当前会话。

---

## 7. 批次实际工作内容（人工补充，Agent 必读）

**批次性质**：纯后端批次（无前端页面、无数据表变更、无 backend 代码改动），交付物为 ai-service 的 MCP 能力暴露（FastMCP + Streamable HTTP + 4 个只读工具）+ Trae 注册验证。因此 §4 的 visual-review 检查项按 N/A 处理，经验以**后端开发/测试/集成**类为主。

**交付概况**：
- 新增 `app/mcp/`（server.py + auth.py ASGI 鉴权中间件）、`app/tools/normalizers.py`（LangChain/MCP 双链路共享归一化）、`scripts/verify_mcp.py`（端到端验证脚本）
- 测试：tests/mcp/ 4 个测试文件 + test_normalizers.py，最终 134 passed、覆盖率 87%（mcp/auth 90%、mcp/server 84%、normalizers 100%）
- code-reviewer 审查：1 HIGH + 3 MEDIUM + 3 LOW，全部 HIGH/MEDIUM 已修复并补回归测试
- 端到端：verify_mcp.py 自动化全通过 + Trae 手动验证 4/4 工具（自然语言触发真实调用），开发者确认闭环

**本批次踩坑与技术经验（Agent 按此提炼）**：

1. **pytest-cov 时序污染（最高价值教训）**：pytest-cov 启动时 coverage 提前导入 app 包 → 触发 config.py 模块级 `load_dotenv()` → 本地 .env 真实 token/限流值写入进程环境 → 早于 conftest 执行 → `os.environ.setdefault()` 保留真实值 → 带 `--cov` 时测试 401/429 失败，不带 cov 全过（诡异的"cov 依赖"假象）。修复：conftest 中所有测试环境变量**强制赋值**而非 setdefault，并写明根因注释。预防：新项目 conftest 一律强制赋值；"只在某运行模式下失败"优先排查进程环境被提前污染。

2. **pydantic root_model KeyError**：pytest-cov 的 trace 干扰 pydantic 延迟加载，`KeyError: 'pydantic.root_model'`。修复：conftest 顶部 `import pydantic.root_model` 预导入。预防：coverage 报导入期 KeyError 时先怀疑延迟加载模块，预导入规避。

3. **FastMCP session_manager.run() 单次限制**：StreamableHTTPSessionManager 每实例只能 run() 一次，测试需多次进入/退出时须在进入前+退出后重置 `_has_started` 标志（stateless 模式下 SDK 已清理内部状态，重置安全）；生产 lifespan 全程一次不受影响。

4. **FastMCP DNS rebinding 保护 421**：host 为 127.0.0.1 时默认 allowed_hosts 仅 localhost，自定义 Host（http://test）被 421 拒绝。已有独立 Bearer token + IP 限流前提下显式禁用（TransportSecuritySettings(enable_dns_rebinding_protection=False)），注释写明安全论证。

5. **FastMCP list 返回值展开**：FastMCP 会把 list 返回值展开为多个 content 块，工具必须返回 `{"list": [...]}` 包装结构保证单 JSON 块响应（与项目 API 约定 data.list 天然对齐）。

6. **hmac.compare_digest 非 ASCII TypeError（code-review 发现）**：str 重载遇非 ASCII 抛 TypeError → 恶意请求得 500 而非 401 + 错误日志噪音。修复：`token.encode("utf-8")` bytes 比较（两处中间件同模式修复 + 2 个回归测试）。预防：安全敏感比较一律 bytes 化。

7. **MCP SDK ExceptionGroup 解包**：连接错误包装在 ExceptionGroup 中，直接 str(exc) 不可读。verify_mcp.py 增加 flatten_exception 解包出根因（ConnectError/401），并输出排查提示（服务未启动/token 不一致/限流）。

8. **端口占用与旧进程陷阱（端到端验证阶段）**：本机 Trae 的 node utility 进程占用 8000 端口（/health 返回 200 造成"服务在运行"假象，实际 POST /mcp-server/mcp 返回 404 暴露真相）→ ai-service 改用 8001。另：验证脚本失败先确认目标进程是否旧代码启动（无 --reload 需手动重启）。诊断顺序：进程命令行 → 决定性端点探测（404 vs 401）→ 再怀疑代码。

9. **配置默认值多源同步**：`knowledge_similarity_threshold` 代码默认 0.7→0.2 修正时，deploy/.env.example 残留 0.7（code-review MEDIUM 发现），生产按模板部署会阻断召回。预防：调优型配置改默认值时全量 grep 所有配置源（代码/config.example/deploy 模板/compose fallback）。

10. **流程经验**：
    - TDD 的 COMMIT 步骤执行不严格：批次只有 1 个 commit（Task 1），Task 2-4 代码完成后未及时提交，导致复盘脚本收集不到 commit 数据（本输入文件 §2 仅 1 行）。预防：每个 Task GREEN 后立即 commit。
    - MCP 协议端点（JSON-RPC over Streamable HTTP）不适用项目 REST 三段式 API 约定，豁免依据已在 US-066 §7.2/§12 显式记录——新协议接入时豁免必须留痕。
    - `.trae/mcp.json` 含真实 token：.gitignore 防线 + example 模板入库（review HIGH 修复），文档中的 `git update-index --skip-worktree` 对未跟踪文件无效，勿再推荐。
    - 纯后端批次的复盘检查清单：visual-review 项 N/A，但 code-reviewer + 覆盖率 + E2E 验证记录归档仍为强制项。

**建议沉淀到经验手册的预防性检查项**（Agent 参考）：
- [ ] conftest 测试环境变量用强制赋值，禁止 setdefault（pytest-cov + load_dotenv 时序）
- [ ] 安全 token 比较必须 encode 后 bytes 比较（compare_digest）
- [ ] 配置默认值调整后全量同步所有配置源（代码/模板/compose）
- [ ] 端到端验证前确认服务进程为新代码（无 --reload 须重启），端口冲突时用决定性探测区分服务
- [ ] 每个 TDD Task GREEN 后立即 commit，保证复盘数据完整

