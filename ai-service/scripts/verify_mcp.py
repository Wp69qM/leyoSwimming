r"""MCP 端到端验证脚本（US-068）。

用 MCP SDK client 走完整链路：initialize → tools/list → 4 个工具各真实调用一次，
输出结果摘要，作为 Trae 手动验证之外的自动化验证手段（双轨验证）。

前置条件：
- ai-service 已启动（默认 http://localhost:8000）
- 项目根目录 .env 已配置 MCP_API_TOKEN（>= 32 位强随机）
- query_knowledge 需要 EMBEDDING_API_KEY 与已同步的 Chroma 知识库数据
- 推荐工具需要 backend 运行中（:8080）

用法示例：
    venv\Scripts\python scripts\verify_mcp.py
    venv\Scripts\python scripts\verify_mcp.py --url http://localhost:8000/mcp-server/mcp
    venv\Scripts\python scripts\verify_mcp.py --token <MCP_API_TOKEN>
"""

import argparse
import asyncio
import json
import os
import sys
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

# 加载项目根目录的 .env（MCP_API_TOKEN 等配置）
load_dotenv(PROJECT_ROOT / ".env")

from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

DEFAULT_URL = "http://localhost:8000/mcp-server/mcp"
TOKEN_PLACEHOLDER = "your-mcp-api-token-here"

EXPECTED_TOOLS = ("query_knowledge", "query_coaches", "query_packages", "get_hot_recommendations")
# 安全红线：以下工具禁止出现在 MCP 工具清单（US-066/067）
FORBIDDEN_TOOLS = ("get_user_profile", "get_user_packages", "web_search", "ingest", "chat")

# 4 个工具各调用一次的固定用例（US-068 §6.1）
# query_knowledge 用例需命中知识库（阈值 0.2），确保验证记录含带来源片段
TOOL_CALL_CASES = [
    ("query_knowledge", {"query": "游泳时抽筋怎么办"}),
    ("query_coaches", {"stroke": "蛙泳"}),
    ("query_packages", {"stroke": "自由泳", "package_mode": "体验"}),
    ("get_hot_recommendations", {"limit": 5}),
]


def extract_payload(result) -> dict:
    """从 CallToolResult 提取工具返回对象（优先 structuredContent，回退解析文本块）。

    工具统一返回单 JSON 块：正常 {"list": [...]}，故障 {"error": "..."}。
    """
    structured = getattr(result, "structuredContent", None)
    if isinstance(structured, dict) and structured:
        return structured
    for block in result.content:
        text = getattr(block, "text", None)
        if text:
            return json.loads(text)
    raise ValueError("工具返回 content 为空")


def flatten_exception(exc: BaseException) -> BaseException:
    """解包 ExceptionGroup，取出最底层的真实异常（连接拒绝/401 等可读诊断）。"""
    while isinstance(exc, ExceptionGroup) and exc.exceptions:
        exc = exc.exceptions[0]
    return exc


def summarize_item(item: dict) -> str:
    """单条结果摘要：知识条目显示来源与分数，业务条目显示名称类字段。"""
    if "source" in item:
        return f"《{item.get('source', '')}》 score={item.get('score', 0):.3f}"
    for key in ("name", "coachName", "packageName", "title"):
        if key in item:
            return str(item[key])
    text = json.dumps(item, ensure_ascii=False)
    return text[:100] + ("..." if len(text) > 100 else "")


