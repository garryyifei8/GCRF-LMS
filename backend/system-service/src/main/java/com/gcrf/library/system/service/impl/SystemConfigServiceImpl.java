package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.system.entity.SystemConfig;
import com.gcrf.library.system.mapper.SystemConfigMapper;
import com.gcrf.library.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper configMapper;

    @Override
    public Map<String, String> getAllConfig() {
        List<SystemConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigKey));
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfig c : configs) {
            result.put(c.getConfigKey(), c.getConfigValue());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Map<String, String> configs, Long userId) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            SystemConfig existing = configMapper.selectById(entry.getKey());
            if (existing != null) {
                existing.setConfigValue(entry.getValue());
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setUpdatedBy(userId);
                configMapper.updateById(existing);
            } else {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(entry.getKey());
                newConfig.setConfigValue(entry.getValue());
                newConfig.setConfigType("STRING");
                newConfig.setUpdatedAt(LocalDateTime.now());
                newConfig.setUpdatedBy(userId);
                configMapper.insert(newConfig);
            }
        }
        log.info("批量保存系统配置成功, count: {}", configs.size());
    }

    @Override
    public boolean isInitialized() {
        SystemConfig config = configMapper.selectById("initialized");
        return config != null && "true".equalsIgnoreCase(config.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markInitialized() {
        SystemConfig config = configMapper.selectById("initialized");
        if (config != null) {
            config.setConfigValue("true");
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey("initialized");
            newConfig.setConfigValue("true");
            newConfig.setDescription("系统是否已完成初始化");
            newConfig.setConfigType("BOOLEAN");
            newConfig.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(newConfig);
        }
        log.info("系统初始化标记已设置");
    }
}
