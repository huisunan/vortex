package com.vortex.flow.service;

import com.vortex.flow.domain.FlowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowDefinitionValidatorTest {

    @Test
    void shouldRejectFlowWithoutPlatformCode() {
        var flow = new FlowDefinition("flow-1", "", List.of(), List.of());

        assertThatThrownBy(() -> new FlowDefinitionValidator().validate(flow))
                .hasMessageContaining("platformCode is required");
    }
}
