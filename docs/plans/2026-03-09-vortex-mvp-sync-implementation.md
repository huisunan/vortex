# Vortex MVP Sync Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a runnable MVP that supports one-platform-per-flow synchronous execution, configurable `run_id` generation, batch push with extractor, and full traceability from Flow run to single record.

**Architecture:** Use a modular monolith backend (`back`) with plugin-style node executors and strong idempotency/trace persistence. Use a React frontend (`web`) with a lightweight visual canvas, node configuration forms, and run monitoring pages. Keep MVP scope strict: manual trigger + single DB source + sync path only.

**Tech Stack:** Java 25, Spring Boot, Maven, MySQL, Flyway, JUnit 5, Testcontainers, React, TypeScript, Vite, React Flow, TanStack Query, Vitest, Playwright.

---

## Preconditions

- 执行本计划前，建议在独立 worktree 中进行（避免与主线混改）。
- 统一使用 TDD：先红灯测试，再最小实现，再绿灯验证。
- 每个 Task 完成后立即小提交（frequent commits）。
- 相关技能：`@test-driven-development` `@verification-before-completion` `@requesting-code-review`

### Task 1: Bootstrap Backend Skeleton

**Files:**
- Create: `back/pom.xml`
- Create: `back/src/main/java/com/vortex/VortexApplication.java`
- Create: `back/src/main/java/com/vortex/config/VirtualThreadConfig.java`
- Create: `back/src/main/resources/application.yml`
- Test: `back/src/test/java/com/vortex/health/HealthControllerTest.java`

