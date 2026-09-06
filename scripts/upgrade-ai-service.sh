#!/bin/bash
# AI Service RAG / Tavily 兜底功能升级脚本
# 适用场景：服务器已平稳运行，本次仅更新 ai-service 代码与配置
# 运行方式：ssh 到服务器后执行 ./deploy/upgrade-ai-service.sh

set -euo pipefail

# ============================================================================
# 可配置变量（请根据实际服务器环境修改）
# ============================================================================
DEPLOY_DIR="/opt/leyoSwimming"
AI_SERVICE_DIR="${DEPLOY_DIR}/ai-service"
BACKEND_DIR="${DEPLOY_DIR}/backend"
WEB_ADMIN_DIST_DIR="${DEPLOY_DIR}/web-admin-dist"
AI_SERVICE_SYSTEMD="leyo-ai-service"
BACKEND_SYSTEMD="leyo-backend"
NGINX_SERVICE="nginx"
BACKUP_DIR="/opt/leyoSwimming/backups"

# 本地构建产物文件名（由 build-and-upload-host.sh 上传）
AI_SERVICE_TAR="ai-service-release.tar.gz"
BACKEND_JAR="leyo-swimming-backend.jar"

# 健康检查重试
HEALTH_MAX_RETRY=30
HEALTH_INTERVAL=2

# ============================================================================
# 颜色输出
# ============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# 前置检查
# ============================================================================
check_root() {
    if [[ "$EUID" -ne 0 ]]; then
        log_error "请使用 root 用户或 sudo 运行此脚本"
        exit 1
    fi
}

check_directory() {
    if [[ ! -d "$1" ]]; then
        log_error "目录不存在: $1"
        exit 1
    fi
}

check_systemd_service() {
    if ! systemctl list-unit-files | grep -q "^$1"; then
        log_warn "systemd 服务不存在: $1"
        return 1
    fi
    return 0
}

# ============================================================================
# 备份
# ============================================================================
backup() {
    log_info "开始备份..."
    mkdir -p "${BACKUP_DIR}"

    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)

    # 备份 ai-service .env
    if [[ -f "${AI_SERVICE_DIR}/.env" ]]; then
        cp "${AI_SERVICE_DIR}/.env" "${BACKUP_DIR}/ai-service.env.${timestamp}.bak"
        log_info "已备份 ai-service/.env"
    fi

    # 备份 Chroma 数据
    if [[ -d "${AI_SERVICE_DIR}/data/chroma" ]]; then
        tar czf "${BACKUP_DIR}/chroma-${timestamp}.tar.gz" -C "${AI_SERVICE_DIR}" data/chroma
        log_info "已备份 Chroma 数据"
    fi

    # 备份 backend jar
    if [[ -f "${BACKEND_DIR}/${BACKEND_JAR}" ]]; then
        cp "${BACKEND_DIR}/${BACKEND_JAR}" "${BACKEND_DIR}/${BACKEND_JAR}.${timestamp}.bak"
        log_info "已备份 backend jar"
    fi

    log_info "备份完成，位置: ${BACKUP_DIR}"
}

# ============================================================================
# 健康检查
# ============================================================================
wait_for_ai_service() {
    log_info "等待 ai-service 健康检查..."
    local retry=0
    while [[ $retry -lt $HEALTH_MAX_RETRY ]]; do
        if curl -fsS "http://localhost:8000/health" >/dev/null 2>&1; then
            log_info "ai-service 健康检查通过"
            return 0
        fi
        retry=$((retry + 1))
        sleep $HEALTH_INTERVAL
    done
    log_error "ai-service 健康检查失败，请查看日志: journalctl -u ${AI_SERVICE_SYSTEMD} -n 100"
    return 1
}

