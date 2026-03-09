# Vortex MVP Sync Phase 2 Backend (Lombok Baseline) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成 MVP 同步链路后端剩余能力：手动运行启动、节点执行器（含 DB 拉取）、追溯查询 API，并全部落到当前 `Spring Boot 4 + Maven + MySQL/Flyway + MyBatis-Plus + Lombok` 基线。

**Architecture:** 以 `RunController -> RunService -> RunIdService` 建立最小可运行入口，默认走 UUID 生成 run_id，失败时返回 `FAIL_INIT`。新增通用节点执行接口 `NodeExecutor` 与 `ExecutionContext`，先实现一个 `DbFetchNodeExecutor`（单表读取）验证执行模型。追溯能力采用 Flyway 新增迁移脚本 + MyBatis-Plus 查询 + REST 查询接口，保持“先测试后实现”的最小增量。

**Tech Stack:** Java 25, Spring Boot 4.0.3, Maven, Lombok, JUnit 5, Mockito, MyBatis-Plus, Flyway, MySQL/H2(test profile)。

---

## Preconditions

- 在独立特性分支执行（当前可沿用 `feat/mvp-sync`）。
- 每个任务严格按 `RED -> GREEN -> COMMIT` 执行。
- 必须遵守 `AGENTS.md`：Java DTO/实体使用普通 `class` + Lombok，禁止 `record`。
- 技能引用：`@test-driven-development` `@verification-before-completion` `@requesting-code-review`。

### Task 1: 完成 Run 启动链路（默认 UUID，失败返回 FAIL_INIT）

**Files:**
- Create: `back/src/main/java/com/vortex/runid/RunIdConfig.java`
- Modify: `back/src/main/java/com/vortex/run/service/RunService.java`
- Modify: `back/src/main/java/com/vortex/run/api/RunController.java`
- Modify: `back/src/main/java/com/vortex/run/api/StartRunRequest.java`
- Modify: `back/src/test/java/com/vortex/run/api/RunControllerTest.java`
- Create: `back/src/test/java/com/vortex/run/service/RunServiceTest.java`

**Step 1: Write the failing test**

```java
class RunServiceTest {

    @Test
    void shouldCreateRunningRecordWithUuidGeneratorByDefault() {
        RunIdService runIdService = mock(RunIdService.class);
        when(runIdService.generate(eq(false), anyMap())).thenReturn("run-001");
        RunService service = new RunService(runIdService);

        StartRunRequest request = new StartRunRequest("flow-1", List.of("A1"));
        RunRecord record = service.start(request);

        assertThat(record.getRunId()).isEqualTo("run-001");
        assertThat(record.getStatus()).isEqualTo(RunStatus.RUNNING);
        assertThat(record.getMessage()).isEqualTo("accepted");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.run.service.RunServiceTest" test`  
Expected: FAIL（`RunService` 构造器/实现与测试预期不一致）。

**Step 3: Write minimal implementation**

```java
// RunIdConfig.java
@Configuration
public class RunIdConfig {
    @Bean("httpRunIdGenerator")
    public RunIdGenerator httpRunIdGenerator() { return new HttpRunIdGenerator(); }
    @Bean("uuidRunIdGenerator")
    public RunIdGenerator uuidRunIdGenerator() { return new UuidRunIdGenerator(); }
    @Bean
    public RunIdService runIdService(
            @Qualifier("httpRunIdGenerator") RunIdGenerator http,
            @Qualifier("uuidRunIdGenerator") RunIdGenerator uuid) {
        return new RunIdService(http, uuid);
    }
}
```

```java
// RunService.java
@Service
public class RunService {
    private final RunIdService runIdService;
    public RunService(RunIdService runIdService) { this.runIdService = runIdService; }

    public RunRecord start(StartRunRequest request) {
        String runId = runIdService.generate(false, Map.of(
                "flowCode", request.getFlowCode(),
                "businessIds", request.getBusinessIds()
        ));
        return new RunRecord(runId, RunStatus.RUNNING, "accepted");
    }
}
```

