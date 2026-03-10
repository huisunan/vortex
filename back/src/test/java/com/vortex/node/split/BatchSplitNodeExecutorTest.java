package com.vortex.node.split;

import com.vortex.engine.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchSplitNodeExecutorTest {

    @Test
    void shouldSplitRecordsByPlatformLimit() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-1", List.of("A1", "A2", "A3", "A4", "A5"));
        context.setRecords(List.of(
            Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
            Map.of("id", 4), Map.of("id", 5)
        ));

        executor.execute(context, Map.of("maxBatchSize", 2));

        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        assertThat(batches).hasSize(3); // 5 records / 2 = 3 batches
        assertThat(batches.get(0).getRecordCount()).isEqualTo(2);
        assertThat(batches.get(1).getRecordCount()).isEqualTo(2);
        assertThat(batches.get(2).getRecordCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleExactMultiple() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-2", List.of("A1"));
        context.setRecords(List.of(
            Map.of("id", 1), Map.of("id", 2), Map.of("id", 3), Map.of("id", 4)
        ));

        executor.execute(context, Map.of("maxBatchSize", 2));

        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        assertThat(batches).hasSize(2);
        assertThat(batches.get(0).getRecordCount()).isEqualTo(2);
        assertThat(batches.get(1).getRecordCount()).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyRecords() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-3", List.of());
        context.setRecords(List.of());

        executor.execute(context, Map.of("maxBatchSize", 10));

        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        assertThat(batches).isEmpty();
    }

    @Test
    void shouldHandleSingleRecord() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-4", List.of("A1"));
        context.setRecords(List.of(Map.of("id", 1)));

        executor.execute(context, Map.of("maxBatchSize", 10));

        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getRecordCount()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenMaxBatchSizeMissing() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-5", List.of());

        assertThatThrownBy(() -> executor.execute(context, Map.of()))
            .hasMessageContaining("maxBatchSize must be a positive integer");
    }

    @Test
    void shouldThrowExceptionWhenMaxBatchSizeInvalid() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-6", List.of());

        assertThatThrownBy(() -> executor.execute(context, Map.of("maxBatchSize", 0)))
            .hasMessageContaining("maxBatchSize must be a positive integer");
    }

    @Test
    void shouldSetBatchMetadata() {
        BatchSplitNodeExecutor executor = new BatchSplitNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-7", List.of("A1"));
        context.setRecords(List.of(
            Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)
        ));

        executor.execute(context, Map.of("maxBatchSize", 2));

        assertThat(context.getVariable("totalBatches")).isEqualTo(2);
        assertThat(context.getVariable("totalRecords")).isEqualTo(3);
    }
}
