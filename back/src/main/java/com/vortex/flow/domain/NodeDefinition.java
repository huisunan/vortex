package com.vortex.flow.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinition {
    private String nodeId;
    private String nodeType;
    private Map<String, Object> config;
}
