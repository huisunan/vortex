package com.vortex.node.fetch;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.NodeExecutor;
import com.vortex.node.fetch.mapper.BusinessRecordMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DbFetchNodeExecutor implements NodeExecutor {

    private final BusinessRecordMapper mapper;

    public DbFetchNodeExecutor(BusinessRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String nodeType() {
        return "db-fetch";
    }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        List<Map<String, Object>> records = mapper.selectByBusinessIds(context.getBusinessIds())
                .stream()
                .map(row -> {
                    Map<String, Object> mapped = new HashMap<>();
                    mapped.put("businessId", row.getBusinessId());
                    mapped.put("payload", row.getPayload());
                    return mapped;
                })
                .collect(Collectors.toList());
        context.setRecords(records);
    }
}