# ============================================================================
# 升级 ai-service
# ============================================================================
upgrade_ai_service() {
    log_info "开始升级 ai-service..."

    check_directory "${AI_SERVICE_DIR}"

    # 1. 检查是否有新的代码包
    if [[ ! -f "${DEPLOY_DIR}/${AI_SERVICE_TAR}" ]]; then
        log_warn "未找到 ${DEPLOY_DIR}/${AI_SERVICE_TAR}，跳过 ai-service 代码更新"
        log_warn "如需更新，请先在本地运行 build-and-upload-host.sh 上传代码包"
        return 0
    fi

    # 2. 停止服务
    log_info "停止 ai-service..."
    if check_systemd_service "${AI_SERVICE_SYSTEMD}"; then
        systemctl stop "${AI_SERVICE_SYSTEMD}" || true
    else
        pkill -f "uvicorn app.main:app" || true
    fi
    sleep 2

    # 3. 解压覆盖代码
    log_info "解压 ai-service 代码..."
    tar xzvf "${DEPLOY_DIR}/${AI_SERVICE_TAR}" -C "${AI_SERVICE_DIR}"

    # 4. 更新依赖
    log_info "更新 ai-service 依赖..."
    cd "${AI_SERVICE_DIR}"
    if [[ ! -d ".venv" ]]; then
        python3 -m venv .venv
    fi
    source .venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements.txt

    # 5. 检查关键依赖
    python -c "import chromadb; import tavily; import langchain_chroma; print('dependencies ok')"

    # 6. 检查环境变量
    if [[ ! -f ".env" ]]; then
        log_warn "ai-service/.env 不存在，请按升级文档第 7 节创建"
    fi

    # 7. 启动服务
    log_info "启动 ai-service..."
    if check_systemd_service "${AI_SERVICE_SYSTEMD}"; then
        systemctl start "${AI_SERVICE_SYSTEMD}"
    else
        log_warn "未找到 systemd 服务，请手动启动 ai-service"
        return 1
    fi

    # 8. 健康检查
    wait_for_ai_service

    # 9. 清理代码包
    rm -f "${DEPLOY_DIR}/${AI_SERVICE_TAR}"

    log_info "ai-service 升级完成"
}

# ============================================================================
# 升级 backend（可选）
# ============================================================================
upgrade_backend() {
    log_info "开始升级 backend..."

    check_directory "${BACKEND_DIR}"

    if [[ ! -f "${DEPLOY_DIR}/${BACKEND_JAR}" ]]; then
        log_warn "未找到 ${DEPLOY_DIR}/${BACKEND_JAR}，跳过 backend 更新"
        return 0
    fi

    # 1. 停止服务
    log_info "停止 backend..."
    if check_systemd_service "${BACKEND_SYSTEMD}"; then
        systemctl stop "${BACKEND_SYSTEMD}" || true
    else
        pkill -f "leyo-swimming-backend.jar" || true
    fi
    sleep 2

    # 2. 替换 jar
    cp "${DEPLOY_DIR}/${BACKEND_JAR}" "${BACKEND_DIR}/${BACKEND_JAR}"

    # 3. 启动服务
    log_info "启动 backend..."
    if check_systemd_service "${BACKEND_SYSTEMD}"; then
        systemctl start "${BACKEND_SYSTEMD}"
    else
        log_warn "未找到 systemd 服务，请手动启动 backend"
        return 1
    fi

    # 4. 清理 jar 包
    rm -f "${DEPLOY_DIR}/${BACKEND_JAR}"

    log_info "backend 升级完成"
}

# ============================================================================
# 升级 web-admin（可选）
# ============================================================================
upgrade_web_admin() {
    log_info "开始升级 web-admin..."

    check_directory "${WEB_ADMIN_DIST_DIR}"

    # web-admin 已通过 rsync 直接更新到 WEB_ADMIN_DIST_DIR
    # 只需检查 nginx 配置并重载
    if systemctl is-active --quiet "${NGINX_SERVICE}"; then
        log_info "重载 nginx..."
        nginx -t
        systemctl reload "${NGINX_SERVICE}"
    else
        log_warn "nginx 未运行，请手动检查"
    fi

    log_info "web-admin 升级完成"
}

# ============================================================================
# 主流程
# ============================================================================
main() {
    log_info "==================== Leyo AI Service 升级脚本 ===================="

    check_root
    check_directory "${DEPLOY_DIR}"

    # 解析参数
    UPGRADE_BACKEND=false
    UPGRADE_WEB_ADMIN=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --backend)
                UPGRADE_BACKEND=true
                shift
                ;;
            --web-admin)
                UPGRADE_WEB_ADMIN=true
                shift
                ;;
            --all)
                UPGRADE_BACKEND=true
                UPGRADE_WEB_ADMIN=true
                shift
                ;;
            -h|--help)
                echo "用法: $0 [选项]"
                echo "选项:"
                echo "  --backend    同时升级 backend"
                echo "  --web-admin  同时升级 web-admin（重载 nginx）"
                echo "  --all        升级 ai-service + backend + web-admin"
                echo "  -h, --help   显示帮助"
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                exit 1
                ;;
        esac
    done

    backup
    upgrade_ai_service

    if [[ "$UPGRADE_BACKEND" == true ]]; then
        upgrade_backend
    fi

    if [[ "$UPGRADE_WEB_ADMIN" == true ]]; then
        upgrade_web_admin
    fi

    log_info "==================== 升级完成 ===================="
    log_info "ai-service 状态:"
    systemctl status "${AI_SERVICE_SYSTEMD}" --no-pager || true

    if [[ "$UPGRADE_BACKEND" == true ]]; then
        log_info "backend 状态:"
        systemctl status "${BACKEND_SYSTEMD}" --no-pager || true
    fi
}

main "$@"
