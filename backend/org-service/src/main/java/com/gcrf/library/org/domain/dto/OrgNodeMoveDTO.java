package com.gcrf.library.org.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织节点移动请求 DTO
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class OrgNodeMoveDTO {

    @NotNull(message = "newParentId cannot be null")
    private Long newParentId;
}
