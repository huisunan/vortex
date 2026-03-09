# Flow 执行引擎实现总结

## 已完成功能

### 1. FlowExecutionEngine - 流程执行引擎
**位置**: `src/main/java/com/vortex/engine/FlowExecutionEngine.java`

功能：
- ✅ 加载 FlowDefinition
- ✅ 拓扑排序节点（Kahn 算法）
- ✅ 检测循环依赖
- ✅ 调用 NodeExecutor 执行每个节点
- ✅ 传递 ExecutionContext
- ✅ 支持并行节点执行

### 2. ExecutionTracer - 执行追踪器
**位置**: `src/main/java/com/vortex/engine/ExecutionTracer.java`

功能：
- ✅ 记录节点执行开始（RUNNING 状态）
- ✅ 记录节点执行成功（SUCCESS 状态）
- ✅ 记录节点执行失败（FAILED 状态）
- ✅ 持久化到 trace_record 表

### 3. RunService 集成
**位置**: `src/main/java/com/vortex/run/service/RunService.java`

功能：
- ✅ 加载 FlowDefinition
- ✅ 创建 ExecutionContext
- ✅ 触发 FlowExecutionEngine 执行
- ✅ 返回执行结果（SUCCESS/FAIL）

### 4. FlowDefinitionService
**位置**: `src/main/java/com/vortex/flow/service/FlowDefinitionService.java`

功能：
- ✅ 加载 FlowDefinition（当前使用内存缓存）
- ✅ TODO: 后续从数据库加载

### 5. FilterNodeExecutor - Spring EL 迁移
**位置**: `src/main/java/com/vortex/node/filter/FilterNodeExecutor.java`

功能：
- ✅ 从 JEXL 迁移到 Spring EL (SpEL)
- ✅ 支持表达式：`#record['fieldName'] > 100`
- ✅ 支持逻辑运算符：`&&`, `||`, `!`
- ✅ 支持比较运算符：`==`, `!=`, `>`, `>=`, `<`, `<=`

## 测试覆盖

### FlowExecutionEngineTest
- ✅ 简单线性流程执行
- ✅ 拓扑排序验证
- ✅ 单节点流程
- ✅ 循环依赖检测
- ✅ 并行节点执行

### RunServiceTest
- ✅ 成功执行流程
- ✅ 执行失败处理

### FilterNodeExecutorTest
- ✅ 表达式过滤
- ✅ AND/OR 逻辑
- ✅ 表达式验证

## 使用示例

### 定义流程
```java
FlowDefinition flow = new FlowDefinition(
    "sample-flow",
    "default",
    Arrays.asList(
        new NodeDefinition("fetch", "fetch", Map.of("query", "SELECT * FROM business")),
        new NodeDefinition("filter", "filter", Map.of("expression", "#record['status'] == 1")),
        new NodeDefinition("transform", "transform", Map.of("script", "record.processed = true; return record;"))
    ),
    Arrays.asList(
        new EdgeDefinition("fetch", "filter"),
        new EdgeDefinition("filter", "transform")
    )
);
```

### 执行流程
```java
// 通过 RunService 触发
StartRunRequest request = new StartRunRequest("sample-flow", Arrays.asList("biz-1", "biz-2"));
RunRecord result = runService.start(request);

// 或直接使用 FlowExecutionEngine
ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1", "biz-2"));
context.setRecords(...); // 设置初始数据
flowExecutionEngine.execute(flow, context);
```

### 查看执行追踪
```java
List<TraceRecordEntity> traces = traceService.getByBusinessId("biz-1");
// 查看每个节点的执行状态：RUNNING -> SUCCESS/FAILED
```

## 依赖更新

### pom.xml
- ✅ 添加 `spring-expression` (Spring EL)
- ✅ 添加 `graalvm.polyglot` (JavaScript 执行)
- ✅ 添加 `graalvm.js` (JavaScript 引擎)
- ❌ 移除 `commons-jexl3` (已迁移到 SpEL)

## SpEL 表达式语法

FilterNodeExecutor 使用 SpEL 语法：

```spel
#record['price'] > 100                          // 比较
#record['status'] == 'active'                   // 字符串比较
#record['price'] > 100 && #record['status'] == 'active'  // AND
#record['price'] > 100 || #record['status'] == 'active'  // OR
!#record['deleted']                             // NOT
```

## TODO / 后续优化

1. **FlowDefinition 持久化**: 从数据库加载流程定义
2. **异步执行**: RunService 当前是同步执行，可改为异步
3. **节点错误恢复**: 支持失败重试机制
4. **执行超时**: 添加节点执行超时控制
5. **TransformNodeExecutor**: 修复 GraalVM return 语句问题
6. **更多 NodeExecutor**: 实现更多类型的节点执行器

## 注意事项

1. **SpEL vs JEXL**: 表达式语法有差异，需要更新现有流程配置
2. **Map 访问**: SpEL 访问 Map 字段使用 `#record['fieldName']` 语法
3. **循环依赖**: 引擎会检测并抛出异常
4. **执行追踪**: 每个节点都会记录到 trace_record 表
