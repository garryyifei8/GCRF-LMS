package com.gcrf.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌请求DTO
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * 待刷新的JWT令牌
     */
    @NotBlank(message = "令牌不能为空")
    private String token;
}
