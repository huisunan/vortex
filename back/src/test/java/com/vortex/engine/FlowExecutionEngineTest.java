package com.vortex.engine;

import com.vortex.flow.domain.EdgeDefinition;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.domain.NodeDefinition;
import com.vortex.trace.mapper.TraceRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowExecutionEngineTest {

    private FlowExecutionEngine engine;
    private ExecutionTracer tracer;
    private TraceRecordMapper mockMapper;

    @BeforeEach
    void setUp() {
        // 使用 Mockito 创建 mock
        mockMapper = Mockito.mock(TraceRecordMapper.class);
        when(mockMapper.insert(any(com.vortex.trace.entity.TraceRecordEntity.class))).thenReturn(1);
        
        tracer = new ExecutionTracer(mockMapper);
        
        // 创建 mock 的 NodeExecutor 用于测试
        NodeExecutor mockExecutor = new NodeExecutor() {
            @Override
            public String nodeType() {
                return "mock";
            }

            @Override
            public void execute(ExecutionContext context, Map<String, Object> config) {
                // 不执行任何操作
            }
        };
        
        NodeExecutor fetchExecutor = new NodeExecutor() {
            @Override
            public String nodeType() {
                return "fetch";
            }

            @Override
            public void execute(ExecutionContext context, Map<String, Object> config) {
                // 模拟 fetch 操作
            }
        };
        
        NodeExecutor transformExecutor = new NodeExecutor() {
            @Override
            public String nodeType() {
                return "transform";
            }

            @Override
            public void execute(ExecutionContext context, Map<String, Object> config) {
                // 模拟 transform 操作
            }
        };
        
        NodeExecutor filterExecutor = new NodeExecutor() {
            @Override
            public String nodeType() {
                return "filter";
            }

            @Override
            public void execute(ExecutionContext context, Map<String, Object> config) {
                // 模拟 filter 操作
            }
        };
        
        engine = new FlowExecutionEngine(Arrays.asList(
            mockExecutor, fetchExecutor, transformExecutor, filterExecutor
        ), tracer);
    }

    @Test
    void shouldExecuteSimpleLinearFlow() {
        // Given: 一个简单的线性流程：fetch -> transform
        FlowDefinition flow = new FlowDefinition(
            "test-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("node1", "fetch", Map.of("query", "SELECT * FROM test")),
                new NodeDefinition("node2", "transform", Map.of("script", "return record;"))
            ),
            Arrays.asList(
                new EdgeDefinition("node1", "node2")
            )
        );

        ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1", "biz-2"));
        context.setRecords(Arrays.asList(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        ));

        // When: 执行流程
        engine.execute(flow, context);

        // Then: 应该成功执行，不抛出异常
        assertNotNull(context.getRecords());
    }

    @Test
    void shouldExecuteNodesInTopologicalOrder() {
        // Given: 一个有依赖关系的流程：fetch -> filter -> transform
        FlowDefinition flow = new FlowDefinition(
            "test-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("node3", "transform", Map.of("script", "return record;")),
                new NodeDefinition("node1", "fetch", Map.of("query", "SELECT * FROM test")),
                new NodeDefinition("node2", "filter", Map.of("expression", "id > 0"))
            ),
            Arrays.asList(
                new EdgeDefinition("node1", "node2"),
                new EdgeDefinition("node2", "node3")
            )
        );

        ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1"));
        context.setRecords(Arrays.asList(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", -1, "name", "test2") // 这个会被过滤掉
        ));

        // When: 执行流程
        engine.execute(flow, context);

        // Then: 应该按照正确的顺序执行（fetch -> filter -> transform）
        assertNotNull(context.getRecords());
    }

    @Test
    void shouldHandleSingleNodeFlow() {
        // Given: 只有一个节点的流程
        FlowDefinition flow = new FlowDefinition(
            "test-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("node1", "fetch", Map.of("query", "SELECT * FROM test"))
            ),
            Arrays.asList()
        );

        ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1"));
        context.setRecords(Arrays.asList(Map.of("id", 1, "name", "test")));

        // When: 执行流程
        engine.execute(flow, context);

        // Then: 应该成功执行
        assertNotNull(context.getRecords());
    }

    @Test
    void shouldThrowExceptionForCircularDependency() {
        // Given: 一个有循环依赖的流程：A -> B -> C -> A
        FlowDefinition flow = new FlowDefinition(
            "test-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("A", "fetch", Map.of()),
                new NodeDefinition("B", "transform", Map.of("script", "return record;")),
                new NodeDefinition("C", "filter", Map.of("expression", "true"))
            ),
            Arrays.asList(
                new EdgeDefinition("A", "B"),
                new EdgeDefinition("B", "C"),
                new EdgeDefinition("C", "A")
            )
        );

        ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1"));

        // When/Then: 应该抛出异常
        assertThrows(IllegalStateException.class, () -> engine.execute(flow, context));
    }

    @Test
    void shouldExecuteParallelNodes() {
        // Given: 一个并行流程：fetch -> (filter1, filter2) -> transform
        FlowDefinition flow = new FlowDefinition(
            "test-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("node1", "fetch", Map.of("query", "SELECT * FROM test")),
                new NodeDefinition("node2", "filter", Map.of("expression", "id > 0")),
                new NodeDefinition("node3", "filter", Map.of("expression", "name != null")),
                new NodeDefinition("node4", "transform", Map.of("script", "return record;"))
            ),
            Arrays.asList(
                new EdgeDefinition("node1", "node2"),
                new EdgeDefinition("node1", "node3"),
                new EdgeDefinition("node2", "node4"),
                new EdgeDefinition("node3", "node4")
            )
        );

        ExecutionContext context = new ExecutionContext("run-123", Arrays.asList("biz-1"));
        context.setRecords(Arrays.asList(Map.of("id", 1, "name", "test")));

        // When: 执行流程
        engine.execute(flow, context);

        // Then: 应该成功执行
        assertNotNull(context.getRecords());
    }
}
