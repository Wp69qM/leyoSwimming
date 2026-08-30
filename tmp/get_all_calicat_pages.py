import subprocess, json, sys, os, time, threading

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"

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
        deadline = time.time()+60
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

c = MCPClient()
try:
    c.call("initialize", {"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"all-pages","version":"1.0"}})
    c.notify("notifications/initialized", {})
    def parse_result(raw):
        if isinstance(raw, dict) and "content" in raw:
            text = raw["content"][0]["text"]
            return json.loads(text)["result"]
        return raw

    canvases = parse_result(c.call("tools/call", {"name":"get_canvas_list","arguments":{"file_id":FILE_ID}}))
    all_pages = []
    for canvas in canvases["canvas_list"]:
        pages = parse_result(c.call("tools/call", {"name":"get_design_page_list","arguments":{"file_id":FILE_ID, "canvas_id": canvas["canvas_id"]}}))
        all_pages.append({
            "canvas_id": canvas["canvas_id"],
            "canvas_name": canvas["name"],
            "pages": pages["page_list"]
        })
    with open("tmp/calicat_all_pages.json", "w", encoding="utf-8") as f:
        json.dump(all_pages, f, ensure_ascii=False, indent=2)
    print("Saved to tmp/calicat_all_pages.json")
finally:
    c.close()
