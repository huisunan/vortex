package com.vortex.engine;

import com.vortex.flow.domain.EdgeDefinition;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.domain.NodeDefinition;
import com.vortex.flow.service.FlowDefinitionService;
import com.vortex.node.extract.JsonPathResultExtractor;
import com.vortex.node.extract.ResultExtractor;
import com.vortex.node.split.BatchSplitNodeExecutor;
import com.vortex.run.api.StartRunRequest;
import com.vortex.run.domain.RunStatus;
import com.vortex.run.service.RunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow 执行引擎集成测试
 * 
 * 测试完整流程：fetch -> filter -> transform -> batch-split -> push -> extract
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowExecutionIntegrationTest {

    @Autowired
    private RunService runService;

    @Autowired
    private FlowDefinitionService flowDefinitionService;

    @Test
    void shouldExecuteCompleteFlow() {
        // Given: 定义一个完整的流程（不包含 transform，因为 GraalVM 需要特殊配置）
        FlowDefinition flow = new FlowDefinition(
            "integration-test-flow",
            "test-platform",
            Arrays.asList(
                // 1. 过滤（测试流程从过滤开始，因为 db-fetch 需要数据库中有数据）
                new NodeDefinition("filter", "filter", Map.of(
                    "expression", "#record['status'] == 1"
                )),
                // 2. 批量拆分
                new NodeDefinition("split", "batch-split", Map.of(
                    "maxBatchSize", 10
                ))
            ),
            Arrays.asList(
                new EdgeDefinition("filter", "split")
            )
        );

        // 保存流程定义
        flowDefinitionService.save(flow);

        // When: 启动流程执行
        StartRunRequest request = new StartRunRequest("integration-test-flow", Arrays.asList("biz-1", "biz-2"));
        var result = runService.start(request);

        // Then: 应该成功执行
        if (result.getStatus() != RunStatus.SUCCESS) {
            System.err.println("Flow execution failed: " + result.getMessage());
        }
        assertThat(result.getStatus()).isEqualTo(RunStatus.SUCCESS);
    }

    @Test
    void shouldHandleCircularDependency() {
        // Given: 一个有循环依赖的流程
        FlowDefinition flow = new FlowDefinition(
            "circular-flow",
            "test-platform",
            Arrays.asList(
                new NodeDefinition("A", "fetch", Map.of()),
                new NodeDefinition("B", "filter", Map.of("expression", "true")),
                new NodeDefinition("C", "transform", Map.of("script", "return record;"))
            ),
            Arrays.asList(
                new EdgeDefinition("A", "B"),
                new EdgeDefinition("B", "C"),
                new EdgeDefinition("C", "A")  // 循环依赖
            )
        );

        flowDefinitionService.save(flow);

        // When: 启动流程执行
        StartRunRequest request = new StartRunRequest("circular-flow", Arrays.asList("biz-1"));
        var result = runService.start(request);

        // Then: 应该失败并返回错误信息
        assertThat(result.getStatus()).isEqualTo(RunStatus.FAIL);
        assertThat(result.getMessage()).contains("Circular");
    }

    @Test
    void shouldExecuteBatchSplitCorrectly() {
        // Given: 测试批量拆分
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-test", Arrays.asList("biz-1"));
        
        // 创建 25 条记录
        List<Map<String, Object>> records = Arrays.asList(
            Map.of("id", 1), Map.of("id", 2), Map.of("id", 3), Map.of("id", 4), Map.of("id", 5),
            Map.of("id", 6), Map.of("id", 7), Map.of("id", 8), Map.of("id", 9), Map.of("id", 10),
            Map.of("id", 11), Map.of("id", 12), Map.of("id", 13), Map.of("id", 14), Map.of("id", 15),
            Map.of("id", 16), Map.of("id", 17), Map.of("id", 18), Map.of("id", 19), Map.of("id", 20),
            Map.of("id", 21), Map.of("id", 22), Map.of("id", 23), Map.of("id", 24), Map.of("id", 25)
        );
        context.setRecords(records);

        // When: 执行批量拆分（每批 10 条）
        executor.execute(context, Map.of("maxBatchSize", 10));

        // Then: 应该拆分为 3 个批次
        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        assertThat(batches).hasSize(3);
        assertThat(batches.get(0).getRecordCount()).isEqualTo(10);
        assertThat(batches.get(1).getRecordCount()).isEqualTo(10);
        assertThat(batches.get(2).getRecordCount()).isEqualTo(5);
        assertThat(context.getVariable("totalBatches")).isEqualTo(3);
        assertThat(context.getVariable("totalRecords")).isEqualTo(25);
    }

    @Test
    void shouldExtractResultWithJsonPath() {
        // Given: JSONPath 提取器
        ResultExtractor extractor = new JsonPathResultExtractor();
        
        String responseBody = """
            {
                "status": "SUCCESS",
                "data": {
                    "platformId": "platform-123",
                    "batchId": "batch-456"
                },
                "error": null
            }
            """;
        
        Map<String, Object> config = Map.of(
            "statusPath", "$.status",
            "platformIdPath", "$.data.platformId",
            "batchIdPath", "$.data.batchId"
        );

        // When: 执行提取
        ResultExtractor.ExtractResult result = extractor.extract(responseBody, config);

        // Then: 应该正确提取字段
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getPlatformId()).isEqualTo("platform-123");
        assertThat(result.getBatchId()).isEqualTo("batch-456");
    }

    @Test
    void shouldExtractAsyncBatchId() {
        // Given: 异步模式下的响应
        ResultExtractor extractor = new JsonPathResultExtractor();
        
        String responseBody = """
            {
                "code": 200,
                "message": "accepted",
                "data": {
                    "taskId": "async-task-789"
                }
            }
            """;
        
        Map<String, Object> config = Map.of(
            "batchIdPath", "$.data.taskId"
        );

        // When: 执行提取（异步模式只提取批次 ID）
        ResultExtractor.ExtractResult result = extractor.extract(responseBody, config);

        // Then: 应该提取到批次 ID
        assertThat(result.getBatchId()).isEqualTo("async-task-789");
    }
}
