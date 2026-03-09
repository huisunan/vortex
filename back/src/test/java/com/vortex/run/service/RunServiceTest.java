package com.vortex.run.service;

import com.vortex.run.api.StartRunRequest;
import com.vortex.run.domain.RunStatus;
import com.vortex.runid.RunIdService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RunServiceTest {

    @Test
    void shouldCreateRunningRecordWithUuidGeneratorByDefault() {
        RunIdService runIdService = mock(RunIdService.class);
        when(runIdService.generate(eq(false), anyMap())).thenReturn("run-001");
        RunService service = new RunService(runIdService);

        StartRunRequest request = new StartRunRequest("flow-1", List.of("A1"));
        var record = service.start(request);

        assertThat(record.getRunId()).isEqualTo("run-001");
        assertThat(record.getStatus()).isEqualTo(RunStatus.RUNNING);
        assertThat(record.getMessage()).isEqualTo("accepted");
    }
}
