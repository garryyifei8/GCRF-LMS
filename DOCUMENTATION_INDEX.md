# GCRF Library Management System - Documentation Index

**Last Updated**: 2025-11-01
**Purpose**: Quick reference guide to all project documentation
**Version**: 3.0 - Post-Reorganization

---

## 🆕 Documentation Reorganization Complete (2025-11-01)

All documentation has been reorganized into a clear hierarchical structure under the `docs/` directory for improved navigation and maintenance.

---

## Primary Documentation (Root Level)

### 📋 Essential Guidelines & Status
- **`CLAUDE.md`** (ROOT)
  - **Status**: ✅ Current (2025-10-25)
  - **Purpose**: Official development guidelines and instructions
  - **Audience**: All developers
  - **Authority**: Primary development process document

- **`PROJECT_STATUS_SUMMARY.md`** (ROOT)
  - **Status**: ✅ Current (2025-11-01)
  - **Purpose**: High-level project status and executive summary
  - **Audience**: Stakeholders, project managers

- **`DOCUMENTATION_INDEX.md`** (ROOT - This File)
  - **Status**: ✅ Current (2025-11-01 v3.0)
  - **Purpose**: Complete documentation reference guide
  - **Audience**: All team members

---

## 📚 Documentation Center (`docs/`)

### 🏗️ Architecture & Technical Specs (`docs/architecture/`)
- **`docs/architecture/architect.md`** ⚠️ AUTHORITATIVE
  - **Status**: ✅ Current (1570 lines)
  - **Purpose**: Complete technical specification (database schemas, API contracts, architecture)
  - **Audience**: Backend developers, architects
  - **Note**: Single source of truth for technical decisions

- **`docs/architecture/ARCHITECTURE.md`**
  - **Status**: ⚠️ Overview only (may contain outdated references)
  - **Purpose**: High-level architecture overview

- **`docs/architecture/face-recognition-architecture.md`**
  - **Purpose**: Face recognition system architecture and integration

- **`docs/architecture/face-api-spec.md`**
  - **Purpose**: Face recognition API specifications

- **`docs/architecture/information-architecture.md`**
  - **Purpose**: Information architecture and data organization

- **`docs/architecture/业务流程图.md`**
  - **Purpose**: Business process diagrams (Chinese)

- **`docs/architecture/用例图.md`**
  - **Purpose**: Use case diagrams (Chinese)

### 💻 Development Documentation (`docs/development/`)
- **`DEVELOPMENT_PROGRESS.md`**
  - **Status**: ✅ Current (2025-11-01)
  - **Purpose**: Comprehensive progress tracker with all completed stages
  - **Update Frequency**: After each major stage completion

- **`MICROSERVICE_CONFIG_CHECKLIST.md`**
  - **Status**: ✅ Current
  - **Purpose**: Microservice configuration standards and checklist

- **`IMPLEMENTATION_PLAN_STAGE15.md`**
  - **Status**: 🔄 In Progress (60% complete)
  - **Purpose**: Stage 15 Nacos configuration and Docker infrastructure

- **`chrome-debugging-guide.md`**
  - **Purpose**: Chrome DevTools debugging guide for development

### 🚀 Deployment & Infrastructure (`docs/deployment/`)

#### Docker Documentation (`docs/deployment/docker/`)
- **`DOCKER_BUILD_MASTER_GUIDE.md`** ⚠️ PRIMARY BUILD GUIDE (1,737 lines)
  - **Purpose**: Complete Docker build lifecycle documentation
  - **Topics**: Multi-stage builds, optimization, security, CI/CD

- **`DOCKER_BUILD_QUICK_REFERENCE.md`** (535 lines)
  - **Purpose**: Quick command reference for daily operations

- **`BUILD_OPTIMIZATION.md`** (820 lines)
  - **Purpose**: Build performance optimization techniques
  - **Metrics**: 77-88% improvement achieved

- **`SECURITY_SCANNING.md`** (1,019 lines)
  - **Purpose**: Trivy security scanning integration
  - **Topics**: Vulnerability management, remediation

- **`INFRASTRUCTURE_README.md`**
  - **Purpose**: Infrastructure setup and configuration

- **`SERVICES_ARCHITECTURE.md`**
  - **Purpose**: Service architecture and dependencies

- **`NACOS_CONFIGURATION.md`** (400+ lines)
  - **Purpose**: Nacos service discovery configuration

- **`ENVIRONMENT_VARIABLES.md`** (900+ lines)
  - **Purpose**: Comprehensive environment variable documentation
  - **Security**: 60+ variables with sensitivity levels

- **`NETWORK_SECURITY.md`** (1,164 lines)
  - **Purpose**: 3-tier network security architecture

