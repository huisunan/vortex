package com.vortex.run.service;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.FlowExecutionEngine;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.service.FlowDefinitionService;
import com.vortex.run.api.StartRunRequest;
import com.vortex.run.domain.RunRecord;
import com.vortex.run.domain.RunStatus;
import com.vortex.runid.RunIdService;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RunService {

    private final RunIdService runIdService;
    private final FlowDefinitionService flowDefinitionService;
    private final FlowExecutionEngine flowExecutionEngine;

    public RunService(
        RunIdService runIdService,
        FlowDefinitionService flowDefinitionService,
        FlowExecutionEngine flowExecutionEngine
    ) {
        this.runIdService = runIdService;
        this.flowDefinitionService = flowDefinitionService;
        this.flowExecutionEngine = flowExecutionEngine;
    }

    /**
     * 启动流程执行
     * 
     * @param request 启动请求
     * @return 运行记录
     */
    public RunRecord start(StartRunRequest request) {
        // 生成 runId
        var input = new HashMap<String, Object>();
        input.put("flowCode", request.getFlowCode());
        input.put("businessIds", request.getBusinessIds());
        var runId = runIdService.generate(false, input);

        // 加载流程定义
        FlowDefinition flow = flowDefinitionService.load(request.getFlowCode());

        // 创建执行上下文
        ExecutionContext context = new ExecutionContext(runId, request.getBusinessIds());

        // 执行流程（异步执行，这里先同步执行）
        try {
            flowExecutionEngine.execute(flow, context);
            return new RunRecord(runId, RunStatus.SUCCESS, "completed");
        } catch (Exception e) {
            return new RunRecord(runId, RunStatus.FAIL, e.getMessage());
        }
    }
}
