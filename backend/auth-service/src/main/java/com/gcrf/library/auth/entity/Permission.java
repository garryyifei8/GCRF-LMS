package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gcrf_region.auth_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String module;
    private String action;
    private String name;
    private String description;
    @TableField("sort_order")
    private Integer sortOrder;
}
