package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 学校创建请求 DTO
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class SchoolCreateDTO {

    @NotNull(message = "parentId cannot be null")
    private Long parentId; // REGION or DISTRICT id

    @NotBlank(message = "name cannot be blank")
    private String name;

    @NotBlank(message = "code cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "code must be alphanumeric, dash, or underscore")
    private String code;
}
