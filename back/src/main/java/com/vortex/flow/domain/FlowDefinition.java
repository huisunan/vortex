package com.vortex.flow.domain;

import java.util.List;

public record FlowDefinition(
        String flowCode,
        String platformCode,
        List<NodeDefinition> nodes,
        List<EdgeDefinition> edges
) {
}
