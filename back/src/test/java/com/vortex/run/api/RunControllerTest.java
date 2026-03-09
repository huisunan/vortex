package com.vortex.run.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.vortex.run.service.RunService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RunControllerTest {

    @Test
    void shouldReturn422WhenRunIdGenerationFails() throws Exception {
        RunService runService = mock(RunService.class);
        when(runService.start(any())).thenThrow(new IllegalStateException("run_id generation failed"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RunController(runService)).build();

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flowCode\":\"flow-1\",\"businessIds\":[\"A1\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("FAIL_INIT"));
    }
}