async def run(url: str, token: str) -> int:
    headers = {"Authorization": f"Bearer {token}"}
    failures: list[str] = []

    print(f"MCP endpoint：{url}")
    print("=" * 80)

    try:
        async with streamablehttp_client(url, headers=headers) as (read_stream, write_stream, _):
            async with ClientSession(read_stream, write_stream) as session:
                await session.initialize()
                print("[OK] initialize 握手成功")

                tools_result = await session.list_tools()
                tool_names = [tool.name for tool in tools_result.tools]
                print(f"[OK] tools/list 返回 {len(tool_names)} 个工具：{', '.join(tool_names)}")

                missing = [name for name in EXPECTED_TOOLS if name not in tool_names]
                leaked = [name for name in FORBIDDEN_TOOLS if name in tool_names]
                if missing:
                    failures.append(f"工具清单缺少预期工具：{missing}")
                    print(f"[FAIL] 工具清单缺少预期工具：{missing}")
                else:
                    print("[OK] 4 个预期工具全部在列")
                if leaked:
                    failures.append(f"安全红线违规，出现禁止暴露的工具：{leaked}")
                    print(f"[FAIL] 禁止暴露的工具出现在清单中：{leaked}")
                else:
                    print("[OK] 禁止暴露的工具均未出现（get_user_profile/get_user_packages/web_search/ingest/chat）")

                print("-" * 80)
                for name, arguments in TOOL_CALL_CASES:
                    arg_text = ", ".join(f"{k}={v!r}" for k, v in arguments.items())
                    try:
                        result = await session.call_tool(name, arguments)
                    except Exception as exc:
                        failures.append(f"{name} 调用异常：{exc}")
                        print(f"[FAIL] {name}({arg_text}) 调用异常：{exc}")
                        continue

                    if getattr(result, "isError", False):
                        failures.append(f"{name} 工具执行错误（isError=true）")
                        print(f"[FAIL] {name}({arg_text}) 工具执行错误")
                        continue

                    try:
                        payload = extract_payload(result)
                    except (ValueError, json.JSONDecodeError) as exc:
                        failures.append(f"{name} 返回内容无法解析：{exc}")
                        print(f"[FAIL] {name}({arg_text}) 返回内容无法解析：{exc}")
                        continue

                    if "error" in payload:
                        failures.append(f"{name} 返回结构化错误：{payload['error']}")
                        print(f"[FAIL] {name}({arg_text}) → {payload['error']}")
                    elif "list" in payload and isinstance(payload["list"], list):
                        items = payload["list"]
                        print(f"[OK] {name}({arg_text}) → 返回 {len(items)} 条")
                        for item in items[:3]:
                            print(f"     - {summarize_item(item)}")
                        if len(items) > 3:
                            print(f"     ... 共 {len(items)} 条")
                    else:
                        failures.append(f"{name} 返回结构异常（既无 list 也无 error）")
                        print(f"[FAIL] {name}({arg_text}) 返回结构异常：{payload}")

    except Exception as exc:
        root = flatten_exception(exc)
        exc_name = type(root).__name__
        failures.append(f"MCP 连接失败：{exc_name}: {root}")
        print(f"[FAIL] MCP 连接失败：{exc_name}: {root}")
        print("      排查提示：")
        print("      - 连接被拒 → ai-service 是否已启动（默认 :8000）")
        print("      - 401      → MCP_API_TOKEN 与 .trae/mcp.json / --token 是否一致")
        print("      - 限流 429 → 稍后重试（IP 维度限流）")

    print("=" * 80)
    if failures:
        print(f"验证失败（{len(failures)} 项）：")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("全部验证通过：initialize / tools/list / 4 工具真实调用 / 安全红线检查")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="MCP 端到端验证（initialize → tools/list → 4 工具调用）")
    parser.add_argument("--url", default=DEFAULT_URL, help=f"MCP endpoint，默认 {DEFAULT_URL}")
    parser.add_argument("--token", default=os.getenv("MCP_API_TOKEN", ""), help="MCP API Token（默认读取 .env 的 MCP_API_TOKEN）")
    args = parser.parse_args()

    token = args.token
    if not token:
        print("[FAIL] 未配置 MCP_API_TOKEN：请在 ai-service/.env 中设置，或通过 --token 传入")
        sys.exit(1)
    if token == TOKEN_PLACEHOLDER or len(token) < 32:
        print("[FAIL] MCP_API_TOKEN 无效：仍是占位符或长度不足 32 位，请生成强随机 token（openssl rand -hex 32）")
        sys.exit(1)

    sys.exit(asyncio.run(run(args.url, token)))


if __name__ == "__main__":
    main()
