package com.vortex.mapping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据映射实体
 */
@Data
@TableName("data_mapping")
public class DataMappingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessId;

    private String platformCode;

    private String platformId;

    private String status; // SUCCESS, FAILED

    private String errorCode;

    private String errorMessage;

    private String runId;

    private LocalDateTime mappedAt;
}
