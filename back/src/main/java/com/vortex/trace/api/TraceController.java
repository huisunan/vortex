package com.vortex.trace.api;

import com.vortex.trace.entity.TraceRecordEntity;
import com.vortex.trace.service.TraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/api/traces/business/{businessId}")
    public List<TraceRecordEntity> byBusiness(@PathVariable String businessId) {
        return traceService.getByBusinessId(businessId);
    }
}
