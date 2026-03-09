package com.vortex.runid;

import java.util.Map;

public interface RunIdGenerator {
    String generate(Map<String, Object> input);
}
