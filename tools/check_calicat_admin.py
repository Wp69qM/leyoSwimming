import subprocess
import json
import sys
import os
import time
import threading

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"

class MCPClient:
    def __init__(self):
        self.proc = subprocess.Popen(
            SERVER_CMD,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=False,
            shell=True,
        )
        self._lock = threading.Lock()
        self._req_id = 0
        self._pending = {}
        self._reader = threading.Thread(target=self._read_loop, daemon=True)
        self._reader.start()

    def _read_loop(self):
        while True:
            try:
                line = self.proc.stdout.readline()
                if not line:
                    break
                text = line.decode("utf-8", errors="replace").strip()
                if not text:
                    continue
                try:
                    msg = json.loads(text)
                except json.JSONDecodeError:
                    continue
                req_id = msg.get("id")
                if req_id is not None and req_id in self._pending:
                    self._pending[req_id].append(msg)
            except Exception:
                break

    def _next_id(self):
        with self._lock:
            self._req_id += 1
            return self._req_id

    def call(self, method, params):
        req_id = self._next_id()
        msg = {"jsonrpc": "2.0", "id": req_id, "method": method, "params": params}
        line = json.dumps(msg, ensure_ascii=False)
        self._pending[req_id] = []
        self.proc.stdin.write((line + "\n").encode("utf-8"))
        self.proc.stdin.flush()
        deadline = time.time() + 60
        while time.time() < deadline:
            if self._pending[req_id]:
                resp = self._pending[req_id].pop(0)
                if "error" in resp:
                    raise RuntimeError(f"MCP error: {resp['error']}")
                return resp.get("result")
            time.sleep(0.05)
        raise TimeoutError(f"Timeout waiting for response to {method}")

    def notify(self, method, params=None):
        msg = {"jsonrpc": "2.0", "method": method}
        if params is not None:
            msg["params"] = params
        line = json.dumps(msg, ensure_ascii=False)
        self.proc.stdin.write((line + "\n").encode("utf-8"))
        self.proc.stdin.flush()

    def close(self):
        try:
            self.proc.stdin.close()
        except Exception:
            pass
        self.proc.terminate()
        try:
            self.proc.wait(timeout=5)
        except Exception:
            self.proc.kill()


def main():
    expected_titles = [
        "跨批次全局设计原则-Web后台管理端",
        "Web后台管理端跨页面一致性自检报告",
        "页面规格-A-管理员登录页",
        "页面规格-A-数据看板页",
        "页面规格-A-用户管理页",
        "页面规格-A-用户详情页",
        "页面规格-A-教练入驻审核队列页",
        "页面规格-A-教练入驻审核详情页",
        "页面规格-A-教练管理页",
        "页面规格-A-教练详情页",
        "页面规格-A-离职审批队列页",
        "页面规格-A-离职工单详情页",
        "页面规格-A-排班管理页",
        "页面规格-A-请假审批页",
        "页面规格-A-预约释放配置页",
        "页面规格-A-套餐管理页",
        "页面规格-A-套餐详情页",
        "页面规格-A-订单管理页",
        "页面规格-A-订单详情页",
        "页面规格-A-退款审批页",
        "页面规格-A-退款审批详情页",
        "页面规格-A-场馆配置页",
        "页面规格-A-公告运营配置页",
        "页面规格-A-用户须知配置页",
        "页面规格-A-闭馆换水设置页",
        "页面规格-A-客服工单管理页",
        "页面规格-A-客服工单详情页",
    ]

    client = MCPClient()
    try:
        init = client.call("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "leyoSwimming-check-admin", "version": "1.0"}
        })
        client.notify("notifications/initialized")

        result = client.call("tools/call", {"name": "get_prd_list", "arguments": {"file_id": FILE_ID}})
        # JSON-RPC result wraps calicat response in content[].text
        text = result["content"][0]["text"]
        calicat_resp = json.loads(text)
        prd_list = calicat_resp.get("result", {}).get("prd_list", [])
        existing_titles = {p.get("prd_title", "").strip() for p in prd_list}

        print("==== Web 后台管理端文档 calicat 存在性检查 ====", file=sys.stderr)
        missing = []
        for t in expected_titles:
            status = "✅ 已存在" if t in existing_titles else "❌ 缺失"
            print(f"{status}: {t}", file=sys.stderr)
            if t not in existing_titles:
                missing.append(t)

        print(f"\n总计：{len(expected_titles)} 个，已存在 {len(expected_titles) - len(missing)} 个，缺失 {len(missing)} 个", file=sys.stderr)
        if missing:
            print("\n缺失清单：", file=sys.stderr)
            for t in missing:
                print(f"- {t}", file=sys.stderr)
    finally:
        client.close()


if __name__ == "__main__":
    main()
