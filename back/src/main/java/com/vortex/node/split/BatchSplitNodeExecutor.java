package com.vortex.node.split;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.NodeExecutor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 批量拆分节点执行器 - 按目标平台单批次最大条数限制拆分数据
 * 
 * 配置项：
 * - maxBatchSize: 单批次最大条数（必填）
 * - batchIntervalMs: 批次间发送间隔（毫秒）
 */
@Component
public class BatchSplitNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "batch-split";
    }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        Integer maxBatchSize = (Integer) config.get("maxBatchSize");
        
        if (maxBatchSize == null || maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be a positive integer");
        }

        List<Map<String, Object>> records = context.getRecords();
        List<Batch> batches = split(records, maxBatchSize);
        
        // 将批次信息存入上下文，供后续推送节点使用
        context.setVariable("batches", batches);
        context.setVariable("totalBatches", batches.size());
        context.setVariable("totalRecords", records.size());
    }

    /**
     * 将记录列表按指定大小拆分
     */
    public List<Batch> split(List<Map<String, Object>> records, int maxBatchSize) {
        List<Batch> batches = new ArrayList<>();
        
        if (records == null || records.isEmpty()) {
            return batches;
        }

        int totalRecords = records.size();
        int batchCount = (int) Math.ceil((double) totalRecords / maxBatchSize);

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * maxBatchSize;
            int toIndex = Math.min(fromIndex + maxBatchSize, totalRecords);
            
            List<Map<String, Object>> batchRecords = records.subList(fromIndex, toIndex);
            Batch batch = new Batch();
            batch.setBatchIndex(i + 1);
            batch.setTotalBatches(batchCount);
            batch.setRecords(new ArrayList<>(batchRecords));
            batch.setRecordCount(batchRecords.size());
            batch.setStatus("PENDING");
            
            batches.add(batch);
        }

        return batches;
    }

    /**
     * 批次信息
     */
    @Data
    @NoArgsConstructor
    public static class Batch {
        private int batchIndex;
        private int totalBatches;
        private List<Map<String, Object>> records;
        private int recordCount;
        private String status; // PENDING, PROCESSING, SUCCESS, FAILED
        private String errorMessage;
    }
}
