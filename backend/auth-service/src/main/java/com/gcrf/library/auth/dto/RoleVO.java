package com.gcrf.library.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String scopeDefault;
    private Boolean isSystem;
    private Integer permissionCount;
    private Integer userCount;
}
