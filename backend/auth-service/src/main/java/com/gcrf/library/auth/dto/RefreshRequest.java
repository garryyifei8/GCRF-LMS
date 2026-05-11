package com.gcrf.library.auth.dto;

import lombok.Data;

/**
 * Refresh token 请求 DTO — 供 /refresh 和 /logout 端点使用
 *
 * @author GCRF Team
 * @date 2026-05-11
 */
@Data
public class RefreshRequest {
    private String refreshToken;
}
