package com.vortex.engine;

import java.util.Map;

public interface NodeExecutor {
    String nodeType();

    void execute(ExecutionContext context, Map<String, Object> config);
}
