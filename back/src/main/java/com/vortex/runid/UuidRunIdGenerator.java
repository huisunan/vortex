package com.vortex.runid;

import java.util.Map;
import java.util.UUID;

public class UuidRunIdGenerator implements RunIdGenerator {

    @Override
    public String generate(Map<String, Object> input) {
        return UUID.randomUUID().toString();
    }
}
