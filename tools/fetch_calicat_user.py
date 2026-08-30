import subprocess
import json
import sys
import os
import time
import threading

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tmp", "calicat_user")

PAGES = [
    ("U-splash-page", "adcfa91b-2124-4b73-9b4d-a845ac17b2b8"),
    ("U-wechat-auth-page", "800149e3-6552-4d07-9e17-57f187d123e9"),
    ("U-login-protocol-modal-privacy", "24f5ecc9-b5cb-4f5b-a2cb-4f726482c0f1"),
    ("U-login-protocol-modal-terms", "55c3468a-a193-4aba-bbda-567b8c3a3b0e"),
    ("U-phone-login-page", "d82a1f63-214d-444a-818b-5df4ffa5f00a"),
    ("U-phone-complete-page-adult-empty", "7d740853-a39f-4ec2-be61-81dad8b39ac3"),
    ("U-phone-complete-page-related-1", "8774704a-e7b2-4bc1-bcc4-dfd606c40005"),
    ("U-phone-complete-page-related-2", "9ca89c86-6538-48ec-9a92-ff038b357221"),
    ("U-phone-complete-page-related-3", "2afc16a7-e91a-4d44-a342-a087cf5aec3c"),
    ("U-phone-complete-page-related-4", "a070760a-d6f2-43e6-8220-626625c28c9f"),
    ("U-phone-complete-page-related-5", "d720cfeb-cc05-4511-b2ca-c87af2074759"),
    ("U-account-cancel-page-satisfied", "ff8c90a8-721e-45c4-858f-6326a631cdbe"),
    ("U-account-cancel-page-related-1", "17a36d48-f1cc-4bc5-8dd0-0cb7d5f697d1"),
    ("U-account-cancel-page-related-2", "0e8d7f43-0a7e-48fc-ae79-d898f8682b88"),
    ("U-account-cancel-page-related-3", "c8a36814-59b3-4130-a8c1-b9343f152530"),
]


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
        deadline = time.time() + 180
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


def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def fetch_page(client, name, layer_id):
    print(f"\n[INFO] Fetching {name} ({layer_id})", file=sys.stderr)
    try:
        design = client.call("tools/call", {
            "name": "get_design_data",
            "arguments": {"file_id": FILE_ID, "selected_layer_id": layer_id}
        })
        design_path = os.path.join(OUTPUT_DIR, f"{name}_design.json")
        save_json(design_path, design)
        print(f"[OK] design -> {design_path}", file=sys.stderr)
    except Exception as e:
        print(f"[FAIL] design {name}: {e}", file=sys.stderr)
        design = None
    return design


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    client = MCPClient()
    try:
        init = client.call("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "leyoSwimming-check-agent-user", "version": "1.0"}
        })
        print("[INFO] initialized:", json.dumps(init, ensure_ascii=False)[:300], file=sys.stderr)
        client.notify("notifications/initialized")

        for name, layer_id in PAGES:
            fetch_page(client, name, layer_id)

        print("\n[INFO] Done. Output dir:", OUTPUT_DIR, file=sys.stderr)
    finally:
        client.close()


if __name__ == "__main__":
    main()
