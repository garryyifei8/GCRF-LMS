#!/bin/sh
# ====================================================================
# PostgreSQL 自动备份脚本
# 用途: 定期执行增量和全量备份
# 备份策略:
#   - 每日凌晨2点: 增量备份（仅WAL日志）
#   - 每周日凌晨3点: 全量备份（完整数据库）
# ====================================================================

set -e

BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_TYPE="${1:-incremental}"
RETENTION_DAYS=30

# 数据库连接信息
PGHOST="${PGHOST:-postgres-primary}"
PGPORT="${PGPORT:-5432}"
PGUSER="${POSTGRES_USER:-postgres}"
export PGPASSWORD="${POSTGRES_PASSWORD:-gcrf_secure_2024}"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 创建备份目录
mkdir -p "${BACKUP_DIR}/full"
mkdir -p "${BACKUP_DIR}/incremental"
mkdir -p "${BACKUP_DIR}/wal"
mkdir -p "${BACKUP_DIR}/logs"

LOG_FILE="${BACKUP_DIR}/logs/backup_${DATE}.log"

# 日志重定向
exec 1>"${LOG_FILE}"
exec 2>&1

log "========================================="
log "开始执行备份: ${BACKUP_TYPE}"
log "========================================="

# 等待数据库就绪
log "检查数据库连接..."
until pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}"; do
    log "数据库未就绪，等待5秒..."
    sleep 5
done
log "数据库连接正常"

# 全量备份
if [ "${BACKUP_TYPE}" = "full" ]; then
    log "开始全量备份..."
    BACKUP_FILE="${BACKUP_DIR}/full/full_backup_${DATE}.tar.gz"

    # 使用pg_basebackup创建全量备份
    pg_basebackup \
        -h "${PGHOST}" \
        -p "${PGPORT}" \
        -U "${PGUSER}" \
        -D "${BACKUP_DIR}/full/temp_${DATE}" \
        -Ft \
        -z \
        -P \
        -v

    # 打包备份
    tar -czf "${BACKUP_FILE}" -C "${BACKUP_DIR}/full" "temp_${DATE}"
    rm -rf "${BACKUP_DIR}/full/temp_${DATE}"

    log "全量备份完成: ${BACKUP_FILE}"
    log "备份大小: $(du -h ${BACKUP_FILE} | cut -f1)"

    # 备份所有数据库的Schema
    log "备份数据库Schema..."
    for db in auth_service book_service reader_service circulation_service system_service \
              recommend_service nlp_service vision_service analytics_service \
              notification_service file_service search_service postgres; do
        if psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -lqt | cut -d \| -f 1 | grep -qw "${db}"; then
            SCHEMA_FILE="${BACKUP_DIR}/full/schema_${db}_${DATE}.sql"
            pg_dump -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${db}" --schema-only > "${SCHEMA_FILE}"
            gzip "${SCHEMA_FILE}"
            log "已备份 ${db} 的Schema"
        fi
    done

# 增量备份（WAL归档）
elif [ "${BACKUP_TYPE}" = "incremental" ]; then
    log "开始增量备份（WAL归档）..."

    # 执行WAL归档
    psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -c "SELECT pg_switch_wal();" >/dev/null

    # 备份当前WAL文件
    WAL_BACKUP_DIR="${BACKUP_DIR}/wal/${DATE}"
    mkdir -p "${WAL_BACKUP_DIR}"

    # 使用pg_receivewal接收WAL日志
    timeout 60 pg_receivewal \
        -h "${PGHOST}" \
        -p "${PGPORT}" \
        -U "${PGUSER}" \
        -D "${WAL_BACKUP_DIR}" \
        --synchronous \
        -v || true

    log "增量备份完成"
    log "WAL文件数: $(find ${WAL_BACKUP_DIR} -type f | wc -l)"
fi

# 清理过期备份
log "清理超过 ${RETENTION_DAYS} 天的旧备份..."
find "${BACKUP_DIR}/full" -name "full_backup_*.tar.gz" -mtime +${RETENTION_DAYS} -delete
find "${BACKUP_DIR}/full" -name "schema_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
find "${BACKUP_DIR}/wal" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} + 2>/dev/null || true
find "${BACKUP_DIR}/logs" -name "backup_*.log" -mtime +${RETENTION_DAYS} -delete

# 备份统计
log "========================================="
log "备份统计信息:"
log "全量备份数: $(find ${BACKUP_DIR}/full -name 'full_backup_*.tar.gz' | wc -l)"
log "WAL归档数: $(find ${BACKUP_DIR}/wal -type d -mindepth 1 | wc -l)"
log "总备份大小: $(du -sh ${BACKUP_DIR} | cut -f1)"
log "========================================="
log "备份任务完成"

# 验证备份完整性
if [ "${BACKUP_TYPE}" = "full" ]; then
    log "验证备份完整性..."
    if tar -tzf "${BACKUP_FILE}" >/dev/null 2>&1; then
        log "备份文件完整性验证通过"
    else
        log "错误: 备份文件损坏！"
        exit 1
    fi
fi

# 发送备份通知（可选，集成通知服务）
# curl -X POST "http://notification-service/api/notifications" \
#     -H "Content-Type: application/json" \
#     -d "{\"type\":\"backup\",\"status\":\"success\",\"message\":\"${BACKUP_TYPE} backup completed\"}"

log "所有任务完成"
exit 0
