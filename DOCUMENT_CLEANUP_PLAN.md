# 文档清理计划

**日期**: 2025-11-02
**目的**: 清理冗余、重复、过期文档,保留权威文档

---

## 🗂️ 文档分类

### 1. **冗余重复文档** (建议归档/删除)

#### deployment/scripts/ 完成总结文档 (重复)
- `TASK3_NETWORK_SECURITY_COMPLETION.md` → 已在Phase 3完成
- `TASK4_COMPLETION_SUMMARY.md` → 已在Phase 3完成
- `TASK5_BUILD_AUTOMATION_COMPLETION.md` → 已在Phase 3完成
- `TASK5_COMPLETION_SUMMARY.md` → 与上一个重复
- `PHASE4_DAY1_SUMMARY.md` → 已在PHASE4_COMPLETION_SUMMARY.md中

#### docs/deployment/docker/ 多版本指南 (冗余)
- `DOCKER_BUILD.md` → 基础版
- `DOCKER_BUILD_MASTER_GUIDE.md` → 综合版
- `DOCKER_BUILD_QUICK_REFERENCE.md` → 快速参考
- **建议**: 保留 MASTER_GUIDE, 归档其余

#### docs/deployment/docker/ 镜像构建文档 (部分过期)
- `BUILD_OPTIMIZATION.md` → Phase 3完成
- `BUILD_OPTIMIZATION_QUICK_START.md` → 与上一个重复
- `BUILD_SCRIPTS_GUIDE.md` → 已被 AUTOMATION_GUIDE 替代
- `TASK3_DOCKER_BUILD_OPTIMIZATION_REPORT.md` → Phase 3报告

#### docs/deployment/docker/ 安全扫描文档 (重复)
- `SECURITY_SCANNING.md`
- `SECURITY_SCANNING_QUICKSTART.md`
- **建议**: 合并为一个文档

#### docs/deployment/ 服务发现文档 (重复)
- `SERVICE_DISCOVERY_SUMMARY.md` (docs/)
- `SERVICE_DISCOVERY_README.md` (scripts/)
- **建议**: 保留一个权威版本

#### docs/api/ Mock API文档 (前期文档,可能过期)
- `Mock-API-Implementation-Summary.md`
- `Mock-API-Strategy.md`
- **建议**: 检查是否还在使用

### 2. **过期文档** (建议归档)

#### backend/doc/
- `ARCHITECTURE.md` → ⚠️ **重要**: 与 docs/architecture/ARCHITECTURE.md 重复
  - 标记为 "Overview only (may be outdated)"
  - CLAUDE.md 已说明: architect.md 是权威文档

#### library-backend/ (旧后端目录,已废弃)
- `README.md`
- `infrastructure/README.md`
- `infrastructure/DELIVERABLES.md`
- `infrastructure/QUICKSTART.md`
- `library-gateway/README.md`
- **建议**: 整个 library-backend/ 目录可能已废弃

#### docs/archives/
- `DevPlan.md` → 归档
- `IMPLEMENTATION_PLAN_STAGE14.md` → 归档
- `PHASE2_DETAILED_PLAN.md` → 归档
- `Phase1_Development_Plan.md` → 归档
- **状态**: 已经在archives目录,保持现状

### 3. **权威保留文档**

#### 核心架构文档
- ✅ `backend/doc/architect.md` (1570 lines) → **权威**
- ✅ `docs/architecture/ARCHITECTURE.md` → 概览版,保留
- ✅ `CLAUDE.md` → 开发指南,保留

#### Phase 4-5 完成文档
- ✅ `deployment/monitoring/PHASE4_COMPLETION_SUMMARY.md` → 保留
- ✅ `docs/deployment/MONITORING_GUIDE.md` → 保留
- ✅ `docs/deployment/TROUBLESHOOTING_METRICS.md` → 保留
- ✅ `docs/deployment/AUTOMATION_GUIDE.md` → 保留

#### 专项文档
- ✅ `deployment/monitoring/GRAFANA_QUICKSTART.md` → 保留
- ✅ `deployment/monitoring/ACTUATOR_CONFIG_TEMPLATE.md` → 保留
- ✅ `deployment/monitoring/ALERTS_GUIDE.md` → 保留
- ✅ `docs/deployment/docker/NETWORK_SECURITY.md` → 保留
- ✅ `docs/deployment/docker/SERVICES_ARCHITECTURE.md` → 保留

#### 基础设施 README
- ✅ `backend/infrastructure/nacos/README.md` → 保留
- ✅ `backend/infrastructure/postgresql/README.md` → 保留
- ✅ `backend/infrastructure/postgresql/QUICKSTART.md` → 保留

---

## 📋 清理行动计划