#### Deployment Summaries (`docs/deployment/`)
- **`CONFIG_VALIDATION_SUMMARY.md`**
- **`SERVICE_DISCOVERY_SUMMARY.md`**
- **`SECURITY_SCANNING_IMPLEMENTATION_REPORT.md`**

### 🧪 Testing Documentation (`docs/testing/`)
- **`INTEGRATION_TEST_PATTERNS.md`**
  - **Status**: ✅ Current (500+ lines)
  - **Purpose**: Testing patterns and best practices for microservices
  - **Audience**: All developers writing tests

- **`INTEGRATION_TEST_REPORT.md`**
  - **Purpose**: Test results and coverage metrics

### 🎨 Frontend Documentation (`docs/frontend/`)
- **`FRONTEND_DESIGN_SPEC_V2.md`**
  - **Purpose**: UI/UX specifications and requirements

- **`UI_COMPONENTS_LIB.md`**
  - **Purpose**: Reusable component documentation

- **`UI_DESIGN_REQUIREMENTS.md`**
  - **Purpose**: Design system and standards

- **`ui-design-guidelines.md`**
  - **Purpose**: Comprehensive UI design guidelines and principles

- **`interaction-design.md`**
  - **Purpose**: Interaction design patterns and user flows

- **`interaction-prototype.md`**
  - **Purpose**: Interactive prototype documentation

### 🔧 Services Documentation (`docs/services/`)

#### System Service (`docs/services/system-service/`)
- **`IMPLEMENTATION_SUMMARY.md`**
  - **Status**: ✅ Complete (2025-10-29)
  - **Purpose**: System service implementation details

- **`QUICK_REFERENCE.md`**
  - **Purpose**: Quick API and usage reference

#### Reader Service (`docs/services/reader-service/`)
- **`ARCHITECTURE.md`**
  - **Purpose**: Reader service architecture and design

### 📦 Archives (`docs/archives/`)
- **`PHASE2_DETAILED_PLAN.md`**
  - **Status**: ✅ Complete (historical reference)

- **`IMPLEMENTATION_PLAN_STAGE14.md`**
  - **Status**: ✅ Complete (historical reference)

- **`Phase1_Development_Plan.md`**
  - **Status**: ✅ Complete (historical reference)

- **`DevPlan.md`**
  - **Status**: ✅ Complete (historical development plan)

- **`PRD.md`**
  - **Status**: ✅ Complete (original product requirements document)

---

## Documentation Hierarchy (New Structure)

```
GCRF_LibraryManagementSystem/
│
├── 📋 Root Documents (Essential)
│   ├── CLAUDE.md                     ⭐ Development guidelines
│   ├── PROJECT_STATUS_SUMMARY.md     📊 Project status
│   └── DOCUMENTATION_INDEX.md        📖 This file
│
├── 📚 docs/                          Documentation Center
│   ├── README.md                     🗺️ Documentation navigation
│   │
│   ├── architecture/                 🏗️ Architecture docs (7 files)
│   │   ├── architect.md              ⚠️ AUTHORITATIVE spec
│   │   ├── ARCHITECTURE.md
│   │   ├── face-recognition-architecture.md
│   │   ├── face-api-spec.md
│   │   ├── information-architecture.md
│   │   ├── 业务流程图.md
│   │   └── 用例图.md
│   │
│   ├── api/                          🔌 API documentation (3 files)
│   │   ├── API-Design.md
│   │   ├── Mock-API-Strategy.md
│   │   └── Mock-API-Implementation-Summary.md
│   │
│   ├── development/                  💻 Development docs (4 files)
│   │   ├── DEVELOPMENT_PROGRESS.md
│   │   ├── MICROSERVICE_CONFIG_CHECKLIST.md
│   │   ├── IMPLEMENTATION_PLAN_STAGE15.md
│   │   └── chrome-debugging-guide.md
│   │
│   ├── deployment/                   🚀 Deployment docs
│   │   ├── docker/                   🐳 Docker guides (19 files)
│   │   └── [Summary reports]
│   │
│   ├── testing/                      🧪 Testing docs
│   │   ├── INTEGRATION_TEST_PATTERNS.md
│   │   └── INTEGRATION_TEST_REPORT.md
│   │
│   ├── frontend/                     🎨 Frontend docs (6 files)
│   │   ├── FRONTEND_DESIGN_SPEC_V2.md
│   │   ├── UI_COMPONENTS_LIB.md
│   │   ├── UI_DESIGN_REQUIREMENTS.md
│   │   ├── ui-design-guidelines.md
│   │   ├── interaction-design.md
│   │   └── interaction-prototype.md
│   │
│   ├── services/                     🔧 Service docs
│   │   ├── system-service/
│   │   └── reader-service/
│   │
│   └── archives/                     📦 Historical docs (5 files)
│       ├── PHASE2_DETAILED_PLAN.md
│       ├── IMPLEMENTATION_PLAN_STAGE14.md
│       ├── Phase1_Development_Plan.md
│       ├── DevPlan.md
│       └── PRD.md
```

