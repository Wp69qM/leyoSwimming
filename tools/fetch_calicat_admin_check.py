import subprocess
import json
import sys
import os
import time
import threading

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tmp", "calicat_admin_check")

FRAMES = [
    ("A-admin-login-page", "fcc37e45-5558-46f7-9f60-fe42c4a1f74d"),
    ("A-admin-login-page-loading", "81333567-2808-4906-bef8-eb018f8ca544"),
    ("A-admin-login-page-error", "124cba20-d435-4820-9104-15f06656368e"),
    ("A-admin-login-page-disabled", "068b1c9a-39c0-4078-8d40-7003373f48ab"),
    ("A-admin-login-page-token-expired", "7cd276ee-0af1-4418-bfbe-f9684e46764e"),
    ("A-dashboard-page", "d05c8a00-484a-46e0-96e7-648c9fde6665"),
    ("A-user-management-page", "4f6b40d4-99f4-4b7e-b263-60f9bad4ed26"),
    ("A-user-create-modal-adult-no-base", "e4a6ea65-882e-4afd-b099-bc36c73460f0"),
    ("A-user-create-modal-adult-base", "a049153c-b10b-4904-9fb8-38559baf0763"),
    ("A-user-create-modal-minor-base", "e3149f94-1320-434d-a739-0ae47434216e"),
    ("A-user-edit-modal-adult-base", "dc6cdfd2-d814-4c0d-ad48-6ad94ef9f76a"),
    ("A-user-edit-modal-minor-base", "10cacee2-7b95-49ac-ab80-dd8a287aa4eb"),
    ("A-user-view-modal", "0151e8d5-daf9-4ac7-ac9a-227d7459ceb8"),
    ("A-admin-management-page", "b7a6b26c-3f67-4284-bae1-7d6b86fc09ae"),
    ("A-admin-create-modal", "7d3638c9-abda-43b5-b62f-198d8590b204"),
    ("A-admin-edit-modal", "6846d595-8d81-4bac-9682-9c0141c2ee07"),
    ("A-admin-view-modal", "3cfb59d4-128f-4de0-be32-9fe9b640d46a"),
    ("A-admin-reset-password-modal", "81d6e68e-e7ef-4d42-9f35-7482e577a111"),
    ("A-coach-management-page", "00c8fc73-3bec-4d62-99f1-d83a32f124fb"),
    ("A-coach-edit-modal", "56a15eaf-8892-4bfa-bed2-35909b6ffac7"),
    ("A-coach-detail-page-basic", "6475c58b-576b-43c5-b2f3-a54449430ccb"),
    ("A-coach-detail-page-students", "419ab471-acd6-423b-9f25-f45cbf334152"),
    ("A-coach-detail-page-schedule", "a8a1decf-29a0-4694-b8c7-e3929f290ba2"),
    ("A-coach-audit-queue-page", "d613f64c-14c9-4202-8b02-b3a81649fbcb"),
    ("A-coach-audit-detail-page", "e8093623-f0db-41f6-b99b-d531b42c5005"),
    ("A-resignation-approval-queue-page", "3a9151e0-e4b4-4246-8806-e79f225e57c2"),
    ("A-resignation-ticket-detail-page", "e04598b5-4992-45b7-ad84-255a18884bab"),
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
            "clientInfo": {"name": "leyoSwimming-admin-check", "version": "1.0"}
        })
        print("[INFO] initialized:", json.dumps(init, ensure_ascii=False)[:300], file=sys.stderr)
        client.notify("notifications/initialized")

        for name, layer_id in FRAMES:
            fetch_page(client, name, layer_id)

        print("\n[INFO] Done. Output dir:", OUTPUT_DIR, file=sys.stderr)
    finally:
        client.close()


if __name__ == "__main__":
    main()