### Phase 1: 立即归档 (低风险)

**归档目录**: `docs/archives/phase15_cleanup/`

```bash
# 创建归档目录
mkdir -p docs/archives/phase15_cleanup/{scripts,docker,deployment}

# 归档 scripts 完成总结
mv deployment/scripts/TASK*.md docs/archives/phase15_cleanup/scripts/
mv deployment/monitoring/PHASE4_DAY1_SUMMARY.md docs/archives/phase15_cleanup/

# 归档 docker 构建报告
mv docs/deployment/docker/TASK3_*.md docs/archives/phase15_cleanup/docker/
mv docs/deployment/docker/PHASE3_COMPLETION_REPORT.md docs/archives/phase15_cleanup/docker/
```

### Phase 2: 合并冗余 (需验证)

1. **Docker构建指南合并** → 创建统一的 DOCKER_GUIDE.md
   - 合并: BUILD_OPTIMIZATION + BUILD_SCRIPTS_GUIDE
   - 归档旧版本

2. **安全扫描文档合并** → 创建 SECURITY_GUIDE.md
   - 合并: SECURITY_SCANNING + SECURITY_SCANNING_QUICKSTART
   - 保留 SCAN_RESULTS_SUMMARY.md 作为示例

3. **服务发现文档** → 统一到 deployment/scripts/SERVICE_DISCOVERY_README.md
   - 归档: docs/deployment/SERVICE_DISCOVERY_SUMMARY.md

### Phase 3: 检查并归档旧后端 (需确认)

**⚠️ 需要确认**: `library-backend/` 目录是否已废弃?

```bash
# 如果确认废弃,整体归档
mv library-backend/ docs/archives/library-backend-deprecated/
```

### Phase 4: 检查 Mock API 文档

- 检查前端是否还在使用 Mock API
- 如果已转向真实API,归档 Mock 相关文档

---

## ✅ 清理后文档结构

```
GCRF_LibraryManagementSystem/
├── CLAUDE.md                           # 开发指南 (权威)
├── STAGE15_PROGRESS_SUMMARY.md         # 当前进度
├── PHASE6_PLAN.md                      # 当前阶段计划
├── backend/
│   ├── doc/
│   │   └── architect.md                # 技术架构 (权威1570行)
│   └── infrastructure/
│       ├── nacos/README.md
│       ├── postgresql/README.md
│       └── postgresql/QUICKSTART.md
├── deployment/
│   ├── README.md
│   ├── monitoring/
│   │   ├── PHASE4_COMPLETION_SUMMARY.md   # Phase 4总结
│   │   ├── GRAFANA_QUICKSTART.md
│   │   ├── ACTUATOR_CONFIG_TEMPLATE.md
│   │   └── ALERTS_GUIDE.md
│   └── scripts/
│       ├── ORCHESTRATION_README.md
│       ├── SERVICE_DISCOVERY_README.md
│       └── VOLUME_MANAGEMENT.md
├── docs/
│   ├── README.md
│   ├── architecture/
│   │   ├── ARCHITECTURE.md              # 架构概览
│   │   ├── architect.md                 # 符号链接→backend/doc/
│   │   ├── face-recognition-architecture.md
│   │   └── face-api-spec.md
│   ├── api/
│   │   └── API_REFERENCE.md
│   ├── deployment/
│   │   ├── MONITORING_GUIDE.md          # Phase 4
│   │   ├── TROUBLESHOOTING_METRICS.md   # Phase 4
│   │   ├── AUTOMATION_GUIDE.md          # Phase 5
│   │   ├── docker/
│   │   │   ├── DOCKER_BUILD_MASTER_GUIDE.md  # 统一指南
│   │   │   ├── SECURITY_GUIDE.md             # 合并版
│   │   │   ├── NETWORK_SECURITY.md
│   │   │   └── SERVICES_ARCHITECTURE.md
│   ├── development/
│   │   ├── IMPLEMENTATION_PLAN_STAGE15.md
│   │   └── DEVELOPMENT_PROGRESS.md
│   └── archives/
│       ├── phase15_cleanup/              # 本次清理归档
│       ├── DevPlan.md
│       ├── IMPLEMENTATION_PLAN_STAGE14.md
│       └── ...
└── web-admin/
    ├── README.md
    ├── DEVELOPMENT.md
    └── QUICKSTART.md
```

---

## 🎯 清理原则

1. **权威优先**: 保留技术最完整的版本
2. **历史保存**: 所有归档而非删除
3. **可追溯**: 清理前创建本计划文档
4. **渐进式**: 分阶段执行,避免误删

---

**创建日期**: 2025-11-02
**执行状态**: 待确认并执行
**预计清理**: 20-30个冗余文件
