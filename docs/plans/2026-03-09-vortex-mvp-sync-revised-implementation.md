# Vortex MVP Sync (Revised Baseline) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the remaining MVP sync-flow capabilities on top of the current backend baseline, using MySQL + Flyway + MyBatis-Plus, while keeping one-platform-per-flow and full traceability constraints.

**Architecture:** Keep backend as a modular monolith in `back` with clear module boundaries (definition, execution, nodes, trace). Use Flyway for schema evolution and MyBatis-Plus for single-table CRUD. Use Actuator health endpoint (`/api/health`) and YAML configuration as immutable baseline. Build frontend incrementally in `web` for canvas + run monitoring.

**Tech Stack:** Java 25, Spring Boot 4.0.3, Maven, MySQL, Flyway, MyBatis-Plus, JUnit 5, React, TypeScript, Vite, React Flow, Vitest, Playwright.

---

## Preconditions

- 在独立分支/工作区执行（当前分支建议：`feat/mvp-sync`）。
- 统一 TDD：先写失败测试，再最小实现，再验证通过。
- 每个任务完成后立即提交。
- 相关技能：`@test-driven-development` `@verification-before-completion` `@requesting-code-review`

### Task 1: Add MySQL + Flyway + MyBatis-Plus Baseline

**Files:**
- Modify: `back/pom.xml`
- Modify: `back/src/main/resources/application.yml`
- Create: `back/src/main/resources/db/migration/V1__init_core_tables.sql`
- Test: `back/src/test/java/com/vortex/bootstrap/PersistenceBootstrapTest.java`

**Step 1: Write the failing test**

```java
@SpringBootTest
class PersistenceBootstrapTest {

  @Autowired
  private Flyway flyway;

  @Test
  void shouldLoadFlywayBean() {
    assertThat(flyway).isNotNull();
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=PersistenceBootstrapTest test`
Expected: FAIL with missing Flyway bean/dependency.

**Step 3: Write minimal implementation**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vortex
    username: vortex
    password: vortex
  flyway:
    enabled: true
```

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
<dependency>
  <groupId>com.baomidou</groupId>
  <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
  <version>3.5.14</version>
</dependency>
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=PersistenceBootstrapTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/pom.xml back/src/main/resources/application.yml back/src/main/resources/db/migration/V1__init_core_tables.sql back/src/test/java/com/vortex/bootstrap/PersistenceBootstrapTest.java
git commit -m "feat(back): add mysql flyway and mybatis-plus baseline"
```

### Task 2: Add Flow Definition Domain and Validator

**Files:**
- Create: `back/src/main/java/com/vortex/flow/domain/FlowDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/domain/NodeDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/domain/EdgeDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/service/FlowDefinitionValidator.java`
- Test: `back/src/test/java/com/vortex/flow/service/FlowDefinitionValidatorTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldRejectFlowWithoutPlatformCode() {
  var flow = new FlowDefinition("flow-1", "", List.of(), List.of());
  assertThatThrownBy(() -> new FlowDefinitionValidator().validate(flow))
      .hasMessageContaining("platformCode is required");
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.flow.service.FlowDefinitionValidatorTest test`
Expected: FAIL with class not found.

**Step 3: Write minimal implementation**

```java
public void validate(FlowDefinition flow) {
  if (flow.platformCode() == null || flow.platformCode().isBlank()) {
    throw new IllegalArgumentException("platformCode is required");
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.flow.service.FlowDefinitionValidatorTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/flow back/src/test/java/com/vortex/flow
git commit -m "feat(back): add flow definition model and validator"
```

### Task 3: Implement Run ID Strategy (HTTP + UUID)

**Files:**
- Create: `back/src/main/java/com/vortex/runid/RunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/UuidRunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/HttpRunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/RunIdService.java`
- Test: `back/src/test/java/com/vortex/runid/RunIdServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldFailFastWhenHttpGeneratorFails() {
  var http = mock(RunIdGenerator.class);
  when(http.generate(any())).thenThrow(new RuntimeException("503"));

  var service = new RunIdService(http, new UuidRunIdGenerator());

  assertThatThrownBy(() -> service.generate(true, Map.of()))
      .hasMessageContaining("run_id generation failed");
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.runid.RunIdServiceTest test`
Expected: FAIL with class not found.

