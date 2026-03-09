package com.vortex.run.service;

import com.vortex.run.api.StartRunRequest;
import com.vortex.run.domain.RunRecord;
import com.vortex.run.domain.RunStatus;
import com.vortex.runid.RunIdService;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RunService {

    private final RunIdService runIdService;

    public RunService(RunIdService runIdService) {
        this.runIdService = runIdService;
    }

    public RunRecord start(StartRunRequest request) {
        var input = new HashMap<String, Object>();
        input.put("flowCode", request.getFlowCode());
        input.put("businessIds", request.getBusinessIds());
        var runId = runIdService.generate(false, input);
        return new RunRecord(runId, RunStatus.RUNNING, "accepted");
    }
}
