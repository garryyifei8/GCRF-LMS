package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 组织节点创建请求 DTO
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class OrgNodeCreateDTO {

    private Long parentId;

    @NotBlank(message = "type cannot be blank")
    @Pattern(regexp = "^(REGION|DISTRICT|SCHOOL|SUB_SCHOOL|BRANCH|STAGE|GRADE|CLASS)$",
             message = "type must be one of: REGION, DISTRICT, SCHOOL, SUB_SCHOOL, BRANCH, STAGE, GRADE, CLASS")
    private String type;

    @NotBlank(message = "name cannot be blank")
    private String name;

    @NotBlank(message = "code cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "code must be alphanumeric, dash, or underscore")
    private String code;

    private String metadata;
}
