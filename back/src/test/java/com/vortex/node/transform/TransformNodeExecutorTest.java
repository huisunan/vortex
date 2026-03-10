package com.vortex.node.transform;

import com.vortex.engine.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransformNodeExecutorTest {

    @Test
    void shouldApplyScriptTransform() {
        TransformNodeExecutor executor = new TransformNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-1", List.of("A1"));
        context.setRecords(List.of(
            Map.of("status", 1),
            Map.of("status", 0)
        ));

        executor.execute(context, Map.of("script", "record.label = record.status == 1 ? '正常' : '禁用'; return record;"));

        assertThat(context.getRecords()).hasSize(2);
        assertThat(context.getRecords().get(0).get("label")).isEqualTo("正常");
        assertThat(context.getRecords().get(1).get("label")).isEqualTo("禁用");
    }

    @Test
    void shouldAddNewFields() {
        TransformNodeExecutor executor = new TransformNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-2", List.of("A1"));
        context.setRecords(List.of(
            Map.of("price", 100),
            Map.of("price", 200)
        ));

        executor.execute(context, Map.of("script", "record.tax = record.price * 0.08; return record;"));

        assertThat(context.getRecords().get(0).get("tax")).isEqualTo(8.0);
        assertThat(context.getRecords().get(1).get("tax")).isEqualTo(16.0);
    }

    @Test
    void shouldRenameFields() {
        TransformNodeExecutor executor = new TransformNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-3", List.of("A1"));
        context.setRecords(List.of(
            Map.of("oldName", "value1"),
            Map.of("oldName", "value2")
        ));

        executor.execute(context, Map.of("script", "record.newName = record.oldName; delete record.oldName; return record;"));

        assertThat(context.getRecords().get(0)).doesNotContainKey("oldName");
        assertThat(context.getRecords().get(0).get("newName")).isEqualTo("value1");
    }

    @Test
    void shouldThrowExceptionWhenScriptMissing() {
        TransformNodeExecutor executor = new TransformNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-4", List.of());

        assertThatThrownBy(() -> executor.execute(context, Map.of()))
            .hasMessageContaining("script is required");
    }

    @Test
    void shouldHandleComplexTransformation() {
        TransformNodeExecutor executor = new TransformNodeExecutor();
        ExecutionContext context = new ExecutionContext("run-5", List.of("A1"));
        context.setRecords(List.of(
            Map.of("firstName", "John", "lastName", "Doe"),
            Map.of("firstName", "Jane", "lastName", "Smith")
        ));

        executor.execute(context, Map.of("script", 
            "record.fullName = record.firstName + ' ' + record.lastName; " +
            "record.upperName = record.fullName.toUpperCase(); " +
            "return record;"));

        assertThat(context.getRecords().get(0).get("fullName")).isEqualTo("John Doe");
        assertThat(context.getRecords().get(0).get("upperName")).isEqualTo("JOHN DOE");
    }
}
