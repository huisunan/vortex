package com.vortex.node.extract;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JSONPath 结果提取器 - 使用 JSONPath 从响应中提取字段
 * 
 * 配置项：
 * - statusPath: 状态字段的 JSONPath
 * - platformIdPath: 平台 ID 的 JSONPath
 * - batchIdPath: 批次 ID 的 JSONPath（异步模式）
 * - errorCodePath: 错误码的 JSONPath
 * - errorMessagePath: 错误信息的 JSONPath
 */
@Component
public class JsonPathResultExtractor implements ResultExtractor {

    @Override
    public String type() {
        return "jsonpath";
    }

    @Override
    public ExtractResult extract(String responseBody, Map<String, Object> config) {
        if (responseBody == null || responseBody.isBlank()) {
            ExtractResult result = new ExtractResult();
            result.setSuccess(false);
            result.setErrorMessage("Empty response body");
            return result;
        }

        try {
            ReadContext ctx = JsonPath.parse(responseBody);
            
            String statusPath = (String) config.get("statusPath");
            String platformIdPath = (String) config.get("platformIdPath");
            String batchIdPath = (String) config.get("batchIdPath");
            String errorCodePath = (String) config.get("errorCodePath");
            String errorMessagePath = (String) config.get("errorMessagePath");

            ExtractResult result = new ExtractResult();
            result.setRawResponse(responseBody);

            // 提取状态
            if (statusPath != null && !statusPath.isBlank()) {
                result.setStatus(ctx.read(statusPath, String.class));
            }

            // 提取平台 ID
            if (platformIdPath != null && !platformIdPath.isBlank()) {
                result.setPlatformId(ctx.read(platformIdPath, String.class));
            }

            // 提取批次 ID（异步模式）
            if (batchIdPath != null && !batchIdPath.isBlank()) {
                result.setBatchId(ctx.read(batchIdPath, String.class));
            }

            // 提取错误码
            if (errorCodePath != null && !errorCodePath.isBlank()) {
                result.setErrorCode(ctx.read(errorCodePath, String.class));
            }

            // 提取错误信息
            if (errorMessagePath != null && !errorMessagePath.isBlank()) {
                result.setErrorMessage(ctx.read(errorMessagePath, String.class));
            }

            // 判断是否成功
            if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
                result.setSuccess(true);
            } else if (result.getPlatformId() != null && !result.getPlatformId().isBlank()) {
                result.setSuccess(true);
                if (result.getStatus() == null) {
                    result.setStatus("SUCCESS");
                }
            } else {
                result.setSuccess(false);
                if (result.getStatus() == null) {
                    result.setStatus("FAILED");
                }
            }

            return result;

        } catch (Exception e) {
            ExtractResult result = new ExtractResult();
            result.setSuccess(false);
            result.setErrorMessage("Failed to parse response with JSONPath: " + e.getMessage());
            result.setRawResponse(responseBody);
            return result;
        }
    }
}