**Step 3: Write minimal implementation**

```java
public String generate(boolean useHttpGenerator, Map<String, Object> input) {
  try {
    return useHttpGenerator ? httpGenerator.generate(input) : uuidGenerator.generate(input);
  } catch (Exception ex) {
    throw new IllegalStateException("run_id generation failed", ex);
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.runid.RunIdServiceTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/runid back/src/test/java/com/vortex/runid
git commit -m "feat(back): add run-id strategy and fail-fast policy"
```

### Task 4: Add Manual Run API with FAIL_INIT Response

**Files:**
- Create: `back/src/main/java/com/vortex/run/api/RunController.java`
- Create: `back/src/main/java/com/vortex/run/service/RunService.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunStatus.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunRecord.java`
- Test: `back/src/test/java/com/vortex/run/api/RunControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldReturn422WhenRunIdGenerationFails() throws Exception {
  when(runService.start(any())).thenThrow(new IllegalStateException("run_id generation failed"));

  mockMvc.perform(post("/api/runs")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"flowId\":\"flow-1\",\"businessIds\":[\"A1\"]}"))
    .andExpect(status().isUnprocessableEntity())
    .andExpect(jsonPath("$.status").value("FAIL_INIT"));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.run.api.RunControllerTest test`
Expected: FAIL with missing endpoint.

**Step 3: Write minimal implementation**

```java
@PostMapping("/api/runs")
ResponseEntity<?> start(@RequestBody StartRunRequest req) {
  try {
    return ResponseEntity.ok(runService.start(req));
  } catch (IllegalStateException ex) {
    return ResponseEntity.unprocessableEntity()
      .body(Map.of("status", "FAIL_INIT", "message", ex.getMessage()));
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.run.api.RunControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/run back/src/test/java/com/vortex/run
git commit -m "feat(back): add manual run endpoint with fail-init handling"
```

### Task 5: Add Node Executor Contract + DB Fetch Node with MyBatis-Plus

**Files:**
- Create: `back/src/main/java/com/vortex/engine/NodeExecutor.java`
- Create: `back/src/main/java/com/vortex/engine/ExecutionContext.java`
- Create: `back/src/main/java/com/vortex/node/fetch/DbFetchNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/node/fetch/entity/BusinessRecordEntity.java`
- Create: `back/src/main/java/com/vortex/node/fetch/mapper/BusinessRecordMapper.java`
- Test: `back/src/test/java/com/vortex/node/fetch/DbFetchNodeExecutorTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldLoadRecordsByBusinessIds() {
  var mapper = mock(BusinessRecordMapper.class);
  when(mapper.selectBatchIds(anyCollection())).thenReturn(List.of(new BusinessRecordEntity("A1", "alpha")));

  var executor = new DbFetchNodeExecutor(mapper);
  var ctx = new ExecutionContext("run-1", List.of("A1"));

  executor.execute(ctx, Map.of());

  assertThat(ctx.records()).hasSize(1);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest test`
Expected: FAIL with class not found.

**Step 3: Write minimal implementation**

```java
public void execute(ExecutionContext ctx, Map<String, Object> config) {
  var rows = mapper.selectBatchIds(ctx.businessIds());
  ctx.setRecords(rows.stream().map(BusinessRecordEntity::toMap).toList());
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/engine back/src/main/java/com/vortex/node/fetch back/src/test/java/com/vortex/node/fetch
git commit -m "feat(back): add executor contract and db-fetch node with mybatis-plus"
```

### Task 6: Add Trace Schema + Query API

