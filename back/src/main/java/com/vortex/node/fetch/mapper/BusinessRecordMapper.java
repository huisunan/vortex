package com.vortex.node.fetch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vortex.node.fetch.entity.BusinessRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BusinessRecordMapper extends BaseMapper<BusinessRecordEntity> {

    @Select("""
            <script>
            select id, business_id, payload
            from business_record
            where business_id in
            <foreach collection='businessIds' item='businessId' open='(' separator=',' close=')'>
              #{businessId}
            </foreach>
            </script>
            """)
    List<BusinessRecordEntity> selectByBusinessIds(@Param("businessIds") List<String> businessIds);
}