```java
// RunControllerTest.java
@SpringBootTest
@ActiveProfiles("test")
class RunControllerTest { ... }
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.run.service.RunServiceTest,com.vortex.run.api.RunControllerTest" test`  
Expected: PASS（`RunServiceTest` 与 `RunControllerTest` 都通过）。

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/runid/RunIdConfig.java back/src/main/java/com/vortex/run/service/RunService.java back/src/main/java/com/vortex/run/api/RunController.java back/src/main/java/com/vortex/run/api/StartRunRequest.java back/src/test/java/com/vortex/run/service/RunServiceTest.java back/src/test/java/com/vortex/run/api/RunControllerTest.java
git commit -m "feat(back): wire run id strategy and implement run start service"
```

### Task 2: 建立节点执行器契约并实现 DB Fetch 节点

**Files:**
- Create: `back/src/main/java/com/vortex/engine/NodeExecutor.java`
- Create: `back/src/main/java/com/vortex/engine/ExecutionContext.java`
- Create: `back/src/main/java/com/vortex/node/fetch/entity/BusinessRecordEntity.java`
- Create: `back/src/main/java/com/vortex/node/fetch/mapper/BusinessRecordMapper.java`
- Create: `back/src/main/java/com/vortex/node/fetch/DbFetchNodeExecutor.java`
- Create: `back/src/main/resources/db/migration/mysql/V2__add_business_record_table.sql`
- Create: `back/src/test/java/com/vortex/node/fetch/DbFetchNodeExecutorTest.java`

**Step 1: Write the failing test**

```java
class DbFetchNodeExecutorTest {

