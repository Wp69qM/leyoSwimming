#!/bin/bash
# leyoSwimming 后端重启问题一键排查脚本
# 用法：ssh root@<服务器IP> 'bash -s' < scripts/diagnose-leyo-backend.sh
# 或上传到服务器后执行：chmod +x diagnose-leyo-backend.sh && ./diagnose-leyo-backend.sh

set -e

DEPLOY_DIR="/opt/leyo-swimming/deploy"
ENV_FILE="${DEPLOY_DIR}/.env"
LOG_LINES=200

echo "========================================"
echo "leyoSwimming 后端问题排查报告"
echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

echo ""
echo "【1/7】容器运行状态"
echo "----------------------------------------"
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.State}}\t{{.Ports}}" | grep leyo || true

echo ""
echo "【2/7】容器健康状态"
echo "----------------------------------------"
for container in leyo-backend leyo-mysql leyo-redis leyo-nginx; do
    if docker inspect "$container" >/dev/null 2>&1; then
        status=$(docker inspect --format="{{.State.Status}}" "$container")
        health=$(docker inspect --format="{{.State.Health.Status}}" "$container" 2>/dev/null || echo "无健康检查")
        restarts=$(docker inspect --format="{{.RestartCount}}" "$container" 2>/dev/null || echo "未知")
        echo "$container: status=$status, health=$health, restarts=$restarts"
    else
        echo "$container: 不存在"
    fi
done

echo ""
echo "【3/7】leyo-backend 最近 ${LOG_LINES} 行日志"
echo "----------------------------------------"
if docker inspect leyo-backend >/dev/null 2>&1; then
    docker logs --tail="${LOG_LINES}" leyo-backend 2>&1 || true
else
    echo "leyo-backend 容器不存在"
fi

echo ""
echo "【4/7】.env 关键配置检查"
echo "----------------------------------------"
if [ -f "$ENV_FILE" ]; then
    echo "文件存在: $ENV_FILE"
    echo ""
    echo "--- AI 服务地址 ---"
    grep -E '^AI_SERVICE_BASE_URL=' "$ENV_FILE" || echo "未设置"
    echo ""
    echo "--- DOMAIN ---"
    grep -E '^DOMAIN=' "$ENV_FILE" || echo "未设置"
    echo ""
    echo "--- 数据库配置 ---"
    grep -E '^(MYSQL_HOST|MYSQL_PORT|MYSQL_DATABASE|MYSQL_USER|MYSQL_PASSWORD)=' "$ENV_FILE" | sed -E 's/(PASSWORD=).*/\1***/' || true
    echo ""
    echo "--- Redis 配置 ---"
    grep -E '^(REDIS_HOST|REDIS_PORT|REDIS_PASSWORD|REDIS_DATABASE)=' "$ENV_FILE" | sed -E 's/(PASSWORD=).*/\1***/' || true
    echo ""
    echo "--- LLM 配置（密钥已脱敏）---"
    grep -E '^(LLM_MODEL|LLM_BASE_URL|LLM_API_KEY)=' "$ENV_FILE" | sed -E 's/(LLM_API_KEY=).*/\1***/' || true
    echo ""
    echo "--- 内部接口 Token（已脱敏）---"
    grep -E '^INTERNAL_API_TOKEN=' "$ENV_FILE" | sed -E 's/(INTERNAL_API_TOKEN=).*/\1***/' || true
else
    echo "ERROR: .env 文件不存在: $ENV_FILE"
fi

echo ""
echo "【5/7】端口占用情况"
echo "----------------------------------------"
ss -tlnp 2>/dev/null | grep -E ':(80|8080|3306|6379|8000)\b' || echo "以上端口均未被占用"

echo ""
echo "【6/7】服务器资源"
echo "----------------------------------------"
echo "内存:"
free -h
echo ""
echo "磁盘:"
df -h /
echo ""
echo "Docker 容器资源:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>/dev/null | grep leyo || true

echo ""
echo "【7/7】服务连通性测试"
echo "----------------------------------------"
echo "测试 AI 服务 (localhost:8000):"
curl -s --max-time 5 http://localhost:8000/health && echo "" || echo "AI 服务无法访问"

echo ""
echo "测试 AI 服务 (公网IP:8000):"
curl -s --max-time 5 http://$(curl -s --max-time 3 ifconfig.me 2>/dev/null || echo '127.0.0.1'):8000/health 2>&1 | head -c 200 || echo "无法获取公网IP或访问失败"

echo ""
echo "测试后端健康检查:"
curl -s --max-time 5 http://localhost:8080/health && echo "" || echo "后端 8080 无法访问"

echo ""
echo "测试 Nginx 80 端口:"
curl -s --max-time 5 -o /dev/null -w "%{http_code}" http://localhost/health 2>/dev/null || echo "无法访问"
echo ""

echo "========================================"
echo "排查报告结束"
echo "========================================"
