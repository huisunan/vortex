package com.vortex.flow.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeDefinition {
    private String fromNodeId;
    private String toNodeId;
}
