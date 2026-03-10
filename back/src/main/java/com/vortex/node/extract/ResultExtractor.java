package com.vortex.node.extract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 推送结果提取器接口 - 从平台响应中提取关键信息
 * 
 * 支持两种模式：
 * - 同步模式：提取状态、平台 ID、错误信息
 * - 异步模式：提取平台批次 ID/任务 ID
 */
public interface ResultExtractor {

    /**
     * 提取器类型
     */
    String type();

    /**
     * 从响应中提取结果
     * 
     * @param responseBody 响应体
     * @param config 提取配置（JSONPath 表达式或脚本）
     * @return 提取结果
     */
    ExtractResult extract(String responseBody, Map<String, Object> config);

    /**
     * 提取结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ExtractResult {
        private boolean success;
        private String status;          // 处理状态：SUCCESS, FAILED, PROCESSING
        private String platformId;      // 平台返回的唯一 ID
        private String batchId;         // 异步模式下的批次 ID/任务 ID
        private String errorCode;       // 错误码
        private String errorMessage;    // 错误信息
        private String rawResponse;     // 原始响应
    }
}