    @Test
    void shouldLoadRecordsByBusinessIdsIntoContext() {
        BusinessRecordMapper mapper = mock(BusinessRecordMapper.class);
        BusinessRecordEntity row = new BusinessRecordEntity(1L, "A1", "{\"name\":\"alpha\"}");
        when(mapper.selectByBusinessIds(List.of("A1"))).thenReturn(List.of(row));

        DbFetchNodeExecutor executor = new DbFetchNodeExecutor(mapper);
        ExecutionContext context = new ExecutionContext("run-1", List.of("A1"));

        executor.execute(context, Map.of());

        assertThat(context.getRecords()).hasSize(1);
        assertThat(context.getRecords().get(0).get("businessId")).isEqualTo("A1");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest" test`  
Expected: FAIL（缺少 engine/node 类与 mapper）。

**Step 3: Write minimal implementation**

```java
// NodeExecutor.java
public interface NodeExecutor {
    String nodeType();
    void execute(ExecutionContext context, Map<String, Object> config);
}
```

```java
// DbFetchNodeExecutor.java
@Component
public class DbFetchNodeExecutor implements NodeExecutor {
    private final BusinessRecordMapper mapper;
    public DbFetchNodeExecutor(BusinessRecordMapper mapper) { this.mapper = mapper; }

    @Override
    public String nodeType() { return "db-fetch"; }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        List<BusinessRecordEntity> rows = mapper.selectByBusinessIds(context.getBusinessIds());
        List<Map<String, Object>> mapped = rows.stream()
                .map(row -> Map.of("businessId", row.getBusinessId(), "payload", row.getPayload()))
                .toList();
        context.setRecords(mapped);
    }
}
```

```sql
CREATE TABLE IF NOT EXISTS business_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id VARCHAR(64) NOT NULL,
  payload VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_business_record_business_id (business_id)
);
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.node.fetch.DbFetchNodeExecutorTest" test`  
Expected: PASS。

**Step 5: Commit**

```bash
git add back/src/main/java/com/vortex/engine back/src/main/java/com/vortex/node/fetch back/src/main/resources/db/migration/mysql/V2__add_business_record_table.sql back/src/test/java/com/vortex/node/fetch/DbFetchNodeExecutorTest.java
git commit -m "feat(back): add node executor contract and db fetch executor"
```

### Task 3: 追溯表迁移 + Trace 查询 API

**Files:**
- Create: `back/src/main/resources/db/migration/mysql/V3__add_trace_record_table.sql`
- Create: `back/src/main/java/com/vortex/trace/entity/TraceRecordEntity.java`
- Create: `back/src/main/java/com/vortex/trace/mapper/TraceRecordMapper.java`
- Create: `back/src/main/java/com/vortex/trace/service/TraceService.java`
- Create: `back/src/main/java/com/vortex/trace/api/TraceController.java`
- Create: `back/src/test/java/com/vortex/trace/service/TraceServiceTest.java`
- Create: `back/src/test/java/com/vortex/trace/api/TraceControllerTest.java`

**Step 1: Write the failing test**

```java
class TraceServiceTest {
    @Test
    void shouldReturnTraceRecordsByBusinessId() {
        TraceRecordMapper mapper = mock(TraceRecordMapper.class);
        TraceRecordEntity row = new TraceRecordEntity(1L, "run-1", "A1", "db-fetch", "SUCCESS", "ok");
        when(mapper.selectByBusinessId("A1")).thenReturn(List.of(row));

        TraceService service = new TraceService(mapper);
        List<TraceRecordEntity> result = service.getByBusinessId("A1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessId()).isEqualTo("A1");
    }
}
```

```java
@SpringBootTest
@ActiveProfiles("test")
class TraceControllerTest {
    @Autowired WebApplicationContext context;

    @Test
    void shouldReturnOkForTraceQuery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(get("/api/traces/business/A1"))
                .andExpect(status().isOk());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.trace.service.TraceServiceTest,com.vortex.trace.api.TraceControllerTest" test`  
Expected: FAIL（缺少 trace 相关类与接口）。

**Step 3: Write minimal implementation**

```java
// TraceController.java
@RestController
public class TraceController {
    private final TraceService traceService;
    public TraceController(TraceService traceService) { this.traceService = traceService; }

    @GetMapping("/api/traces/business/{businessId}")
    public List<TraceRecordEntity> byBusiness(@PathVariable String businessId) {
        return traceService.getByBusinessId(businessId);
    }
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
  KEY idx_trace_business_id (business_id)
);
```

**Step 4: Run test to verify it passes**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.trace.service.TraceServiceTest,com.vortex.trace.api.TraceControllerTest" test`  
Expected: PASS。

**Step 5: Commit**

```bash
git add back/src/main/resources/db/migration/mysql/V3__add_trace_record_table.sql back/src/main/java/com/vortex/trace back/src/test/java/com/vortex/trace
git commit -m "feat(back): add trace table and trace query api"
```

### Task 4: 全量回归验证与文档对齐

**Files:**
- Modify: `docs/plans/2026-03-09-vortex-mvp-sync-phase2-backend-lombok-implementation.md`（勾选完成状态）
- Modify: `AGENTS.md`（仅当新增工程约束时）

**Step 1: Run backend full tests**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml clean test`  
Expected: PASS（全部测试通过）。

**Step 2: Sanity check actuator and APIs**

Run: `mvn -s .mvn-settings.xml -f back\pom.xml spring-boot:run`  
Expected: `/api/health` 返回 `{"status":"UP"}`，`/api/info` 返回 404，`POST /api/runs` 与 `GET /api/traces/business/{id}` 可访问。

**Step 3: Update plan execution notes**

```markdown
- [x] Task 1 completed
- [x] Task 2 completed
- [x] Task 3 completed
- [x] Task 4 verification completed
```

**Step 4: Request code review**

Run: `git status && git log --oneline -n 10`  
Expected: 工作区干净（或仅保留待评审文档变更），最近提交与任务一一对应。

**Step 5: Commit (if docs changed)**

```bash
git add docs/plans/2026-03-09-vortex-mvp-sync-phase2-backend-lombok-implementation.md AGENTS.md
git commit -m "docs: update phase2 backend execution status"
```

## Final Verification Commands

1. `mvn -s .mvn-settings.xml -f back\pom.xml clean test`
2. `mvn -s .mvn-settings.xml -f back\pom.xml "-Dtest=com.vortex.run.api.RunControllerTest,com.vortex.run.service.RunServiceTest,com.vortex.node.fetch.DbFetchNodeExecutorTest,com.vortex.trace.api.TraceControllerTest" test`
3. `git log --oneline -n 20`

## Out of Scope Guardrails

- 本计划不实现 CDC 触发。
- 本计划不实现异步状态轮询节点。
- 本计划不实现一条 Flow 推送多个平台。
- 本计划不引入消息队列或分布式调度器。

## Execution Status (2026-03-09)

- [x] Task 1 completed
- [x] Task 2 completed
- [x] Task 3 completed
- [x] Task 4 verification completed
