package com.vortex.trace.api;

import com.vortex.trace.entity.TraceRecordEntity;
import com.vortex.trace.service.TraceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TraceControllerTest {

    @Test
    void shouldReturnTraceByBusinessId() throws Exception {
        TraceService traceService = mock(TraceService.class);
        when(traceService.getByBusinessId("A1"))
                .thenReturn(List.of(new TraceRecordEntity(1L, "run-1", "A1", "db-fetch", "SUCCESS", "ok")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TraceController(traceService)).build();

        mockMvc.perform(get("/api/traces/business/A1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessId").value("A1"));
    }
}
