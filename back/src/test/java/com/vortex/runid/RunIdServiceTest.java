package com.vortex.runid;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RunIdServiceTest {

    @Test
    void shouldFailFastWhenHttpGeneratorFails() {
        var http = mock(RunIdGenerator.class);
        when(http.generate(any())).thenThrow(new RuntimeException("503"));

        var service = new RunIdService(http, new UuidRunIdGenerator());

        assertThatThrownBy(() -> service.generate(true, Map.of()))
                .hasMessageContaining("run_id generation failed");
    }
}