**Files:**
- Create: `back/src/main/resources/db/migration/V2__init_trace_tables.sql`
- Create: `back/src/main/java/com/vortex/trace/api/TraceController.java`
- Create: `back/src/main/java/com/vortex/trace/service/TraceService.java`
- Test: `back/src/test/java/com/vortex/trace/api/TraceControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldReturnTraceByBusinessId() throws Exception {
  mockMvc.perform(get("/api/traces/business/A1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.businessId").value("A1"));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.trace.api.TraceControllerTest test`
Expected: FAIL with 404.

**Step 3: Write minimal implementation**

```java
@GetMapping("/api/traces/business/{businessId}")
public TraceResponse byBusiness(@PathVariable String businessId) {
  return traceService.getByBusinessId(businessId);
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.trace.api.TraceControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/resources/db/migration/V2__init_trace_tables.sql back/src/main/java/com/vortex/trace back/src/test/java/com/vortex/trace
git commit -m "feat(back): add trace schema and business trace query api"
```

### Task 7: Bootstrap Frontend App and Health Widget

**Files:**
- Create: `web/package.json`
- Create: `web/vite.config.ts`
- Create: `web/tsconfig.json`
- Create: `web/src/main.tsx`
- Create: `web/src/App.tsx`
- Create: `web/src/api/client.ts`
- Test: `web/src/App.test.tsx`

**Step 1: Write the failing test**

```tsx
it('shows backend health status', async () => {
  render(<App />)
  expect(await screen.findByText('Backend: UP')).toBeInTheDocument()
})
```

**Step 2: Run test to verify it fails**

Run: `cd web; npm test -- --runInBand src/App.test.tsx`
Expected: FAIL with missing app/test runtime.

**Step 3: Write minimal implementation**

```tsx
useEffect(() => {
  fetch('/api/health').then(r => r.json()).then(d => setStatus(d.status))
}, [])
```

**Step 4: Run test to verify it passes**

Run: `cd web; npm test -- --runInBand src/App.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web
git commit -m "feat(web): bootstrap vite app and health widget"
```

### Task 8: Add Canvas + Monitor Pages (MVP UI)

**Files:**
- Create: `web/src/pages/FlowCanvasPage.tsx`
- Create: `web/src/pages/RunMonitorPage.tsx`
- Create: `web/src/components/NodeStatusBadge.tsx`
- Create: `web/src/components/NodeConfigPanel.tsx`
- Modify: `web/src/App.tsx`
- Test: `web/src/pages/FlowCanvasPage.test.tsx`
- Test: `web/src/pages/RunMonitorPage.test.tsx`

**Step 1: Write the failing tests**

```tsx
it('renders flow nodes with status', () => {
  render(<FlowCanvasPage />)
  expect(screen.getByText('db-fetch')).toBeInTheDocument()
})
```

```tsx
it('renders run metrics', async () => {
  render(<RunMonitorPage />)
  expect(await screen.findByText(/in:\s*100/i)).toBeInTheDocument()
})
```

**Step 2: Run test to verify it fails**

Run: `cd web; npm test -- --runInBand src/pages/FlowCanvasPage.test.tsx src/pages/RunMonitorPage.test.tsx`
Expected: FAIL with missing pages/components.

**Step 3: Write minimal implementation**

```tsx
// Canvas page renders seeded nodes + status badge.
// Monitor page polls run metrics and renders in/out/fail numbers.
```

**Step 4: Run test to verify it passes**

Run: `cd web; npm test -- --runInBand src/pages/FlowCanvasPage.test.tsx src/pages/RunMonitorPage.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web/src/pages web/src/components web/src/App.tsx
git commit -m "feat(web): add mvp canvas and run monitor pages"
```

## Final Verification Commands (Before PR)

1. `mvn -s .mvn-settings.xml -f back\pom.xml clean test`
2. `cd web; npm ci && npm test`
3. `cd web; npx playwright install --with-deps && npx playwright test`
4. `git log --oneline -n 20`

## Out of Scope Guardrails

- 不实现 CDC 触发。
- 不实现异步查询节点。
- 不实现单 Flow 多目标平台。
- 不引入分布式编排或消息总线。
