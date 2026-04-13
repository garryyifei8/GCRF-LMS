package com.gcrf.library.system.client;

import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 认证服务Feign客户端
 *
 * @author GCRF Team
 * @since 2026-04-13
 */
@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthServiceClient {

    @GetMapping("/users/{userId}/role-ids")
    Result<List<Long>> getUserRoleIds(@PathVariable("userId") Long userId);
}
