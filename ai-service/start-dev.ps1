# 本地开发启动脚本
.\.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload --reload-dir app
