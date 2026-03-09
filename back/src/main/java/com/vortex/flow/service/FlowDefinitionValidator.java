package com.vortex.flow.service;

import com.vortex.flow.domain.FlowDefinition;

public class FlowDefinitionValidator {

    public void validate(FlowDefinition flow) {
        if (flow == null) {
            throw new IllegalArgumentException("flow is required");
        }
        if (flow.getPlatformCode() == null || flow.getPlatformCode().isBlank()) {
            throw new IllegalArgumentException("platformCode is required");
        }
    }
}
