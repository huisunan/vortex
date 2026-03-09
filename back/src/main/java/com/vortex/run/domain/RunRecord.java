package com.vortex.run.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunRecord {
    private String runId;
    private RunStatus status;
    private String message;
}