**Step 1: Write the failing test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {
  @Autowired MockMvc mockMvc;

  @Test
  void should_return_up() throws Exception {
    mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(content().json("{\"status\":\"UP\"}"));
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=HealthControllerTest test`
Expected: FAIL with "No mapping for GET /api/health".

**Step 3: Write minimal implementation**

```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true

management:
  endpoints:
    web:
      base-path: /api
      exposure:
        include: health
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=HealthControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/pom.xml back/src/main back/src/test
git commit -m "feat(back): bootstrap service with actuator health and virtual threads"
```

### Task 2: Add Flow Definition Model and One-Platform Constraint

**Files:**
- Create: `back/src/main/java/com/vortex/flow/domain/FlowDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/domain/NodeDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/domain/EdgeDefinition.java`
- Create: `back/src/main/java/com/vortex/flow/service/FlowDefinitionValidator.java`
- Test: `back/src/test/java/com/vortex/flow/service/FlowDefinitionValidatorTest.java`

**Step 1: Write the failing test**

```java
class FlowDefinitionValidatorTest {
  @Test
  void should_reject_flow_without_platform_code() {
    var flow = new FlowDefinition("flow-1", "", List.of(), List.of());
    assertThatThrownBy(() -> new FlowDefinitionValidator().validate(flow))
      .hasMessageContaining("platformCode is required");
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.flow.service.FlowDefinitionValidatorTest test`
Expected: FAIL with "cannot find symbol FlowDefinitionValidator".

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
git commit -m "feat(back): add flow definition model and platform constraint validator"
```

### Task 3: Implement `run_id` Generator Strategy (HTTP + UUID)

**Files:**
- Create: `back/src/main/java/com/vortex/runid/RunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/UuidRunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/HttpRunIdGenerator.java`
- Create: `back/src/main/java/com/vortex/runid/RunIdService.java`
- Test: `back/src/test/java/com/vortex/runid/RunIdServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_fail_fast_when_http_generator_fails() {
  var http = mock(RunIdGenerator.class);
  when(http.generate(any())).thenThrow(new RuntimeException("503"));

  var service = new RunIdService(http, new UuidRunIdGenerator());

  assertThatThrownBy(() -> service.generate(true, Map.of()))
    .hasMessageContaining("run_id generation failed");
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.runid.RunIdServiceTest test`
Expected: FAIL with missing classes.

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
git commit -m "feat(back): add run_id strategy with fail-fast semantics"
```

### Task 4: Add Manual Trigger API and Initialization Failure State

**Files:**
- Create: `back/src/main/java/com/vortex/run/api/RunController.java`
- Create: `back/src/main/java/com/vortex/run/service/RunService.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunRecord.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunStatus.java`
- Test: `back/src/test/java/com/vortex/run/api/RunControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_return_422_when_run_id_generation_fails() throws Exception {
  when(runService.start(any())).thenThrow(new IllegalStateException("run_id generation failed"));

  mockMvc.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
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
git commit -m "feat(back): add manual run API with FAIL_INIT handling"
```

### Task 5: Add Node Executor Contract + DB Fetch Node

**Files:**
- Create: `back/src/main/java/com/vortex/engine/NodeExecutor.java`
- Create: `back/src/main/java/com/vortex/engine/ExecutionContext.java`
- Create: `back/src/main/java/com/vortex/node/fetch/DbFetchNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/node/fetch/BusinessDataRepository.java`
- Test: `back/src/test/java/com/vortex/node/fetch/DbFetchNodeExecutorTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_load_records_by_business_ids() {
  var repo = ids -> List.of(Map.of("businessId", "A1", "name", "alpha"));
  var executor = new DbFetchNodeExecutor(repo);
  var ctx = new ExecutionContext("run-1", List.of("A1"));

  executor.execute(ctx, Map.of());

  assertThat(ctx.records()).hasSize(1);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest test`
Expected: FAIL with missing executor class.

**Step 3: Write minimal implementation**

```java
public void execute(ExecutionContext ctx, Map<String, Object> config) {
  var rows = repository.findByBusinessIds(ctx.businessIds());
  ctx.setRecords(rows);
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/engine back/src/main/java/com/vortex/node/fetch back/src/test/java/com/vortex/node/fetch
git commit -m "feat(back): add node executor contract and db fetch node"
```

### Task 6: Add Filter/Transform Node (Expression + Script)

**Files:**
- Modify: `back/pom.xml` (add JEXL + GraalJS deps)
- Create: `back/src/main/java/com/vortex/node/filter/FilterNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/node/transform/TransformNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/script/ScriptRuntime.java`
- Test: `back/src/test/java/com/vortex/node/filter/FilterNodeExecutorTest.java`
- Test: `back/src/test/java/com/vortex/node/transform/TransformNodeExecutorTest.java`

**Step 1: Write the failing tests**

```java
@Test
void should_filter_out_record_when_expression_false() {
  var ctx = ExecutionContext.withRecords(List.of(Map.of("price", 10), Map.of("price", 200)));
  new FilterNodeExecutor().execute(ctx, Map.of("expression", "price > 100"));
  assertThat(ctx.records()).hasSize(1);
}
```

```java
@Test
void should_apply_script_transform() {
  var ctx = ExecutionContext.withRecords(List.of(Map.of("status", 1)));
  new TransformNodeExecutor().execute(ctx, Map.of("script", "record.label = record.status == 1 ? '正常' : '禁用'; return record;"));
  assertThat(ctx.records().get(0).get("label")).isEqualTo("正常");
}
```

**Step 2: Run tests to verify they fail**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.filter.FilterNodeExecutorTest,com.vortex.node.transform.TransformNodeExecutorTest test`
Expected: FAIL with missing executors.

**Step 3: Write minimal implementation**

```java
// Filter: evaluate expression per record, keep true rows.
// Transform: invoke script runtime with variable `record` and return updated record.
```

**Step 4: Run tests to verify they pass**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.filter.FilterNodeExecutorTest,com.vortex.node.transform.TransformNodeExecutorTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/pom.xml back/src/main/java/com/vortex/node/filter back/src/main/java/com/vortex/node/transform back/src/main/java/com/vortex/script back/src/test/java/com/vortex/node/filter back/src/test/java/com/vortex/node/transform
git commit -m "feat(back): add expression filter and script transform nodes"
```

### Task 7: Add Batch Split, Push, Extractor, and Mapping Idempotency

**Files:**
- Create: `back/src/main/java/com/vortex/node/split/BatchSplitNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/node/push/PushNodeExecutor.java`
- Create: `back/src/main/java/com/vortex/node/extract/ResultExtractor.java`
- Create: `back/src/main/java/com/vortex/node/extract/JsonPathResultExtractor.java`
- Create: `back/src/main/java/com/vortex/node/extract/ScriptResultExtractor.java`
- Create: `back/src/main/java/com/vortex/mapping/MappingService.java`
- Test: `back/src/test/java/com/vortex/node/split/BatchSplitNodeExecutorTest.java`
- Test: `back/src/test/java/com/vortex/node/extract/JsonPathResultExtractorTest.java`
- Test: `back/src/test/java/com/vortex/mapping/MappingServiceTest.java`

**Step 1: Write the failing tests**

```java
@Test
void should_split_records_by_platform_limit() {
  var batches = new BatchSplitNodeExecutor().split(List.of(1,2,3,4,5), 2);
  assertThat(batches).hasSize(3);
}
```

```java
@Test
void should_extract_platform_id_with_jsonpath() {
  var response = "{\"data\":{\"platformId\":\"P-100\"}}";
  var value = new JsonPathResultExtractor().extract(response, "$.data.platformId");
  assertThat(value).isEqualTo("P-100");
}
```

```java
@Test
void should_ignore_duplicate_success_for_same_business_and_platform() {
  mappingService.saveSuccess("A1", "PLAT_A", "P-1");
  mappingService.saveSuccess("A1", "PLAT_A", "P-2");
  assertThat(mappingService.find("A1", "PLAT_A").platformId()).isEqualTo("P-1");
}
```

**Step 2: Run tests to verify they fail**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.split.BatchSplitNodeExecutorTest,com.vortex.node.extract.JsonPathResultExtractorTest,com.vortex.mapping.MappingServiceTest test`
Expected: FAIL with missing classes.

**Step 3: Write minimal implementation**

```java
// Split: partition list by maxBatchSize.
// Extract: JSONPath/script returns status/platformId/error.
// Mapping: enforce unique success by (businessId, platformCode).
```

**Step 4: Run tests to verify they pass**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.node.split.BatchSplitNodeExecutorTest,com.vortex.node.extract.JsonPathResultExtractorTest,com.vortex.mapping.MappingServiceTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/node/split back/src/main/java/com/vortex/node/push back/src/main/java/com/vortex/node/extract back/src/main/java/com/vortex/mapping back/src/test/java/com/vortex/node/split back/src/test/java/com/vortex/node/extract back/src/test/java/com/vortex/mapping
git commit -m "feat(back): add batch split, push extractor, and idempotent mapping"
```

### Task 8: Add Run/Batch/Node Trace Persistence and Query APIs

**Files:**
- Create: `back/src/main/resources/db/migration/V1__init_core_tables.sql`
- Create: `back/src/main/resources/db/migration/V2__init_trace_tables.sql`
- Create: `back/src/main/java/com/vortex/trace/TraceService.java`
- Create: `back/src/main/java/com/vortex/trace/api/TraceController.java`
- Test: `back/src/test/java/com/vortex/trace/api/TraceControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_return_trace_by_business_id() throws Exception {
  mockMvc.perform(get("/api/traces/business/A1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.businessId").value("A1"))
    .andExpect(jsonPath("$.steps").isArray());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.trace.api.TraceControllerTest test`
Expected: FAIL with 404.

**Step 3: Write minimal implementation**

```java
@GetMapping("/api/traces/business/{businessId}")
TraceResponse byBusiness(@PathVariable String businessId) {
  return traceService.getByBusinessId(businessId);
}
```

**Step 4: Run tests to verify they pass**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.trace.api.TraceControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/resources/db/migration back/src/main/java/com/vortex/trace back/src/test/java/com/vortex/trace
git commit -m "feat(back): add trace persistence schema and query api"
```

### Task 9: Bootstrap Frontend and Health Wiring

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
  server.use(http.get('/api/health', () => HttpResponse.json({ status: 'UP' })));
  render(<App />);
  expect(await screen.findByText('Backend: UP')).toBeInTheDocument();
});
```

**Step 2: Run test to verify it fails**

Run: `cd web; npm test -- --runInBand App.test.tsx`
Expected: FAIL with missing App/client.

**Step 3: Write minimal implementation**

```tsx
useEffect(() => { fetch('/api/health').then(r => r.json()).then(d => setStatus(d.status)); }, []);
return <div>Backend: {status}</div>;
```

**Step 4: Run test to verify it passes**

Run: `cd web; npm test -- --runInBand App.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web/package.json web/vite.config.ts web/tsconfig.json web/src
git commit -m "feat(web): bootstrap react app and backend health wiring"
```

### Task 10: Build Canvas, Node Config, and Run Monitor MVP UI

**Files:**
- Create: `web/src/pages/FlowCanvasPage.tsx`
- Create: `web/src/pages/RunMonitorPage.tsx`
- Create: `web/src/components/NodeConfigPanel.tsx`
- Create: `web/src/components/NodeStatusBadge.tsx`
- Create: `web/src/store/flowStore.ts`
- Create: `web/src/api/runApi.ts`
- Modify: `web/src/App.tsx`
- Test: `web/src/pages/FlowCanvasPage.test.tsx`
- Test: `web/src/pages/RunMonitorPage.test.tsx`

**Step 1: Write the failing tests**

```tsx
it('renders flow nodes with status colors', () => {
  render(<FlowCanvasPage />);
  expect(screen.getByText('db-fetch')).toHaveAttribute('data-status', 'idle');
});
```

```tsx
it('renders run metrics from api', async () => {
  render(<RunMonitorPage />);
  expect(await screen.findByText(/in: 100/i)).toBeInTheDocument();
});
```

**Step 2: Run tests to verify they fail**

Run: `cd web; npm test -- --runInBand FlowCanvasPage.test.tsx RunMonitorPage.test.tsx`
Expected: FAIL with missing components.

**Step 3: Write minimal implementation**

```tsx
// FlowCanvasPage: render React Flow with seeded nodes and status badge.
// NodeConfigPanel: form for platformCode, batchSize, retryN, extractor mode.
// RunMonitorPage: poll /api/runs/{runId}/metrics and render in/out/fail.
```

**Step 4: Run tests to verify they pass**

Run: `cd web; npm test -- --runInBand FlowCanvasPage.test.tsx RunMonitorPage.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web/src/pages web/src/components web/src/store web/src/api web/src/App.tsx
git commit -m "feat(web): add canvas config panel and run monitor pages"
```

### Task 11: End-to-End Sync Flow Test and Release Checklist

**Files:**
- Create: `back/src/test/java/com/vortex/e2e/SyncFlowE2ETest.java`
- Create: `web/e2e/sync-flow.spec.ts`
- Create: `docs/plans/verification-checklist.md`
- Modify: `AGENTS.md`

**Step 1: Write the failing end-to-end test**

```java
@Test
void should_execute_sync_flow_from_manual_trigger_to_mapping() {
  // start run -> execute nodes -> assert mapping saved and trace queryable
}
```

```ts
test('operator can start run and inspect node metrics', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Run Flow' }).click();
  await expect(page.getByText(/SUCCESS/)).toBeVisible();
});
```

**Step 2: Run tests to verify they fail**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml -Dtest=com.vortex.e2e.SyncFlowE2ETest test`
Expected: FAIL.

Run: `cd web; npx playwright test e2e/sync-flow.spec.ts`
Expected: FAIL.

**Step 3: Write minimal implementation to pass**

```text
Implement missing API wiring, test fixtures, and UI run action until both tests pass.
Keep scope MVP-only (no CDC/no async polling).
```

**Step 4: Run full verification**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml test`
Expected: PASS.

Run: `cd web; npm test && npx playwright test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/test/java/com/vortex/e2e web/e2e docs/plans/verification-checklist.md AGENTS.md
git commit -m "test: add e2e sync flow verification and release checklist"
```

## Final Verification Commands (Before PR)

1. `mvn -s .mvn-settings.xml -f back\pom.xml clean test`
2. `cd web; npm ci && npm test`
3. `cd web; npx playwright install --with-deps && npx playwright test`
4. `git log --oneline -n 15` (确认提交粒度与顺序)

## Out of Scope Guardrails

- 不实现 CDC 触发。
- 不实现异步查询节点。
- 不实现多目标平台单 Flow。
- 不引入复杂分布式编排（保持模块化单体）。



