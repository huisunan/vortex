package com.vortex.flow.service;

import com.vortex.flow.domain.FlowDefinition;

public class FlowDefinitionValidator {

    public void validate(FlowDefinition flow) {
        if (flow == null) {
            throw new IllegalArgumentException("flow is required");
        }
        if (flow.platformCode() == null || flow.platformCode().isBlank()) {
            throw new IllegalArgumentException("platformCode is required");
        }
    }
}
