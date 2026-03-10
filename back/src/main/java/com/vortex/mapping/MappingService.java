package com.vortex.mapping;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据映射服务 - 维护内部 ID 与平台 ID 的映射关系
 * 
 * 核心约束：
 * - 唯一键：businessId + platformCode
 * - 幂等写入：相同 businessId + platformCode 只保留第一条成功映射
 * - 支持查询：按内部 ID 查平台映射，按平台 ID 反查
 */
@Service
public class MappingService {

    private final JdbcTemplate jdbcTemplate;

    public MappingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存成功映射（幂等）
     * 
     * @param businessId 内部业务 ID
     * @param platformCode 目标平台编码
     * @param platformId 平台返回的唯一 ID
     * @param runId 运行 ID
     * @return 是否成功保存（false 表示已存在相同映射）
     */
    @Transactional
    public boolean saveSuccess(String businessId, String platformCode, String platformId, String runId) {
        String sql = """
            INSERT INTO data_mapping (business_id, platform_code, platform_id, run_id, status, mapped_at)
            VALUES (?, ?, ?, ?, 'SUCCESS', ?)
            ON DUPLICATE KEY UPDATE
                platform_id = IF(status != 'SUCCESS', VALUES(platform_id), platform_id),
                status = IF(status != 'SUCCESS', 'SUCCESS', status),
                run_id = IF(status != 'SUCCESS', VALUES(run_id), run_id),
                mapped_at = IF(status != 'SUCCESS', VALUES(mapped_at), mapped_at)
            """;
        
        int rows = jdbcTemplate.update(sql, 
            businessId, platformCode, platformId, runId, 
            Timestamp.valueOf(LocalDateTime.now()));
        
        return rows > 0;
    }

    /**
     * 保存失败映射
     */
    @Transactional
    public void saveFailure(String businessId, String platformCode, String errorCode, 
                           String errorMessage, String runId) {
        String sql = """
            INSERT INTO data_mapping (business_id, platform_code, error_code, error_message, run_id, status, mapped_at)
            VALUES (?, ?, ?, ?, ?, 'FAILED', ?)
            ON DUPLICATE KEY UPDATE
                error_code = VALUES(error_code),
                error_message = VALUES(error_message),
                run_id = VALUES(run_id),
                status = 'FAILED',
                mapped_at = VALUES(mapped_at)
            """;
        
        jdbcTemplate.update(sql, businessId, platformCode, errorCode, errorMessage, 
            runId, Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * 查询映射记录
     */
    public Optional<MappingRecord> find(String businessId, String platformCode) {
        String sql = """
            SELECT business_id, platform_code, platform_id, status, error_code, 
                   error_message, run_id, mapped_at
            FROM data_mapping
            WHERE business_id = ? AND platform_code = ?
            """;
        
        try {
            MappingRecord record = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                MappingRecord r = new MappingRecord();
                r.setBusinessId(rs.getString("business_id"));
                r.setPlatformCode(rs.getString("platform_code"));
                r.setPlatformId(rs.getString("platform_id"));
                r.setStatus(rs.getString("status"));
                r.setErrorCode(rs.getString("error_code"));
                r.setErrorMessage(rs.getString("error_message"));
                r.setRunId(rs.getString("run_id"));
                r.setMappedAt(rs.getTimestamp("mapped_at").toLocalDateTime());
                return r;
            }, businessId, platformCode);
            
            return Optional.ofNullable(record);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 按平台 ID 反查
     */
    public Optional<MappingRecord> findByPlatformId(String platformCode, String platformId) {
        String sql = """
            SELECT business_id, platform_code, platform_id, status, error_code,
                   error_message, run_id, mapped_at
            FROM data_mapping
            WHERE platform_code = ? AND platform_id = ?
            """;
        
        try {
            MappingRecord record = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                MappingRecord r = new MappingRecord();
                r.setBusinessId(rs.getString("business_id"));
                r.setPlatformCode(rs.getString("platform_code"));
                r.setPlatformId(rs.getString("platform_id"));
                r.setStatus(rs.getString("status"));
                r.setErrorCode(rs.getString("error_code"));
                r.setErrorMessage(rs.getString("error_message"));
                r.setRunId(rs.getString("run_id"));
                r.setMappedAt(rs.getTimestamp("mapped_at").toLocalDateTime());
                return r;
            }, platformCode, platformId);
            
            return Optional.ofNullable(record);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 批量查询映射状态
     */
    public List<MappingRecord> findByBusinessIdsAndPlatform(String businessIds, String platformCode) {
        String sql = "SELECT business_id, platform_code, platform_id, status, error_code, " +
                   "error_message, run_id, mapped_at " +
                   "FROM data_mapping " +
                   "WHERE business_id IN (" + businessIds + ") AND platform_code = ?";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MappingRecord r = new MappingRecord();
            r.setBusinessId(rs.getString("business_id"));
            r.setPlatformCode(rs.getString("platform_code"));
            r.setPlatformId(rs.getString("platform_id"));
            r.setStatus(rs.getString("status"));
            r.setErrorCode(rs.getString("error_code"));
            r.setErrorMessage(rs.getString("error_message"));
            r.setRunId(rs.getString("run_id"));
            r.setMappedAt(rs.getTimestamp("mapped_at").toLocalDateTime());
            return r;
        }, platformCode);
    }

    /**
     * 映射记录
     */
    @Data
    @NoArgsConstructor
    public static class MappingRecord {
        private String businessId;
        private String platformCode;
        private String platformId;
        private String status; // SUCCESS, FAILED
        private String errorCode;
        private String errorMessage;
        private String runId;
        private LocalDateTime mappedAt;
    }
}
