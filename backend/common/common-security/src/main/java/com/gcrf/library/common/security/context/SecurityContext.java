package com.gcrf.library.common.security.context;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 不可变的请求安全上下文 POJO。
 * 由 SecurityContextHolder 持有，由 JWT 过滤器在每次请求时填充。
 *
 * @author GCRF Team
 */
@Value
@Builder
public class SecurityContext {

    Long userId;

    String username;

    /** 租户标识，例如 school_000001，区域级用户为 null */
    String tenant;

    Long tenantId;

    @Builder.Default
    List<String> roles = List.of();

    @Builder.Default
    Scope scope = Scope.SELF;

    /** ltree 路径，例如 /100/200/305/，或 null */
    String orgPath;
}
