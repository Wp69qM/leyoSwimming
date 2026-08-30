import subprocess, json, sys, os, time, threading

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)))

FRAMES = [
    ("C-coach-resigning-page-v3", "0cfd2025-dd2b-4230-b5b6-390b641cc230"),
    ("C-coach-resigning-page-v4", "76192f9d-3a45-4505-8859-cdc4a1eb404a"),
]

class MCPClient:
    def __init__(self):
        self.proc = subprocess.Popen(SERVER_CMD, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=False, shell=True)
        self._lock = threading.Lock()
        self._req_id = 0
        self._pending = {}
        self._reader = threading.Thread(target=self._read_loop, daemon=True)
        self._reader.start()
    def _read_loop(self):
        while True:
            line = self.proc.stdout.readline()
            if not line: break
            text = line.decode("utf-8", errors="replace").strip()
            if not text: continue
            try: msg = json.loads(text)
            except: continue
            rid = msg.get("id")
            if rid is not None and rid in self._pending:
                self._pending[rid].append(msg)
    def _next_id(self):
        with self._lock:
            self._req_id += 1
            return self._req_id
    def call(self, method, params):
        req_id = self._next_id()
        msg = {"jsonrpc":"2.0","id":req_id,"method":method,"params":params}
        self._pending[req_id] = []
        self.proc.stdin.write((json.dumps(msg, ensure_ascii=False)+"\n").encode("utf-8"))
        self.proc.stdin.flush()
        deadline = time.time()+180
        while time.time()<deadline:
            if self._pending[req_id]:
                resp = self._pending[req_id].pop(0)
                if "error" in resp: raise RuntimeError(resp["error"])
                return resp.get("result")
            time.sleep(0.05)
        raise TimeoutError(method)
    def notify(self, method, params=None):
        msg = {"jsonrpc":"2.0","method":method}
        if params is not None: msg["params"] = params
        self.proc.stdin.write((json.dumps(msg, ensure_ascii=False)+"\n").encode("utf-8"))
        self.proc.stdin.flush()
    def close(self):
        try: self.proc.stdin.close()
        except: pass
        self.proc.terminate()
        try: self.proc.wait(timeout=5)
        except: self.proc.kill()

def save_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

c = MCPClient()
try:
    c.call("initialize", {"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"leyo-coach-resigning","version":"1.0"}})
    c.notify("notifications/initialized")
    for name, lid in FRAMES:
        print(f"[INFO] fetching {name} {lid}", file=sys.stderr)
        try:
            design = c.call("tools/call", {"name":"get_design_data","arguments":{"file_id":FILE_ID,"selected_layer_id":lid}})
            path = os.path.join(OUTPUT_DIR, f"{name}_design.json")
            save_json(path, design)
            print(f"[OK] {path}", file=sys.stderr)
        except Exception as e:
            print(f"[FAIL] {name}: {e}", file=sys.stderr)
finally:
    c.close()
