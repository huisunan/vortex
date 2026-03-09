package com.vortex.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {
    private String runId;
    private List<String> businessIds = new ArrayList<>();
    private List<Map<String, Object>> records = new ArrayList<>();
    private Map<String, Object> variables = new HashMap<>();

    public ExecutionContext(String runId, List<String> businessIds) {
        this.runId = runId;
        this.businessIds = businessIds;
        this.records = new ArrayList<>();
        this.variables = new HashMap<>();
    }

    public void setVariable(String key, Object value) {
        this.variables.put(key, value);
    }

    public Object getVariable(String key) {
        return this.variables.get(key);
    }
}
