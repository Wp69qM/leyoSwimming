import subprocess, json, sys, os, time, threading, re

SERVER_CMD = "npx -y mcp-remote@latest https://www.calicat.cn/mcp"
FILE_ID = "2083742072257646592"

class MCPClient:
    def __init__(self):
        self.proc = subprocess.Popen(SERVER_CMD, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=False, shell=True)
        self._lock = threading.Lock()
        self._req_id = 0
        self._pending = {}
        threading.Thread(target=self._read_loop, daemon=True).start()
    def _read_loop(self):
        for line in self.proc.stdout:
            text = line.decode("utf-8", errors="replace").strip()
            if not text: continue
            try:
                msg = json.loads(text)
            except Exception:
                continue
            rid = msg.get("id")
            if rid is not None and rid in self._pending:
                self._pending[rid].append(msg)
    def call(self, method, params):
        with self._lock:
            self._req_id += 1
            req_id = self._req_id
        msg = {"jsonrpc":"2.0","id":req_id,"method":method,"params":params}
        self._pending[req_id] = []
        self.proc.stdin.write((json.dumps(msg, ensure_ascii=False)+"\n").encode("utf-8"))
        self.proc.stdin.flush()
        deadline = time.time()+60
        while time.time()<deadline:
            if self._pending[req_id]:
                resp = self._pending[req_id].pop(0)
                return resp.get("result")
            time.sleep(0.05)
        raise TimeoutError(method)
    def close(self):
        try: self.proc.stdin.close()
        except: pass
        self.proc.terminate()
        try: self.proc.wait(timeout=5)
        except: self.proc.kill()

def main():
    c = MCPClient()
    try:
        c.call("initialize", {"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"list","version":"1.0"}})
        c.proc.stdin.write((json.dumps({"jsonrpc":"2.0","method":"notifications/initialized"}, ensure_ascii=False)+"\n").encode("utf-8"))
        c.proc.stdin.flush()
        res = c.call("tools/call", {"name":"get_prd_list","arguments":{"file_id":FILE_ID}})
        text = res["content"][0]["text"]
        data = json.loads(text)
        prds = data["result"]["prd_list"]
        out = [{"prd_id":p["prd_id"], "prd_title":p.get("prd_title","")} for p in prds]
        print(json.dumps(out, ensure_ascii=False, indent=2))
    finally:
        c.close()

if __name__=="__main__":
    main()
