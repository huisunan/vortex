package com.vortex.mapping;

import com.vortex.mapping.entity.DataMappingEntity;
import com.vortex.mapping.mapper.DataMappingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final DataMappingMapper mapper;

    public MappingService(DataMappingMapper mapper) {
        this.mapper = mapper;
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
        // 先查询是否已存在成功映射
        DataMappingEntity existing = mapper.findByBusinessIdAndPlatform(businessId, platformCode);
        
        if (existing != null && "SUCCESS".equals(existing.getStatus())) {
            // 已存在成功映射，不更新
            return false;
        }
        
        if (existing != null) {
            // 存在但状态不是 SUCCESS，更新
            existing.setPlatformId(platformId);
            existing.setStatus("SUCCESS");
            existing.setRunId(runId);
            existing.setMappedAt(LocalDateTime.now());
            existing.setErrorCode(null);
            existing.setErrorMessage(null);
            mapper.updateById(existing);
            return true;
        } else {
            // 不存在，插入新记录
            DataMappingEntity entity = new DataMappingEntity();
            entity.setBusinessId(businessId);
            entity.setPlatformCode(platformCode);
            entity.setPlatformId(platformId);
            entity.setRunId(runId);
            entity.setStatus("SUCCESS");
            entity.setMappedAt(LocalDateTime.now());
            mapper.insert(entity);
            return true;
        }
    }

    /**
     * 保存失败映射
     */
    @Transactional
    public void saveFailure(String businessId, String platformCode, String errorCode, 
                           String errorMessage, String runId) {
        DataMappingEntity existing = mapper.findByBusinessIdAndPlatform(businessId, platformCode);
        
        if (existing != null) {
            // 更新现有记录
            existing.setStatus("FAILED");
            existing.setErrorCode(errorCode);
            existing.setErrorMessage(errorMessage);
            existing.setRunId(runId);
            existing.setMappedAt(LocalDateTime.now());
            existing.setPlatformId(null);
            mapper.updateById(existing);
        } else {
            // 插入新记录
            DataMappingEntity entity = new DataMappingEntity();
            entity.setBusinessId(businessId);
            entity.setPlatformCode(platformCode);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
            entity.setRunId(runId);
            entity.setStatus("FAILED");
            entity.setMappedAt(LocalDateTime.now());
            mapper.insert(entity);
        }
    }

    /**
     * 查询映射记录
     */
    public Optional<MappingRecord> find(String businessId, String platformCode) {
        DataMappingEntity entity = mapper.findByBusinessIdAndPlatform(businessId, platformCode);
        return Optional.ofNullable(toRecord(entity));
    }

    /**
     * 按平台 ID 反查
     */
    public Optional<MappingRecord> findByPlatformId(String platformCode, String platformId) {
        DataMappingEntity entity = mapper.findByPlatformId(platformCode, platformId);
        return Optional.ofNullable(toRecord(entity));
    }

    /**
     * 批量查询映射状态
     */
    public List<MappingRecord> findByBusinessIdsAndPlatform(List<String> businessIds, String platformCode) {
        List<DataMappingEntity> entities = mapper.findByBusinessIdsAndPlatform(businessIds, platformCode);
        return entities.stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    /**
     * 实体转记录
     */
    private MappingRecord toRecord(DataMappingEntity entity) {
        if (entity == null) {
            return null;
        }
        MappingRecord record = new MappingRecord();
        record.setBusinessId(entity.getBusinessId());
        record.setPlatformCode(entity.getPlatformCode());
        record.setPlatformId(entity.getPlatformId());
        record.setStatus(entity.getStatus());
        record.setErrorCode(entity.getErrorCode());
        record.setErrorMessage(entity.getErrorMessage());
        record.setRunId(entity.getRunId());
        record.setMappedAt(entity.getMappedAt());
        return record;
    }

    /**
     * 映射记录
     */
    @lombok.Data
    @lombok.NoArgsConstructor
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
