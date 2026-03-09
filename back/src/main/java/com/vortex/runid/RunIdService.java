package com.vortex.runid;

import java.util.Map;

public class RunIdService {

    private final RunIdGenerator httpGenerator;
    private final RunIdGenerator uuidGenerator;

    public RunIdService(RunIdGenerator httpGenerator, RunIdGenerator uuidGenerator) {
        this.httpGenerator = httpGenerator;
        this.uuidGenerator = uuidGenerator;
    }

    public String generate(boolean useHttpGenerator, Map<String, Object> input) {
        try {
            return useHttpGenerator ? httpGenerator.generate(input) : uuidGenerator.generate(input);
        } catch (Exception ex) {
            throw new IllegalStateException("run_id generation failed", ex);
        }
    }
}
