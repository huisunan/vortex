package com.vortex.node.fetch.entity;

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
@TableName("business_record")
public class BusinessRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("business_id")
    private String businessId;

    @TableField("payload")
    private String payload;
}
