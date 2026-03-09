package com.vortex.runid;

import java.util.Map;
import java.util.UUID;

public class HttpRunIdGenerator implements RunIdGenerator {

    @Override
    public String generate(Map<String, Object> input) {
        Object value = input.get("runId");
        if (value instanceof String runId && !runId.isBlank()) {
            return runId;
        }
        return UUID.randomUUID().toString();
    }
}
