package com.vortex.node.fetch;

import com.vortex.engine.ExecutionContext;
import com.vortex.node.fetch.entity.BusinessRecordEntity;
import com.vortex.node.fetch.mapper.BusinessRecordMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbFetchNodeExecutorTest {

    @Test
    void shouldLoadRecordsByBusinessIdsIntoContext() {
        BusinessRecordMapper mapper = mock(BusinessRecordMapper.class);
        BusinessRecordEntity row = new BusinessRecordEntity(1L, "A1", "{\"name\":\"alpha\"}");
        when(mapper.selectByBusinessIds(List.of("A1"))).thenReturn(List.of(row));

        DbFetchNodeExecutor executor = new DbFetchNodeExecutor(mapper);
        ExecutionContext context = new ExecutionContext("run-1", List.of("A1"));

        executor.execute(context, Map.of());

        assertThat(context.getRecords()).hasSize(1);
        assertThat(context.getRecords().get(0).get("businessId")).isEqualTo("A1");
    }
}
