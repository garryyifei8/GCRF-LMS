package com.gcrf.library.common.security.permission;

import java.util.Set;

/**
 * 业务服务自己实现，命中时返回该 user 拥有的 permission code 集合（含缓存）。
 */
public interface PermissionLookup {
    Set<String> lookup(Long userId);
}
