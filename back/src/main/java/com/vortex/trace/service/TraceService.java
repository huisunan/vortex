package com.vortex.trace.service;

import com.vortex.trace.entity.TraceRecordEntity;
import com.vortex.trace.mapper.TraceRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraceService {

    private final TraceRecordMapper traceRecordMapper;

    public TraceService(TraceRecordMapper traceRecordMapper) {
        this.traceRecordMapper = traceRecordMapper;
    }

    public List<TraceRecordEntity> getByBusinessId(String businessId) {
        return traceRecordMapper.selectByBusinessId(businessId);
    }
}
