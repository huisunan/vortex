package com.vortex.flow.domain;

import java.util.Map;

public record NodeDefinition(
        String nodeId,
        String nodeType,
        Map<String, Object> config
) {
}
