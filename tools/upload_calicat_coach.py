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
            "clientInfo": {"name": "leyoSwimming-upload-agent-coach", "version": "1.0"}
        })
        print("[INFO] initialized:", json.dumps(init, ensure_ascii=False)[:300], file=sys.stderr)
        client.notify("notifications/initialized")

        files_to_upload = [
            ("跨批次全局设计原则-教练侧", "docs/figma/page-spec/CROSS-BATCH-PRINCIPLES-COACH.md"),
            ("教练端跨页面一致性自检报告", "docs/figma/page-spec/cross-page-check-coach.md"),
            ("页面规格-C-入驻资料填写页", "docs/figma/page-spec/C-application-page.md"),
            ("页面规格-C-入驻提交成功页", "docs/figma/page-spec/C-application-success-page.md"),
            ("页面规格-C-重新入驻页", "docs/figma/page-spec/C-reapply-page.md"),
            ("页面规格-C-教练端首页", "docs/figma/page-spec/C-coach-home-page.md"),
            ("页面规格-C-预约管理页", "docs/figma/page-spec/C-booking-management-page.md"),
            ("页面规格-C-上课记录确认页", "docs/figma/page-spec/C-class-confirm-page.md"),
            ("页面规格-C-改约页", "docs/figma/page-spec/C-reschedule-page.md"),
            ("页面规格-C-代约时段选择页", "docs/figma/page-spec/C-substitute-booking-page.md"),
            ("页面规格-C-排班管理页", "docs/figma/page-spec/C-schedule-management-page.md"),
            ("页面规格-C-请假申请页", "docs/figma/page-spec/C-leave-request-page.md"),
            ("页面规格-C-请假记录页", "docs/figma/page-spec/C-leave-list-page.md"),
            ("页面规格-C-我的学员列表页", "docs/figma/page-spec/C-student-list-page.md"),
            ("页面规格-C-学员详情编辑页", "docs/figma/page-spec/C-student-detail-page.md"),
            ("页面规格-C-个人主页编辑页", "docs/figma/page-spec/C-profile-edit-page.md"),
            ("页面规格-C-教练主页分享页", "docs/figma/page-spec/C-share-coach-page.md"),
            ("页面规格-C-游客分享落地页", "docs/figma/page-spec/C-share-landing-page.md"),
            ("页面规格-C-教练中心页", "docs/figma/page-spec/C-coach-center-page.md"),
            ("页面规格-C-参考单价设置页", "docs/figma/page-spec/C-reference-price-page.md"),
            ("页面规格-C-教练收入页", "docs/figma/page-spec/C-coach-income-page.md"),
            ("页面规格-C-离职申请页", "docs/figma/page-spec/C-resignation-page.md"),
            ("页面规格-C-离职工单处理页", "docs/figma/page-spec/C-resignation-ticket-page.md"),
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
