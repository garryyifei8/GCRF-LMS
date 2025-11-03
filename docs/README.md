# 📚 GCRF Library Management System Documentation Center

Welcome to the centralized documentation hub for the GCRF Intelligent Library Management System.

## 📖 Quick Navigation

### 🏗️ Architecture & Design
- **[Core Architecture](architecture/)** - System design and technical architecture
- **[Backend Architecture](../backend/doc/architect.md)** - Authoritative technical specification (1570 lines)

### 💻 Development
- **[Development Progress](development/DEVELOPMENT_PROGRESS.md)** - Current development status and milestones
- **[Microservice Config Checklist](development/MICROSERVICE_CONFIG_CHECKLIST.md)** - Configuration standards
- **[Implementation Plan Stage 15](development/IMPLEMENTATION_PLAN_STAGE15.md)** - Current implementation stage

### 🚀 Deployment & DevOps
- **[Docker Documentation](deployment/docker/)** - Complete Docker build and deployment guides
  - [Docker Build Master Guide](deployment/docker/DOCKER_BUILD_MASTER_GUIDE.md)
  - [Security Scanning](deployment/docker/SECURITY_SCANNING.md)
  - [CI/CD Integration](deployment/docker/CI_CD_INTEGRATION.md)
  - [Infrastructure Setup](deployment/docker/INFRASTRUCTURE_README.md)
  - [Services Architecture](deployment/docker/SERVICES_ARCHITECTURE.md)
  - [Build Optimization](deployment/docker/BUILD_OPTIMIZATION.md)
  - [Environment Variables](deployment/docker/ENVIRONMENT_VARIABLES.md)
  - [Nacos Configuration](deployment/docker/NACOS_CONFIGURATION.md)
  - [Network Security](deployment/docker/NETWORK_SECURITY.md)
- **[Configuration Validation](deployment/CONFIG_VALIDATION_SUMMARY.md)**
- **[Service Discovery](deployment/SERVICE_DISCOVERY_SUMMARY.md)**
- **[Security Implementation](deployment/SECURITY_SCANNING_IMPLEMENTATION_REPORT.md)**

### 🧪 Testing
- **[Integration Test Patterns](testing/INTEGRATION_TEST_PATTERNS.md)** - Testing best practices
- **[Integration Test Report](testing/INTEGRATION_TEST_REPORT.md)** - Test results and coverage

### 🎨 Frontend
- **[Frontend Design Spec V2](frontend/FRONTEND_DESIGN_SPEC_V2.md)** - UI/UX specifications
- **[UI Components Library](frontend/UI_COMPONENTS_LIB.md)** - Reusable component documentation
- **[UI Design Requirements](frontend/UI_DESIGN_REQUIREMENTS.md)** - Design system and standards

### 🔧 Services Documentation
- **[System Service](services/system-service/)**
  - [Implementation Summary](services/system-service/IMPLEMENTATION_SUMMARY.md)
  - [Quick Reference](services/system-service/QUICK_REFERENCE.md)
- **[Reader Service](services/reader-service/)**
  - [Architecture](services/reader-service/ARCHITECTURE.md)

### 📦 Archives
- **[Historical Documents](archives/)** - Completed phases and legacy documentation
  - [Phase 2 Detailed Plan](archives/PHASE2_DETAILED_PLAN.md)
  - [Implementation Plan Stage 14](archives/IMPLEMENTATION_PLAN_STAGE14.md)

---

## 🗺️ Documentation Structure

```
docs/
├── architecture/        # System architecture and design documents
├── development/         # Development plans, progress, and guidelines
├── deployment/          # Deployment, Docker, and infrastructure docs
│   └── docker/         # Docker-specific documentation
├── testing/            # Test documentation and reports
├── frontend/           # Frontend design and component docs
├── services/           # Individual microservice documentation
│   ├── system-service/ # System service docs
│   └── reader-service/ # Reader service docs
└── archives/           # Completed and historical documents
```

---

## 📋 Core Project Documents

These essential documents remain in the root directory for easy access:

- **[CLAUDE.md](../CLAUDE.md)** - Main development guidelines and standards
- **[PROJECT_STATUS_SUMMARY.md](../PROJECT_STATUS_SUMMARY.md)** - High-level project status
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Complete documentation index

---

## 🔍 Finding Information

### By Topic
- **Architecture Questions** → Check `architecture/` and `backend/doc/architect.md`
- **How to Deploy** → See `deployment/docker/`
- **Service APIs** → Look in `services/[service-name]/`
- **UI Components** → Browse `frontend/`
- **Testing Approach** → Review `testing/`

### By Phase
- **Current Work** → `development/IMPLEMENTATION_PLAN_STAGE15.md`
- **Completed Work** → `archives/`
- **Overall Progress** → `development/DEVELOPMENT_PROGRESS.md`

---

## 📝 Documentation Standards

1. **Markdown Format** - All docs use Markdown for consistency
2. **Clear Naming** - Descriptive filenames in UPPER_SNAKE_CASE
3. **Version Control** - All changes tracked in Git
4. **Regular Updates** - Documents updated as implementation progresses
5. **Archive Policy** - Completed phases moved to `archives/`

---

## 🤝 Contributing

When adding new documentation:
1. Place in the appropriate category folder
2. Update this README with a link to your document
3. Update `DOCUMENTATION_INDEX.md` in the root
4. Follow existing naming conventions
5. Include a clear title and purpose at the top of your document

---

*Last Updated: 2025-11-01*
*Documentation Version: 2.0*