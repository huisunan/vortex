package com.vortex.node.extract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * 脚本结果提取器 - 使用 JavaScript 脚本提取复杂响应
 * 
 * 适用于：
 * - 嵌套结构复杂的响应
 * - 需要计算/转换的字段
 * - 加密/签名响应
 * 
 * 脚本中可通过 response 变量访问解析后的 JSON 对象
 * 必须返回 ExtractResult 或包含相应字段的 Map
 */
@Component
public class ScriptResultExtractor implements ResultExtractor {

    private final ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public String type() {
        return "script";
    }

    @Override
    public ExtractResult extract(String responseBody, Map<String, Object> config) {
        String script = (String) config.get("script");
        
        if (script == null || script.isBlank()) {
            ExtractResult result = new ExtractResult();
            result.setSuccess(false);
            result.setErrorMessage("Script is required for script extractor");
            return result;
        }

        if (responseBody == null || responseBody.isBlank()) {
            ExtractResult result = new ExtractResult();
            result.setSuccess(false);
            result.setErrorMessage("Empty response body");
            return result;
        }

        try {
            ScriptEngine engine = engineManager.getEngineByName("JavaScript");
            
            // 解析 JSON 响应
            Object response = parseJson(responseBody);
            engine.put("response", response);
            
            // 执行提取脚本
            Object resultObj = engine.eval(script);
            
            // 处理返回结果
            if (resultObj instanceof ExtractResult) {
                return (ExtractResult) resultObj;
            } else if (resultObj instanceof Map) {
                return mapToExtractResult((Map<String, Object>) resultObj, responseBody);
            } else {
                ExtractResult result = new ExtractResult();
                result.setSuccess(false);
                result.setErrorMessage("Script must return ExtractResult or Map");
                result.setRawResponse(responseBody);
                return result;
            }

        } catch (Exception e) {
            ExtractResult result = new ExtractResult();
            result.setSuccess(false);
            result.setErrorMessage("Failed to execute extract script: " + e.getMessage());
            result.setRawResponse(responseBody);
            return result;
        }
    }

    private Object parseJson(String json) throws Exception {
        ScriptEngine engine = engineManager.getEngineByName("JavaScript");
        return engine.eval("(" + json + ")");
    }

    private ExtractResult mapToExtractResult(Map<String, Object> map, String rawResponse) {
        ExtractResult result = new ExtractResult();
        result.setRawResponse(rawResponse);
        
        Object success = map.get("success");
        if (success instanceof Boolean) {
            result.setSuccess((Boolean) success);
        }
        
        Object status = map.get("status");
        if (status instanceof String) {
            result.setStatus((String) status);
        }
        
        Object platformId = map.get("platformId");
        if (platformId instanceof String) {
            result.setPlatformId((String) platformId);
        }
        
        Object batchId = map.get("batchId");
        if (batchId instanceof String) {
            result.setBatchId((String) batchId);
        }
        
        Object errorCode = map.get("errorCode");
        if (errorCode instanceof String) {
            result.setErrorCode((String) errorCode);
        }
        
        Object errorMessage = map.get("errorMessage");
        if (errorMessage instanceof String) {
            result.setErrorMessage((String) errorMessage);
        }
        
        return result;
    }
}
