# Vortex MVP Sync Phase 2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the remaining MVP sync-flow backend and frontend capabilities after baseline setup (Task 1-3 already done).

**Architecture:** Build the run orchestration API and node execution interfaces in `back` on top of existing Maven + Spring Boot + MySQL/Flyway/MyBatis-Plus baseline. Then bootstrap `web` with a minimal Vite React app and add canvas/monitor MVP pages that call backend APIs. Keep scope strict to synchronous flow and manual trigger.

**Tech Stack:** Java 25, Spring Boot 4.0.3, Maven, MySQL, Flyway, MyBatis-Plus, JUnit 5, React, TypeScript, Vite, React Flow, Vitest.

---

## Preconditions

- Branch must not be `master`/`main`.
- Keep TDD loop for every task: RED -> GREEN -> COMMIT.
- Skills reference: `@test-driven-development` `@verification-before-completion` `@requesting-code-review`.

### Task 1: Manual Run API with FAIL_INIT

**Files:**
- Create: `back/src/main/java/com/vortex/run/api/RunController.java`
- Create: `back/src/main/java/com/vortex/run/service/RunService.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunStatus.java`
- Create: `back/src/main/java/com/vortex/run/domain/RunRecord.java`
- Test: `back/src/test/java/com/vortex/run/api/RunControllerTest.java`

**Step 1: Write the failing test**

```java
@WebMvcTest(RunController.class)
class RunControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean RunService runService;

  @Test
  void shouldReturn422WhenRunIdGenerationFails() throws Exception {
    when(runService.start(any())).thenThrow(new IllegalStateException("run_id generation failed"));

    mockMvc.perform(post("/api/runs")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"flowCode\":\"flow-1\",\"businessIds\":[\"A1\"]}"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.status").value("FAIL_INIT"));
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.run.api.RunControllerTest" test`
Expected: FAIL with missing controller/service classes.

**Step 3: Write minimal implementation**

```java
@PostMapping("/api/runs")
public ResponseEntity<?> start(@RequestBody StartRunRequest req) {
  try {
    return ResponseEntity.ok(runService.start(req));
  } catch (IllegalStateException ex) {
    return ResponseEntity.unprocessableEntity().body(Map.of(
      "status", "FAIL_INIT",
      "message", ex.getMessage()
    ));
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.run.api.RunControllerTest" test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/run back/src/test/java/com/vortex/run
git commit -m "feat(back): add manual run endpoint with fail-init response"
```

### Task 2: Node Executor Contract + DB Fetch Node (MyBatis-Plus)

**Files:**
- Create: `back/src/main/java/com/vortex/engine/NodeExecutor.java`
- Create: `back/src/main/java/com/vortex/engine/ExecutionContext.java`
- Create: `back/src/main/java/com/vortex/node/fetch/entity/BusinessRecordEntity.java`
- Create: `back/src/main/java/com/vortex/node/fetch/mapper/BusinessRecordMapper.java`
- Create: `back/src/main/java/com/vortex/node/fetch/DbFetchNodeExecutor.java`
- Modify: `back/src/main/resources/db/migration/mysql/V1__init_core_tables.sql`
- Test: `back/src/test/java/com/vortex/node/fetch/DbFetchNodeExecutorTest.java`

**Step 1: Write the failing test**

```java
class DbFetchNodeExecutorTest {
  @Test
  void shouldLoadRecordsByBusinessIds() {
    BusinessRecordMapper mapper = mock(BusinessRecordMapper.class);
    when(mapper.selectBatchIds(anyCollection()))
      .thenReturn(List.of(new BusinessRecordEntity(1L, "A1", "alpha")));

    var executor = new DbFetchNodeExecutor(mapper);
    var ctx = new ExecutionContext("run-1", List.of("A1"));

    executor.execute(ctx, Map.of());

    assertThat(ctx.records()).hasSize(1);
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest" test`
Expected: FAIL with missing classes.

**Step 3: Write minimal implementation**

```java
public void execute(ExecutionContext ctx, Map<String, Object> config) {
  var rows = mapper.selectBatchIds(ctx.businessIds());
  var mapped = rows.stream().map(BusinessRecordEntity::toMap).toList();
  ctx.setRecords(mapped);
}
```

```sql
CREATE TABLE IF NOT EXISTS business_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id VARCHAR(64) NOT NULL,
  payload VARCHAR(512) NOT NULL,
  UNIQUE KEY uk_business_id (business_id)
);
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest" test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/engine back/src/main/java/com/vortex/node/fetch back/src/main/resources/db/migration/mysql/V1__init_core_tables.sql back/src/test/java/com/vortex/node/fetch
git commit -m "feat(back): add node executor contract and db fetch node"
```

