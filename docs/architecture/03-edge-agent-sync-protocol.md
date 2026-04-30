# Edge Agent 同步协议

**日期：** 2026-04-30
**状态：** Approved
**ADR：** [ADR-002](../adr/ADR-002-deployment-topology.md) / [ADR-004](../adr/ADR-004-edge-agent-sync.md)

---

## 1. 协议总览

```
┌──────────────────┐         ┌────────────────────┐
│  Edge Agent      │  ──WSS──│  sync-service      │
│  (school_001)    │         │  (cloud, K8s)      │
└────────┬─────────┘         └─────────┬──────────┘
         │                              │
         │ LAN HTTP (80)                │ 内部 REST
         ▼                              ▼
   借还工位浏览器              circulation/reader/...
                              service
```

## 2. 启动 / 注册

### 2.1 首次部署

```bash
EDGE_SCHOOL_ID=000123 EDGE_DEPLOY_TOKEN=xxx ./install-edge.sh
```

### 2.2 启动后

```
1. Edge Agent 启动
2. POST /sync/register?schoolId=000123 &deployToken=xxx
   → cloud 返回 mTLS 客户端证书 + WSS endpoint
3. 持久化证书到 /data/cert/
4. 建立 WSS 连接：wss://sync.gcrf.region/edge/000123
5. 拉取初始快照：
   GET /sync/snapshot/000123?since=0
   → 返回该校全量读者 + 馆藏 + 当日借还 + 馆藏标准
6. 落本地 SQLite
```

## 3. 消息格式（WSS 帧）

所有 WSS 消息为 JSON：

```json
{
  "type": "BORROW_REQUEST" | "RETURN_REQUEST" | "READER_UPDATE" | ...,
  "msgId": "uuid",
  "schoolId": "000123",
  "ts": 1714498800,
  "payload": { ... },
  "idempotencyKey": "school_000123_borrow_20260430_001"
}
```

## 4. 上行消息类型

### 4.1 借书 (BORROW_REQUEST)

```json
{
  "type": "BORROW_REQUEST",
  "msgId": "uuid",
  "schoolId": "000123",
  "ts": 1714498800,
  "payload": {
    "barcode": "B000001234",
    "readerId": 5678,
    "operatorId": 1,
    "borrowAt": "2026-04-30T10:30:00Z",
    "dueAt": "2026-05-30T10:30:00Z"
  },
  "idempotencyKey": "school_000123_borrow_20260430103000_5678_B000001234"
}
```

云端响应：

```json
{
  "type": "BORROW_ACK" | "BORROW_REJECT",
  "msgId": "uuid (echo)",
  "result": "OK" | "CONFLICT" | "ERROR",
  "error": { "code": "ALREADY_BORROWED", "by": "schoolId/readerId" }
}
```

### 4.2 还书 (RETURN_REQUEST)

同结构。

### 4.3 续借 (RENEW_REQUEST)

同结构。

### 4.4 心跳 (HEARTBEAT)

每 30 秒：

```json
{
  "type": "HEARTBEAT",
  "schoolId": "000123",
  "ts": 1714498800,
  "stats": { "queueDepth": 3, "lastSyncAt": 1714498770, "uptimeSeconds": 3600 }
}
```

## 5. 下行消息类型

### 5.1 读者状态变更 (READER_UPDATE)

云端管理员冻结某读者 → 推送到对应学校 Edge Agent：

```json
{
  "type": "READER_UPDATE",
  "schoolId": "000123",
  "payload": { "readerId": 5678, "status": "FROZEN", "reason": "..." }
}
```

Edge Agent 收到后更新本地 SQLite，下次借阅校验生效。

### 5.2 图书状态变更 (BOOK_UPDATE)

新书入馆、馆藏地转移、剔旧 → 推送。

### 5.3 馆藏标准变更 (STANDARD_UPDATE)

区域管理员调整标准 → 推送下行。

### 5.4 通知 (NOTIFICATION)

系统通知、紧急广播。

## 6. 离线 / 重连恢复

### 6.1 网络断

- Edge Agent WSS 断线，进入"本地模式"
- 借还操作仍可进行，写本地 SQLite + 写 `pending_queue` 表
- 工位 UI 显示"⚠ 本地模式（云端未同步）"

### 6.2 网络恢复

- WSS 重连成功后：
  1. 上传 `pending_queue` 全部消息（按时间序）
  2. 拉取离线期间云端变更：`GET /sync/snapshot/{schoolId}?since=<lastSyncTs>`
  3. 应用到本地 SQLite
  4. UI 切换为"在线模式"

### 6.3 重连退避

- 失败后 1s, 2s, 4s, 8s, 16s, ..., max 60s 重试
- 累计断线 > 1 小时 → 心跳告警上报

## 7. 冲突解决

### 7.1 借书冲突（同一本书多个工位扫码）

- 上行带 `idempotencyKey`（含 barcode + ts）
- 云端 `sync-service` 用 Redis SETNX 锁 `lock:borrow:<barcode>` 1 秒
- 先到先借，后到的拿 `BORROW_REJECT { CONFLICT }`
- Edge Agent 收到 REJECT → 回滚本地 SQLite + UI 提示

### 7.2 读者状态冲突

- 云端为权威。例如：A 校 Edge Agent 本地认为读者活跃，但云端已冻结
- 借阅请求上行 → 云端识别冻结 → REJECT
- 云端推送 READER_UPDATE 强制覆盖本地

### 7.3 时钟漂移

- Edge Agent 启动时通过 NTP 同步时钟
- 上行消息带本地 ts，云端二次校验偏差 > 5 min 拒绝并告警

## 8. 安全

- **mTLS**：云端给每个学校 Edge Agent 颁发证书，绑定 schoolId
- **JWT** 二次校验：每条上行消息带 Edge Agent JWT（短期，每 1 小时刷新）
- **本地 SQLite AES-256**：数据库文件加密，密钥来自 deployToken
- **LAN 入站**：浏览器 → Edge Agent 走 JWT 校验（依赖 SSO 登录）

## 9. 性能

- WSS 单连接吞吐：10 msg/s 足够（开学日峰值单校 < 1 msg/s）
- 同步延迟：在线模式 < 200ms 可见云端
- 离线积压：本地 SQLite 可缓 10 万条

## 10. 关联文档

- [ADR-002](../adr/ADR-002-deployment-topology.md) / [ADR-004](../adr/ADR-004-edge-agent-sync.md)
- [02-deployment-topology](02-deployment-topology.md)
- [05-data-model](05-data-model.md)
