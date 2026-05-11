package com.gcrf.library.auth.dto;

import com.gcrf.library.auth.entity.Permission;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleDetailVO {
    private RoleVO role;
    private List<Permission> permissions;
}