---

## Documentation Usage Guidelines

### For New Developers
1. Start with `CLAUDE.md` - Understand development workflow
2. Navigate to `docs/README.md` - Explore documentation center
3. Read `docs/development/DEVELOPMENT_PROGRESS.md` - See current status
4. Study `docs/architecture/architect.md` - Learn the architecture
5. Review `docs/testing/INTEGRATION_TEST_PATTERNS.md` - Learn testing practices

### For Finding Specific Documentation
1. **Architecture** → `docs/architecture/` (architect.md is authoritative)
2. **API Documentation** → `docs/api/`
3. **Development Progress** → `docs/development/`
4. **Docker & Deployment** → `docs/deployment/docker/`
5. **Testing** → `docs/testing/`
6. **Frontend/UI** → `docs/frontend/`
7. **Service-specific** → `docs/services/[service-name]/`
8. **Historical/Completed** → `docs/archives/`

### Authority Order (for conflicts)
1. `docs/architecture/architect.md` - Technical decisions
2. `CLAUDE.md` - Development process
3. `PROJECT_STATUS_SUMMARY.md` - Current status
4. Stage-specific plans - Implementation details

---

## Recent Changes (2025-11-01)

### Documentation Reorganization (Phase 3)
- ✅ Moved 17 documents from `doc/` to `docs/` with categorization
- ✅ Deleted `doc/` and `backend/doc/` directories
- ✅ Created new `docs/architecture/` (7 files) and `docs/api/` (3 files) categories
- ✅ Updated all document references in DOCUMENTATION_INDEX.md
- ✅ Total cleanup: 36+ documents moved, 13 obsolete documents deleted across all phases

### Deleted Documents
- ❌ `backend/system-service/FINAL_REPORT.md` (obsolete)
- ❌ `backend/reader-service/REFACTORING_COMPLETE.md` (obsolete)
- ❌ `backend/infrastructure/postgresql/DEPLOYMENT_REPORT.md` (obsolete)
- ❌ `CLAUDE_back.md` (backup file)
- ❌ `AGENTS.md` (moved to .claude/agents/)
- ❌ `backend/auth-service/DOCKER_BUILD.md` (moved to deployment/docker)

---

## Quick Links by Role

### Backend Developers
- Guidelines: `CLAUDE.md`
- Architecture: `docs/architecture/architect.md`
- API Design: `docs/api/API-Design.md`
- Testing: `docs/testing/INTEGRATION_TEST_PATTERNS.md`
- Progress: `docs/development/DEVELOPMENT_PROGRESS.md`

### Frontend Developers
- Design Specs: `docs/frontend/FRONTEND_DESIGN_SPEC_V2.md`
- Components: `docs/frontend/UI_COMPONENTS_LIB.md`
- Requirements: `docs/frontend/UI_DESIGN_REQUIREMENTS.md`

### DevOps Engineers
- Docker Guide: `docs/deployment/docker/DOCKER_BUILD_MASTER_GUIDE.md`
- Infrastructure: `docs/deployment/docker/INFRASTRUCTURE_README.md`
- Security: `docs/deployment/docker/SECURITY_SCANNING.md`
- Quick Ref: `docs/deployment/docker/DOCKER_BUILD_QUICK_REFERENCE.md`

### Project Managers
- Status: `PROJECT_STATUS_SUMMARY.md`
- Progress: `docs/development/DEVELOPMENT_PROGRESS.md`
- Current Stage: `docs/development/IMPLEMENTATION_PLAN_STAGE15.md`

---

## Test Documentation Summary

### Test Files by Service
- **Auth Service**: 96 tests in `auth-service/src/test/`
- **Gateway Service**: 21 tests in `gateway-service/src/test/`
- **Common Core**: 129 tests in `common/common-core/src/test/`
- **Common Web**: 26 tests in `common/common-web/src/test/`

**Total**: 272 tests (100% passing as of 2025-11-01)

---

## Maintenance Notes

- **Documentation Center**: `docs/` directory is the ONLY documentation location
- **Root Documents**: Only 3 essential project-wide documents remain in root (CLAUDE.md, PROJECT_STATUS_SUMMARY.md, DOCUMENTATION_INDEX.md)
- **Authoritative Spec**: `docs/architecture/architect.md` is the single source of truth for technical decisions
- **Update Frequency**: This index updated with major documentation changes
- **Next Update**: After Stage 15 completion (Nacos configuration)

---

**Questions or Issues?**
- Check `docs/README.md` for navigation
- Review `CLAUDE.md` for guidelines
- Consult `docs/architecture/architect.md` for technical answers
- Browse `docs/` directory categories for specific topics