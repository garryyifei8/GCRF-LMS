package com.gcrf.library.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新用户状态请求DTO
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Data
@Schema(description = "更新用户状态请求")
public class UserStatusRequest {

    @Schema(description = "账号状态：ACTIVE/INACTIVE/LOCKED", example = "ACTIVE")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|LOCKED)$", message = "状态必须是ACTIVE、INACTIVE或LOCKED")
    private String status;
}
