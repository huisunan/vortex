package com.vortex.run.service;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.FlowExecutionEngine;
import com.vortex.flow.domain.EdgeDefinition;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.domain.NodeDefinition;
import com.vortex.flow.service.FlowDefinitionService;
import com.vortex.run.api.StartRunRequest;
import com.vortex.run.domain.RunStatus;
import com.vortex.runid.RunIdService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RunServiceTest {

    @Test
    void shouldCreateRunningRecordWithUuidGeneratorByDefault() {
        // Mock 依赖
        RunIdService runIdService = mock(RunIdService.class);
        when(runIdService.generate(eq(false), anyMap())).thenReturn("run-001");
        
        FlowDefinitionService flowDefinitionService = mock(FlowDefinitionService.class);
        FlowDefinition mockFlow = new FlowDefinition(
            "flow-1",
            "default",
            Arrays.asList(),
            Arrays.asList()
        );
        when(flowDefinitionService.load("flow-1")).thenReturn(mockFlow);
        
        FlowExecutionEngine flowExecutionEngine = mock(FlowExecutionEngine.class);
        doNothing().when(flowExecutionEngine).execute(any(FlowDefinition.class), any(ExecutionContext.class));
        
        RunService service = new RunService(runIdService, flowDefinitionService, flowExecutionEngine);

        StartRunRequest request = new StartRunRequest("flow-1", List.of("A1"));
        var record = service.start(request);

        assertThat(record.getRunId()).isEqualTo("run-001");
        assertThat(record.getStatus()).isEqualTo(RunStatus.SUCCESS);
        assertThat(record.getMessage()).isEqualTo("completed");
    }

    @Test
    void shouldReturnFailStatusWhenExecutionFails() {
        // Mock 依赖
        RunIdService runIdService = mock(RunIdService.class);
        when(runIdService.generate(eq(false), anyMap())).thenReturn("run-002");
        
        FlowDefinitionService flowDefinitionService = mock(FlowDefinitionService.class);
        FlowDefinition mockFlow = new FlowDefinition(
            "flow-1",
            "default",
            Arrays.asList(),
            Arrays.asList()
        );
        when(flowDefinitionService.load("flow-1")).thenReturn(mockFlow);
        
        FlowExecutionEngine flowExecutionEngine = mock(FlowExecutionEngine.class);
        doThrow(new RuntimeException("Execution failed")).when(flowExecutionEngine)
            .execute(any(FlowDefinition.class), any(ExecutionContext.class));
        
        RunService service = new RunService(runIdService, flowDefinitionService, flowExecutionEngine);

        StartRunRequest request = new StartRunRequest("flow-1", List.of("A1"));
        var record = service.start(request);

        assertThat(record.getRunId()).isEqualTo("run-002");
        assertThat(record.getStatus()).isEqualTo(RunStatus.FAIL);
        assertThat(record.getMessage()).contains("Execution failed");
    }
}
