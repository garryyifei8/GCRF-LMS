# 扫尾：全栈覆盖率基线

**日期：** 2026-04-16
**状态：** Approved
**优先级：** 扫尾（覆盖率基线建立）
**背景：** 478 个单测 + 66 个 E2E 已就位，但前端 vitest coverage 未配置（无 exclude/reporter/HTML），后端完全无 jacoco。需要建立可视化覆盖率报告 + hotspot 清单作为后续补测依据。不设强阈值避免阻塞合并。

---

## 目标

1. 前端 vitest coverage 完整配置（reporters + exclude + `all: true`）
2. 后端 JaCoCo Maven 插件配置（根 pom 继承所有子模块）
3. CI 上传 JaCoCo artifact（前端 coverage 已上传）
4. 生成 `docs/coverage-baseline.md` 记录基线百分比与 hotspot

---

## 前端：vitest.config.js 增强

```javascript
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.test.{js,ts}", "src/**/*.spec.{js,ts}"],
    css: false,
    coverage: {
      provider: "v8",
      reporter: ["text", "text-summary", "html", "lcov", "json-summary"],
      reportsDirectory: "./coverage",
      include: ["src/**/*.{js,vue}"],
      exclude: [
        "src/**/*.test.{js,ts}",
        "src/**/*.spec.{js,ts}",
        "src/**/__tests__/**",
        "src/mock/**",
        "src/main.js",
        "src/App.vue",
        "src/router/**",
        "src/assets/**",
        "src/styles/**",
        "scripts/**",
        "**/*.config.{js,ts}",
      ],
      all: true,
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
```

`all: true` 会把所有 src 文件纳入统计（包括完全没被测试 import 的，显示为 0%）。

---

## 后端：JaCoCo 根 pom 配置

在 `backend/pom.xml` 的 `<build>` 加入：

```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
        <executions>
          <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
            <configuration>
              <formats>
                <format>XML</format>
                <format>HTML</format>
              </formats>
            </configuration>
          </execution>
        </executions>
        <configuration>
          <excludes>
            <exclude>**/entity/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/vo/**</exclude>
            <exclude>**/*Application.class</exclude>
            <exclude>**/config/**</exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </pluginManagement>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

Excludes：data classes（entity/dto/vo）、Application 主类、config 类（通常无逻辑）。

---

## 运行

```bash
# 前端
cd web-admin && npm run test:coverage 2>&1 | tail -40
# 输出 coverage/index.html + coverage/lcov.info + coverage/coverage-summary.json

# 后端
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test
# 每个模块：target/site/jacoco/index.html + jacoco.xml
```

---

## CI 调整

`.github/workflows/ci.yml` 的 `build-backend` job 加一步：

```yaml
- name: Upload JaCoCo reports
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: backend-jacoco-reports
    path: backend/**/target/site/jacoco/
    retention-days: 14
```

前端 coverage artifact 已上传（无需修改）。

---

## docs/coverage-baseline.md 格式

```markdown
# Coverage Baseline — 2026-04-16

## Frontend (Vitest + v8)

| Metric     | %    |
| ---------- | ---- |
| Lines      | XX.X |
| Branches   | XX.X |
| Functions  | XX.X |
| Statements | XX.X |

### Hotspots (lowest coverage files)

1. `src/xxx/yyy.js` — X% lines
2. `src/zzz.vue` — Y% lines
   ...

## Backend (JaCoCo)

| Service         | Instruction % | Branch % |
| --------------- | ------------- | -------- |
| gateway-service | XX            | XX       |
| ...             |

### Hotspots

1. service XX / class YY — low coverage class list
   ...

## 说明

- 本基线仅作参考，不设 CI 阈值。
- hotspot 为后续补测的候选清单，非强制。
```

---

## 文件修改清单

| 文件                         | 变更                                      |
| ---------------------------- | ----------------------------------------- |
| `web-admin/vitest.config.js` | 加 coverage 配置                          |
| `backend/pom.xml`            | 加 jacoco-maven-plugin                    |
| `.github/workflows/ci.yml`   | build-backend job 加 jacoco artifact 上传 |
| `docs/coverage-baseline.md`  | 新建，记录基线                            |

---

## 执行策略

并行：

- **T1**：前端 vitest.config.js 加 coverage + 跑一次拿报告
- **T2**：后端 pom.xml 加 jacoco + 跑一次拿报告
- **T3**：CI ci.yml 加 jacoco artifact 上传

串行：

- **T4**：基于 T1+T2 报告数据，写 `docs/coverage-baseline.md`

T4 依赖 T1+T2 的数据输出。