### Task 3: Trace Migration + Trace Query API

**Files:**
- Create: `back/src/main/resources/db/migration/mysql/V2__init_trace_tables.sql`
- Create: `back/src/main/java/com/vortex/trace/api/TraceController.java`
- Create: `back/src/main/java/com/vortex/trace/service/TraceService.java`
- Test: `back/src/test/java/com/vortex/trace/api/TraceControllerTest.java`

**Step 1: Write the failing test**

```java
@WebMvcTest(TraceController.class)
class TraceControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean TraceService traceService;

  @Test
  void shouldReturnTraceByBusinessId() throws Exception {
    when(traceService.getByBusinessId("A1")).thenReturn(Map.of("businessId", "A1", "steps", List.of()));

    mockMvc.perform(get("/api/traces/business/A1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.businessId").value("A1"));
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.trace.api.TraceControllerTest" test`
Expected: FAIL with missing controller/service.

**Step 3: Write minimal implementation**

```java
@GetMapping("/api/traces/business/{businessId}")
public Map<String, Object> byBusiness(@PathVariable String businessId) {
  return traceService.getByBusinessId(businessId);
}
```

```sql
CREATE TABLE IF NOT EXISTS trace_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(128) NOT NULL,
  business_id VARCHAR(64) NOT NULL,
  node_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  detail VARCHAR(1024) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_business_id (business_id)
);
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.trace.api.TraceControllerTest" test`
Expected: PASS.

**Step 5: Commit**

```bash
git add back/src/main/resources/db/migration/mysql/V2__init_trace_tables.sql back/src/main/java/com/vortex/trace back/src/test/java/com/vortex/trace
git commit -m "feat(back): add trace migration and trace query api"
```

### Task 4: Bootstrap Web Project and Health Widget

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
Expected: FAIL with missing app/test setup.

**Step 3: Write minimal implementation**

```tsx
useEffect(() => {
  fetch('/api/health').then(r => r.json()).then(d => setStatus(d.status))
}, [])
return <div>Backend: {status}</div>
```

**Step 4: Run test to verify it passes**

Run: `cd web; npm test -- --runInBand src/App.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web
git commit -m "feat(web): bootstrap app and health status widget"
```

### Task 5: Canvas + Run Monitor MVP Pages

**Files:**
- Create: `web/src/pages/FlowCanvasPage.tsx`
- Create: `web/src/pages/RunMonitorPage.tsx`
- Create: `web/src/components/NodeConfigPanel.tsx`
- Create: `web/src/components/NodeStatusBadge.tsx`
- Modify: `web/src/App.tsx`
- Test: `web/src/pages/FlowCanvasPage.test.tsx`
- Test: `web/src/pages/RunMonitorPage.test.tsx`

**Step 1: Write the failing tests**

```tsx
it('renders default flow node', () => {
  render(<FlowCanvasPage />)
  expect(screen.getByText('db-fetch')).toBeInTheDocument()
})
```

```tsx
it('renders run metrics from api', async () => {
  render(<RunMonitorPage />)
  expect(await screen.findByText(/in:\s*100/i)).toBeInTheDocument()
})
```

**Step 2: Run test to verify it fails**

Run: `cd web; npm test -- --runInBand src/pages/FlowCanvasPage.test.tsx src/pages/RunMonitorPage.test.tsx`
Expected: FAIL with missing pages/components.

**Step 3: Write minimal implementation**

```tsx
// FlowCanvasPage: render seeded nodes + status badges.
// RunMonitorPage: call /api/runs/{runId}/metrics and render in/out/fail counters.
```

**Step 4: Run test to verify it passes**

Run: `cd web; npm test -- --runInBand src/pages/FlowCanvasPage.test.tsx src/pages/RunMonitorPage.test.tsx`
Expected: PASS.

**Step 5: Commit**

```bash
git add web/src/pages web/src/components web/src/App.tsx
git commit -m "feat(web): add canvas and run monitor mvp pages"
```

## Final Verification Commands

1. `mvn -s .mvn-settings.xml -f back\pom.xml clean test`
2. `cd web; npm ci && npm test`
3. `git log --oneline -n 20`

## Out of Scope Guardrails

- No CDC trigger.
- No async query node.
- No distributed orchestrator/message bus.
- No one-flow-multi-platform support.
