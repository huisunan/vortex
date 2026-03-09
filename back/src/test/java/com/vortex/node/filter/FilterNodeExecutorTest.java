package com.vortex.node.filter;

import com.vortex.engine.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterNodeExecutorTest {

    @Test
    void shouldFilterOutRecordsWhenExpressionFalse() {
        FilterNodeExecutor executor = new FilterNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-1", List.of("A1", "A2"));
        context.setRecords(List.of(
            Map.of("price", 10, "name", "cheap"),
            Map.of("price", 200, "name", "expensive"),
            Map.of("price", 50, "name", "medium")
        ));

        // SpEL uses #record['fieldName'] syntax for Map access
        executor.execute(context, Map.of("expression", "#record['price'] > 100"));

        assertThat(context.getRecords()).hasSize(1);
        assertThat(context.getRecords().get(0).get("name")).isEqualTo("expensive");
    }

    @Test
    void shouldKeepAllRecordsWhenExpressionTrue() {
        FilterNodeExecutor executor = new FilterNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-2", List.of("A1"));
        context.setRecords(List.of(
            Map.of("status", "active"),
            Map.of("status", "active")
        ));

        // SpEL uses #record['fieldName'] syntax for Map access
        executor.execute(context, Map.of("expression", "#record['status'] == 'active'"));

        assertThat(context.getRecords()).hasSize(2);
    }

    @Test
    void shouldFilterWithAndLogic() {
        FilterNodeExecutor executor = new FilterNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-3", List.of("A1"));
        context.setRecords(List.of(
            Map.of("price", 150, "status", "active"),
            Map.of("price", 150, "status", "inactive"),
            Map.of("price", 50, "status", "active")
        ));

        // SpEL uses #record['fieldName'] syntax for Map access
        executor.execute(context, Map.of("expression", "#record['price'] > 100 && #record['status'] == 'active'"));

        assertThat(context.getRecords()).hasSize(1);
        assertThat(context.getRecords().get(0).get("status")).isEqualTo("active");
    }

    @Test
    void shouldThrowExceptionWhenExpressionMissing() {
        FilterNodeExecutor executor = new FilterNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-4", List.of());

        assertThatThrownBy(() -> executor.execute(context, Map.of()))
            .hasMessageContaining("expression is required");
    }

    @Test
    void shouldFilterWithOrLogic() {
        FilterNodeExecutor executor = new FilterNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-5", List.of("A1"));
        context.setRecords(List.of(
            Map.of("price", 50, "status", "active"),
            Map.of("price", 200, "status", "inactive"),
            Map.of("price", 30, "status", "inactive")
        ));

        // SpEL uses #record['fieldName'] syntax for Map access
        executor.execute(context, Map.of("expression", "#record['price'] > 100 || #record['status'] == 'active'"));

        assertThat(context.getRecords()).hasSize(2);
    }
}
