package com.vortex.mapping;

import com.vortex.mapping.entity.DataMappingEntity;
import com.vortex.mapping.mapper.DataMappingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MappingServiceTest {

    @Mock
    private DataMappingMapper mapper;

    private MappingService mappingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mappingService = new MappingService(mapper);
    }

    @Test
    void shouldSaveSuccessMapping() {
        // 模拟不存在现有映射
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(null);
        when(mapper.insert(any(DataMappingEntity.class))).thenReturn(1);

        boolean result = mappingService.saveSuccess("A1", "PLATFORM_A", "P-100", "run-1");

        assertThat(result).isTrue();
        verify(mapper).insert(any(DataMappingEntity.class));
    }

    @Test
    void shouldIgnoreDuplicateSuccessForSameBusinessAndPlatform() {
        // 模拟已存在成功映射
        DataMappingEntity existing = new DataMappingEntity();
        existing.setBusinessId("A1");
        existing.setPlatformCode("PLATFORM_A");
        existing.setPlatformId("P-100");
        existing.setStatus("SUCCESS");
        
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(existing);

        boolean result = mappingService.saveSuccess("A1", "PLATFORM_A", "P-200", "run-2");

        assertThat(result).isFalse();
    }

    @Test
    void shouldUpdateFailedMappingToSuccess() {
        // 模拟已存在失败映射
        DataMappingEntity existing = new DataMappingEntity();
        existing.setBusinessId("A1");
        existing.setPlatformCode("PLATFORM_A");
        existing.setStatus("FAILED");
        existing.setErrorCode("ERR_500");
        
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(existing);
        when(mapper.updateById(any(DataMappingEntity.class))).thenReturn(1);

        boolean result = mappingService.saveSuccess("A1", "PLATFORM_A", "P-100", "run-1");

        assertThat(result).isTrue();
        verify(mapper).updateById(any(DataMappingEntity.class));
    }

    @Test
    void shouldSaveFailureMapping() {
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(null);
        when(mapper.insert(any(DataMappingEntity.class))).thenReturn(1);

        mappingService.saveFailure("A1", "PLATFORM_A", "ERR_500", "Server error", "run-1");

        verify(mapper).insert(any(DataMappingEntity.class));
    }

    @Test
    void shouldUpdateExistingMappingToFailure() {
        DataMappingEntity existing = new DataMappingEntity();
        existing.setBusinessId("A1");
        existing.setPlatformCode("PLATFORM_A");
        existing.setStatus("SUCCESS");
        
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(existing);
        when(mapper.updateById(any(DataMappingEntity.class))).thenReturn(1);

        mappingService.saveFailure("A1", "PLATFORM_A", "ERR_500", "Server error", "run-1");

        verify(mapper).updateById(any(DataMappingEntity.class));
    }

    @Test
    void shouldFindMappingByBusinessIdAndPlatform() {
        DataMappingEntity entity = new DataMappingEntity();
        entity.setBusinessId("A1");
        entity.setPlatformCode("PLATFORM_A");
        entity.setPlatformId("P-100");
        entity.setStatus("SUCCESS");

        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(entity);

        Optional<MappingService.MappingRecord> result = mappingService.find("A1", "PLATFORM_A");

        assertThat(result).isPresent();
        assertThat(result.get().getPlatformId()).isEqualTo("P-100");
    }

    @Test
    void shouldReturnEmptyWhenMappingNotFound() {
        when(mapper.findByBusinessIdAndPlatform("A1", "PLATFORM_A")).thenReturn(null);

        Optional<MappingService.MappingRecord> result = mappingService.find("A1", "PLATFORM_A");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindByPlatformId() {
        DataMappingEntity entity = new DataMappingEntity();
        entity.setBusinessId("A1");
        entity.setPlatformCode("PLATFORM_A");
        entity.setPlatformId("P-100");

        when(mapper.findByPlatformId("PLATFORM_A", "P-100")).thenReturn(entity);

        Optional<MappingService.MappingRecord> result = mappingService.findByPlatformId("PLATFORM_A", "P-100");

        assertThat(result).isPresent();
        assertThat(result.get().getBusinessId()).isEqualTo("A1");
    }

    @Test
    void shouldFindByBusinessIdsAndPlatform() {
        DataMappingEntity entity1 = new DataMappingEntity();
        entity1.setBusinessId("A1");
        entity1.setPlatformCode("PLATFORM_A");
        entity1.setPlatformId("P-100");

        DataMappingEntity entity2 = new DataMappingEntity();
        entity2.setBusinessId("A2");
        entity2.setPlatformCode("PLATFORM_A");
        entity2.setPlatformId("P-200");

        when(mapper.findByBusinessIdsAndPlatform(Arrays.asList("A1", "A2"), "PLATFORM_A"))
            .thenReturn(Arrays.asList(entity1, entity2));

        List<MappingService.MappingRecord> result = mappingService.findByBusinessIdsAndPlatform(
            Arrays.asList("A1", "A2"), "PLATFORM_A");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlatformId()).isEqualTo("P-100");
        assertThat(result.get(1).getPlatformId()).isEqualTo("P-200");
    }
}
