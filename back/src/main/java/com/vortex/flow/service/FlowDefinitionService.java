package com.vortex.flow.service;

import com.vortex.flow.domain.EdgeDefinition;
import com.vortex.flow.domain.FlowDefinition;
import com.vortex.flow.domain.NodeDefinition;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow 定义服务
 * 
 * 负责加载和管理 FlowDefinition
 * 
 * TODO: 后续从数据库加载
 */
@Service
public class FlowDefinitionService {

    // 临时内存存储，后续替换为数据库
    private final Map<String, FlowDefinition> flowCache = new HashMap<>();

    public FlowDefinitionService() {
        // 初始化示例流程
        initSampleFlows();
    }

    private void initSampleFlows() {
        // 示例流程：fetch -> filter -> transform
        FlowDefinition sampleFlow = new FlowDefinition(
            "sample-flow",
            "default",
            Arrays.asList(
                new NodeDefinition("fetch", "fetch", Map.of("query", "SELECT * FROM business")),
                new NodeDefinition("filter", "filter", Map.of("expression", "status == 1")),
                new NodeDefinition("transform", "transform", Map.of("script", "record.processed = true; return record;"))
            ),
            Arrays.asList(
                new EdgeDefinition("fetch", "filter"),
                new EdgeDefinition("filter", "transform")
            )
        );
        flowCache.put("sample-flow", sampleFlow);
    }

    /**
     * 根据 flowCode 加载 FlowDefinition
     * 
     * @param flowCode 流程代码
     * @return FlowDefinition
     * @throws IllegalArgumentException 如果流程不存在
     */
    public FlowDefinition load(String flowCode) {
        FlowDefinition flow = flowCache.get(flowCode);
        if (flow == null) {
            throw new IllegalArgumentException("Flow not found: " + flowCode);
        }
        return flow;
    }

    /**
     * 保存 FlowDefinition（临时方法）
     * 
     * @param flow FlowDefinition
     */
    public void save(FlowDefinition flow) {
        flowCache.put(flow.getFlowCode(), flow);
    }
}
