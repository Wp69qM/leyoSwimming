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
        self._buffer = ""
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
                print(f"[RECV] {text[:300]}", file=sys.stderr)
                try:
                    msg = json.loads(text)
                except json.JSONDecodeError:
                    print(f"[WARN] non-json: {text[:200]}", file=sys.stderr)
                    continue
                req_id = msg.get("id")
                if req_id is not None and req_id in self._pending:
                    self._pending[req_id].append(msg)
                else:
                    pass
            except Exception as e:
                print(f"[READ ERR] {e}", file=sys.stderr)
                break

    def _next_id(self):
        with self._lock:
            self._req_id += 1
            return self._req_id

    def call(self, method, params):
        req_id = self._next_id()
        msg = {"jsonrpc": "2.0", "id": req_id, "method": method, "params": params}
        line = json.dumps(msg, ensure_ascii=False)
        print(f"[SEND] {line[:250]}", file=sys.stderr)
        self._pending[req_id] = []
        self.proc.stdin.write((line + "\n").encode("utf-8"))
        self.proc.stdin.flush()
        deadline = time.time() + 120
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
        print(f"[SEND-NOTIFY] {line[:200]}", file=sys.stderr)
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


def parse_calicat_response(result):
    """Parse JSON-RPC result -> calicat response dict."""
    try:
        text = result["content"][0]["text"]
        return json.loads(text)
    except Exception as e:
        return {"status": "error", "error_message": f"parse error: {e}"}


def upload_file(client, title, path):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    print(f"[INFO] Uploading {path} ({len(content)} chars) as '{title}'", file=sys.stderr)
    try:
        raw = client.call("tools/call", {
            "name": "create_document",
            "arguments": {
                "file_id": FILE_ID,
                "document_title": title,
                "document_content": content,
            }
        })
        result = parse_calicat_response(raw)
        print(f"[RAW] {title}: {json.dumps(result, ensure_ascii=False)[:500]}", file=sys.stderr)
        return result
    except Exception as e:
        print(f"[FAIL] {title}: {e}", file=sys.stderr)
        return {"status": "error", "error_message": str(e)}


def main():
    client = MCPClient()
    try:
        init = client.call("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "leyoSwimming-upload-agent-admin", "version": "1.0"}
        })
        print("[INFO] initialized:", json.dumps(init, ensure_ascii=False)[:300], file=sys.stderr)
        client.notify("notifications/initialized")

        files_to_upload = [
            ("跨批次全局设计原则-Web后台管理端", "docs/figma/page-spec/A-CROSS-BATCH-PRINCIPLES.md"),
            ("Web后台管理端跨页面一致性自检报告", "docs/figma/page-spec/cross-page-check-admin.md"),
            ("页面规格-A-管理员登录页", "docs/figma/page-spec/A-admin-login-page.md"),
            ("页面规格-A-数据看板页", "docs/figma/page-spec/A-dashboard-page.md"),
            ("页面规格-A-用户管理页", "docs/figma/page-spec/A-user-management-page.md"),
            ("页面规格-A-用户详情页", "docs/figma/page-spec/A-user-detail-page.md"),
            ("页面规格-A-教练入驻审核队列页", "docs/figma/page-spec/A-coach-audit-queue-page.md"),
            ("页面规格-A-教练入驻审核详情页", "docs/figma/page-spec/A-coach-audit-detail-page.md"),
            ("页面规格-A-教练管理页", "docs/figma/page-spec/A-coach-management-page.md"),
            ("页面规格-A-教练详情页", "docs/figma/page-spec/A-coach-detail-page.md"),
            ("页面规格-A-离职审批队列页", "docs/figma/page-spec/A-resignation-approval-queue-page.md"),
            ("页面规格-A-离职工单详情页", "docs/figma/page-spec/A-resignation-ticket-detail-page.md"),
            ("页面规格-A-排班管理页", "docs/figma/page-spec/A-schedule-management-page.md"),
            ("页面规格-A-请假审批页", "docs/figma/page-spec/A-leave-approval-page.md"),
            ("页面规格-A-预约释放配置页", "docs/figma/page-spec/A-release-config-page.md"),
            ("页面规格-A-套餐管理页", "docs/figma/page-spec/A-package-management-page.md"),
            ("页面规格-A-套餐详情页", "docs/figma/page-spec/A-package-detail-page.md"),
            ("页面规格-A-订单管理页", "docs/figma/page-spec/A-order-management-page.md"),
            ("页面规格-A-订单详情页", "docs/figma/page-spec/A-order-detail-page.md"),
            ("页面规格-A-退款审批页", "docs/figma/page-spec/A-refund-approval-page.md"),
            ("页面规格-A-退款审批详情页", "docs/figma/page-spec/A-refund-approval-detail-page.md"),
            ("页面规格-A-场馆配置页", "docs/figma/page-spec/A-venue-config-page.md"),
            ("页面规格-A-公告运营配置页", "docs/figma/page-spec/A-announcement-config-page.md"),
            ("页面规格-A-用户须知配置页", "docs/figma/page-spec/A-user-agreement-config-page.md"),
            ("页面规格-A-闭馆换水设置页", "docs/figma/page-spec/A-closure-config-page.md"),
            ("页面规格-A-客服工单管理页", "docs/figma/page-spec/A-ticket-management-page.md"),
            ("页面规格-A-客服工单详情页", "docs/figma/page-spec/A-ticket-detail-page.md"),
        ]

        results = []
        for title, rel_path in files_to_upload:
            full = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), rel_path)
            results.append({"title": title, "result": upload_file(client, title, full)})

        print("\n==== UPLOAD SUMMARY ====", file=sys.stderr)
        ok_count = 0
        fail_count = 0
        for r in results:
            res = r["result"]
            is_ok = isinstance(res, dict) and res.get("status") == "success"
            if is_ok:
                status = "OK"
                ok_count += 1
            else:
                status = "FAIL"
                fail_count += 1
            err = res.get("error_message", "") if isinstance(res, dict) else ""
            print(f"[{status}] {r['title']}" + (f" ({err})" if err and status == "FAIL" else ""), file=sys.stderr)
        print(f"\nTotal: {ok_count} OK, {fail_count} FAIL", file=sys.stderr)
    finally:
        client.close()


if __name__ == "__main__":
    main()
