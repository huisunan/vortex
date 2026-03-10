package com.vortex.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vortex.mapping.entity.DataMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 数据映射 Mapper
 */
@Mapper
public interface DataMappingMapper extends BaseMapper<DataMappingEntity> {

    /**
     * 按 businessId + platformCode 查询
     */
    @Select("SELECT * FROM data_mapping WHERE business_id = #{businessId} AND platform_code = #{platformCode}")
    DataMappingEntity findByBusinessIdAndPlatform(@Param("businessId") String businessId, 
                                                   @Param("platformCode") String platformCode);

    /**
     * 按 platformCode + platformId 反查
     */
    @Select("SELECT * FROM data_mapping WHERE platform_code = #{platformCode} AND platform_id = #{platformId}")
    DataMappingEntity findByPlatformId(@Param("platformCode") String platformCode, 
                                        @Param("platformId") String platformId);

    /**
     * 批量查询映射状态
     */
    @Select("""
        <script>
        SELECT * FROM data_mapping 
        WHERE business_id IN 
        <foreach collection='businessIds' item='id' open='(' separator=',' close=')'>
            #{id}
        </foreach>
        AND platform_code = #{platformCode}
        </script>
        """)
    List<DataMappingEntity> findByBusinessIdsAndPlatform(@Param("businessIds") List<String> businessIds,
                                                          @Param("platformCode") String platformCode);
}
