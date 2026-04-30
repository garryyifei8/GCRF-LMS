package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组织节点更新请求 DTO
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class OrgNodeUpdateDTO {

    @NotBlank(message = "name cannot be blank")
    private String name;

    private String status; // ACTIVE / INACTIVE

    private String metadata;
}
