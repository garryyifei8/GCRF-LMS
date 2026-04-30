# Integration Test Quick Reference Card

## 🚀 Run Tests (3 Commands)

```bash
# 1. Set Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 2. Go to backend
cd backend

# 3. Run tests
./run-integration-tests.sh book-service
```

## 📁 File Locations

```
Backend Tests:
backend/{service}/src/test/java/.../integration/
├── BaseIntegrationTest.java
├── {Controller}IntegrationTest.java
└── resources/testdata/{service}-test-data.sql

Documentation:
doc/testing/
├── integration-test-guide.md        (Full guide)
├── integration-test-summary.md      (Summary)
└── QUICK_REFERENCE.md               (This file)

Scripts:
backend/run-integration-tests.sh     (Test runner)
```

## 🎯 Test Commands

```bash
# All services
./run-integration-tests.sh

# Specific service
./run-integration-tests.sh book-service

# With coverage
./run-integration-tests.sh coverage

# Clean build
./run-integration-tests.sh clean

# Help
./run-integration-tests.sh --help
```

## 📊 Coverage Status

| Service             | Tests | Status | Coverage |
| ------------------- | ----- | ------ | -------- |
| book-service        | 65    | ✅     | 85%+     |
| reader-service      | 30    | 🚧     | TBD      |
| circulation-service | 40    | 🚧     | TBD      |
| auth-service        | 15    | 🚧     | TBD      |

## 🔧 Prerequisites Checklist

- [ ] Java 21 installed
- [ ] Docker running
- [ ] Maven 3.8+
- [ ] Testcontainers dependencies in pom.xml

## 📝 Test Template

```java
@Test
@DisplayName("Should {action} {entity} {scenario}")
void test{Action}{Entity}_{scenario}() throws Exception {
    // Arrange
    SomeRequest request = new SomeRequest();
    request.setField("value");

    // Act & Assert
    mockMvc.perform(post("/api/v1/resource")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.field").value("value"));
}
```

## 🐛 Common Issues

| Problem            | Fix                                                |
| ------------------ | -------------------------------------------------- |
| Docker not running | `docker ps` → Start Docker                         |
| Wrong Java version | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| Port in use        | `brew services stop postgresql`                    |
| Tests not found    | Check `@Sql` path: `/testdata/file.sql`            |

## 📚 Documentation Links

- **Full Guide**: `doc/testing/integration-test-guide.md`
- **Summary**: `doc/testing/integration-test-summary.md`
- **Completion Report**: `DevPlan/INTEGRATION_TESTS_COMPLETE.md`
- **Quick Start**: `backend/TEST_README.md`

## 💡 Best Practices

1. ✅ Extend `BaseIntegrationTest`
2. ✅ Use `@Sql` for test data
3. ✅ Test both success and failure
4. ✅ Use descriptive test names
5. ✅ Mock external services
6. ✅ Verify with assertions

## 🎯 Coverage Goal

**Target**: 80%+ per service
**Current**: 85%+ (book-service)

## 📞 Support

- **Documentation**: `/doc/testing/`
- **Examples**: `book-service` tests
- **Team**: GCRF Test Team

---

**Last Updated**: 2025-12-01
**Quick Help**: `./run-integration-tests.sh --help`
