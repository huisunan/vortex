package com.vortex.node.push;

import com.vortex.engine.ExecutionContext;
import com.vortex.engine.NodeExecutor;
import com.vortex.node.split.BatchSplitNodeExecutor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送节点执行器 - 将批次数据推送到目标平台
 * 
 * 配置项：
 * - url: 目标平台 API 地址
 * - method: HTTP 方法 (POST/PUT)
 * - headers: 请求头
 * - platformCode: 目标平台编码
 */
@Component
public class PushNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String nodeType() {
        return "push";
    }

    @Override
    public void execute(ExecutionContext context, Map<String, Object> config) {
        String url = (String) config.get("url");
        String method = (String) config.get("method");
        Map<String, String> headers = (Map<String, String>) config.get("headers");
        String platformCode = (String) config.get("platformCode");

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required for push node");
        }

        // 获取批次信息
        List<BatchSplitNodeExecutor.Batch> batches = 
            (List<BatchSplitNodeExecutor.Batch>) context.getVariable("batches");
        
        if (batches == null || batches.isEmpty()) {
            throw new IllegalStateException("No batches to push. Ensure batch-split node runs before push node.");
        }

        List<PushResult> results = new ArrayList<>();

        // 逐个批次推送
        for (BatchSplitNodeExecutor.Batch batch : batches) {
            PushResult result = pushBatch(batch, url, method, headers, platformCode);
            results.add(result);
            batch.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
            batch.setErrorMessage(result.getErrorMessage());
        }

        // 将推送结果存入上下文
        context.setVariable("pushResults", results);
    }

    private PushResult pushBatch(BatchSplitNodeExecutor.Batch batch, String url, 
                                  String method, Map<String, String> headers, 
                                  String platformCode) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("platformCode", platformCode);
            requestBody.put("batchIndex", batch.getBatchIndex());
            requestBody.put("totalBatches", batch.getTotalBatches());
            requestBody.put("records", batch.getRecords());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, httpHeaders);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            PushResult result = new PushResult();
            result.setSuccess(response.getStatusCode().is2xxSuccessful());
            result.setBatchIndex(batch.getBatchIndex());
            result.setResponseBody(response.getBody());
            result.setStatusCode(response.getStatusCode().value());

            return result;

        } catch (Exception e) {
            PushResult result = new PushResult();
            result.setSuccess(false);
            result.setBatchIndex(batch.getBatchIndex());
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Data
    @NoArgsConstructor
    public static class PushResult {
        private boolean success;
        private int batchIndex;
        private String responseBody;
        private int statusCode;
        private String errorMessage;
    }
}
