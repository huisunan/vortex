package com.vortex.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vortex.trace.entity.TraceRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TraceRecordMapper extends BaseMapper<TraceRecordEntity> {

    @Select("""
            select id, run_id, business_id, node_id, status, detail
            from trace_record
            where business_id = #{businessId}
            order by id asc
            """)
    List<TraceRecordEntity> selectByBusinessId(@Param("businessId") String businessId);
}
