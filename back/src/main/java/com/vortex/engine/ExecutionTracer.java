package com.vortex.engine;

import com.vortex.trace.entity.TraceRecordEntity;
import com.vortex.trace.mapper.TraceRecordMapper;
import org.springframework.stereotype.Component;

/**
 * 执行追踪器
 * 
 * 负责将节点执行状态持久化到 trace_record 表
 */
@Component
public class ExecutionTracer {

    private final TraceRecordMapper traceRecordMapper;

    public ExecutionTracer(TraceRecordMapper traceRecordMapper) {
        this.traceRecordMapper = traceRecordMapper;
    }

    /**
     * 记录节点执行开始
     * 
     * @param runId 运行 ID
     * @param businessId 业务 ID
     * @param nodeId 节点 ID
     */
    public void recordStart(String runId, String businessId, String nodeId) {
        TraceRecordEntity record = new TraceRecordEntity();
        record.setRunId(runId);
        record.setBusinessId(businessId);
        record.setNodeId(nodeId);
        record.setStatus("RUNNING");
        record.setDetail("Node execution started");
        traceRecordMapper.insert(record);
    }

    /**
     * 记录节点执行成功
     * 
     * @param runId 运行 ID
     * @param businessId 业务 ID
     * @param nodeId 节点 ID
     * @param detail 详细信息
     */
    public void recordSuccess(String runId, String businessId, String nodeId, String detail) {
        TraceRecordEntity record = new TraceRecordEntity();
        record.setRunId(runId);
        record.setBusinessId(businessId);
        record.setNodeId(nodeId);
        record.setStatus("SUCCESS");
        record.setDetail(detail != null ? detail : "Node execution completed successfully");
        traceRecordMapper.insert(record);
    }

    /**
     * 记录节点执行失败
     * 
     * @param runId 运行 ID
     * @param businessId 业务 ID
     * @param nodeId 节点 ID
     * @param error 错误信息
     */
    public void recordFailure(String runId, String businessId, String nodeId, String error) {
        TraceRecordEntity record = new TraceRecordEntity();
        record.setRunId(runId);
        record.setBusinessId(businessId);
        record.setNodeId(nodeId);
        record.setStatus("FAILED");
        record.setDetail(error != null ? error : "Node execution failed");
        traceRecordMapper.insert(record);
    }
}
