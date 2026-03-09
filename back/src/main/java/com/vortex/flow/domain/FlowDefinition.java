package com.vortex.flow.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinition {
    private String flowCode;
    private String platformCode;
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
}
