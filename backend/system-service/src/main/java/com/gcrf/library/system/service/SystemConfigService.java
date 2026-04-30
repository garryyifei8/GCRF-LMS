package com.gcrf.library.system.service;

import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
public interface SystemConfigService {

    /**
     * 获取所有系统配置（key-value map）
     */
    Map<String, String> getAllConfig();

    /**
     * 批量保存/更新系统配置
     *
     * @param configs  配置 key-value map
     * @param userId   操作人ID
     */
    void saveConfig(Map<String, String> configs, Long userId);

    /**
     * 判断系统是否已完成初始化
     */
    boolean isInitialized();

    /**
     * 标记系统初始化完成
     */
    void markInitialized();
}
