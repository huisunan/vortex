package com.vortex.run.api;

import com.vortex.run.domain.RunStatus;
import com.vortex.run.service.RunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping("/api/runs")
    public ResponseEntity<?> start(@RequestBody StartRunRequest request) {
        try {
            return ResponseEntity.ok(runService.start(request));
        } catch (IllegalStateException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "status", RunStatus.FAIL_INIT.name(),
                    "message", ex.getMessage()
            ));
        }
    }
}
