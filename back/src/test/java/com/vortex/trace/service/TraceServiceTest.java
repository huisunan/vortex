package com.vortex.trace.service;

import com.vortex.trace.entity.TraceRecordEntity;
import com.vortex.trace.mapper.TraceRecordMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceServiceTest {

    @Test
    void shouldReturnTraceRecordsByBusinessId() {
        TraceRecordMapper mapper = mock(TraceRecordMapper.class);
        TraceRecordEntity row = new TraceRecordEntity(1L, "run-1", "A1", "db-fetch", "SUCCESS", "ok");
        when(mapper.selectByBusinessId("A1")).thenReturn(List.of(row));

        TraceService service = new TraceService(mapper);
        List<TraceRecordEntity> result = service.getByBusinessId("A1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessId()).isEqualTo("A1");
    }
}
