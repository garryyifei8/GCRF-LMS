package com.gcrf.library.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 重置用户密码请求DTO
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Data
@Schema(description = "重置用户密码请求")
public class UserPasswordResetRequest {

    @Schema(description = "新密码（不传则重置为默认密码）", example = "Admin1234")
    private String newPassword;
}
