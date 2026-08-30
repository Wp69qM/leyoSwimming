#!/bin/bash
# 检查 leyoSwimming 生产环境服务状态
# 用法：ssh root@<服务器IP> 'bash -s' < scripts/check-leyo-services.sh
# 或上传到服务器后执行：chmod +x check-leyo-services.sh && ./check-leyo-services.sh

set -e

COMPOSE_FILE="/opt/leyo-swimming/deploy/docker-compose.prod.yml"

echo "========================================"
echo "Docker 服务状态"
echo "========================================"
systemctl status docker --no-pager || true

echo ""
echo "========================================"
echo "leyo 容器运行状态"
echo "========================================"
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.State}}\t{{.Ports}}" | grep leyo || true

echo ""
echo "========================================"
echo "容器健康状态"
echo "========================================"
for container in leyo-backend leyo-mysql leyo-redis leyo-nginx; do
    if docker inspect "$container" >/dev/null 2>&1; then
        status=$(docker inspect --format="{{.State.Status}}" "$container")
        health=$(docker inspect --format="{{.State.Health.Status}}" "$container" 2>/dev/null || echo "无健康检查")
        echo "$container: $status / $health"
    else
        echo "$container: 不存在"
    fi
done

echo ""
echo "========================================"
echo "资源使用"
echo "========================================"
free -h
echo ""
df -h /

echo ""
echo "========================================"
echo "端口占用"
echo "========================================"
ss -tlnp | grep -E ':(80|8080|3306|6379|8000)\b' || true

if [ -f "$COMPOSE_FILE" ]; then
    echo ""
    echo "========================================"
    echo "Compose 服务状态"
    echo "========================================"
    docker compose -f "$COMPOSE_FILE" ps || true
fi

echo ""
echo "提示：查看实时日志可运行 docker logs -f <容器名>"
