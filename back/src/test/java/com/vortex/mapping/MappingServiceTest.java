package com.vortex.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.jdbc.core.RowMapper;

class MappingServiceTest {

    private JdbcTemplate jdbcTemplate;
    private MappingService mappingService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        mappingService = new MappingService(jdbcTemplate);
    }

    @Test
    void shouldSaveSuccessMapping() {
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any()))
            .thenReturn(1);

        boolean result = mappingService.saveSuccess("A1", "PLATFORM_A", "P-100", "run-1");

        assertThat(result).isTrue();
        verify(jdbcTemplate).update(
            any(String.class),
            eq("A1"), eq("PLATFORM_A"), eq("P-100"), eq("run-1"), any()
        );
    }

    @Test
    void shouldIgnoreDuplicateSuccessForSameBusinessAndPlatform() {
        // 第一次保存成功
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any()))
            .thenReturn(1)  // 第一次插入
            .thenReturn(0); // 第二次重复，更新 0 行

        boolean first = mappingService.saveSuccess("A1", "PLATFORM_A", "P-100", "run-1");
        boolean second = mappingService.saveSuccess("A1", "PLATFORM_A", "P-200", "run-2");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void shouldSaveFailureMapping() {
        mappingService.saveFailure("A1", "PLATFORM_A", "ERR_500", "Server error", "run-1");

        verify(jdbcTemplate).update(
            any(String.class),
            eq("A1"), eq("PLATFORM_A"), eq("ERR_500"), eq("Server error"), eq("run-1"), any()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindMappingByBusinessIdAndPlatform() {
        MappingService.MappingRecord record = new MappingService.MappingRecord();
        record.setBusinessId("A1");
        record.setPlatformCode("PLATFORM_A");
        record.setPlatformId("P-100");
        record.setStatus("SUCCESS");

        when(jdbcTemplate.queryForObject(any(String.class), (RowMapper<MappingService.MappingRecord>) any(), eq("A1"), eq("PLATFORM_A")))
            .thenReturn(record);

        Optional<MappingService.MappingRecord> result = mappingService.find("A1", "PLATFORM_A");

        assertThat(result).isPresent();
        assertThat(result.get().getPlatformId()).isEqualTo("P-100");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyWhenMappingNotFound() {
        when(jdbcTemplate.queryForObject(any(String.class), (RowMapper<MappingService.MappingRecord>) any(), eq("A1"), eq("PLATFORM_A")))
            .thenReturn(null);

        Optional<MappingService.MappingRecord> result = mappingService.find("A1", "PLATFORM_A");

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindByPlatformId() {
        MappingService.MappingRecord record = new MappingService.MappingRecord();
        record.setBusinessId("A1");
        record.setPlatformCode("PLATFORM_A");
        record.setPlatformId("P-100");

        when(jdbcTemplate.queryForObject(any(String.class), (RowMapper<MappingService.MappingRecord>) any(), eq("PLATFORM_A"), eq("P-100")))
            .thenReturn(record);

        Optional<MappingService.MappingRecord> result = mappingService.findByPlatformId("PLATFORM_A", "P-100");

        assertThat(result).isPresent();
        assertThat(result.get().getBusinessId()).isEqualTo("A1");
    }
}
