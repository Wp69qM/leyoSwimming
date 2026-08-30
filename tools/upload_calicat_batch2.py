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


def upload_file(client, title, path):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    print(f"[INFO] Uploading {path} ({len(content)} chars) as '{title}'", file=sys.stderr)
    try:
        result = client.call("tools/call", {
            "name": "create_document",
            "arguments": {
                "file_id": FILE_ID,
                "document_title": title,
                "document_content": content,
            }
        })
        print(f"[OK] {title}: {json.dumps(result, ensure_ascii=False)[:500]}", file=sys.stderr)
        return result
    except Exception as e:
        print(f"[FAIL] {title}: {e}", file=sys.stderr)
        return {"error": str(e)}


def main():
    client = MCPClient()
    try:
        init = client.call("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "leyoSwimming-upload-agent-batch2", "version": "1.0"}
        })
        print("[INFO] initialized:", json.dumps(init, ensure_ascii=False)[:300], file=sys.stderr)
        client.notify("notifications/initialized")

        files_to_upload = [
            ("跨批次全局设计原则-用户侧", "docs/figma/page-spec/CROSS-BATCH-PRINCIPLES.md"),
            ("跨页面一致性自检报告", "docs/figma/page-spec/cross-page-check.md"),
            ("页面规格-U-微信授权页", "docs/figma/page-spec/U-wechat-auth-page.md"),
            ("页面规格-U-补充手机号页", "docs/figma/page-spec/U-phone-complete-page.md"),
            ("页面规格-U-套餐列表页", "docs/figma/page-spec/U-package-list-page.md"),
            ("页面规格-U-套餐详情页", "docs/figma/page-spec/U-package-detail-page.md"),
            ("页面规格-U-订单确认页", "docs/figma/page-spec/U-order-confirm-page.md"),
            ("页面规格-U-支付页", "docs/figma/page-spec/U-payment-page.md"),
            ("页面规格-U-预约页", "docs/figma/page-spec/U-booking-page.md"),
            ("页面规格-U-预约成功页", "docs/figma/page-spec/U-booking-success-page.md"),
            ("页面规格-U-候补关注页", "docs/figma/page-spec/U-waitlist-page.md"),
            ("页面规格-U-订单列表页", "docs/figma/page-spec/U-order-list-page.md"),
            ("页面规格-U-订单详情页", "docs/figma/page-spec/U-order-detail-page.md"),
            ("页面规格-U-我的预约页", "docs/figma/page-spec/U-my-appointments-page.md"),
            ("页面规格-U-预约详情页", "docs/figma/page-spec/U-appointment-detail-page.md"),
            ("页面规格-U-上课记录页", "docs/figma/page-spec/U-class-history-page.md"),
        ]

        results = []
        for title, rel_path in files_to_upload:
            full = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), rel_path)
            results.append({"title": title, "result": upload_file(client, title, full)})

        print("\n==== UPLOAD SUMMARY ====", file=sys.stderr)
        for r in results:
            status = "OK" if "error" not in r["result"] else "FAIL"
            print(f"[{status}] {r['title']}", file=sys.stderr)

        prds2 = client.call("tools/call", {"name": "get_prd_list", "arguments": {"file_id": FILE_ID}})
        print("\n==== FINAL PRD LIST ====", file=sys.stderr)
        print(json.dumps(prds2, ensure_ascii=False, indent=2)[:4000], file=sys.stderr)
    finally:
        client.close()


if __name__ == "__main__":
    main()
