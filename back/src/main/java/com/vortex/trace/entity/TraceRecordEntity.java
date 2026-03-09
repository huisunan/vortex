package com.vortex.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("trace_record")
public class TraceRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("run_id")
    private String runId;

    @TableField("business_id")
    private String businessId;

    @TableField("node_id")
    private String nodeId;

    @TableField("status")
    private String status;

    @TableField("detail")
    private String detail;
}
