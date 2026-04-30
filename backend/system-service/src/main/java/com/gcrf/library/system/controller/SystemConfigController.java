package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统参数配置与初始化")
public class SystemConfigController {

    private final SystemConfigService configService;

    /**
     * 获取所有系统配置
     */
    @GetMapping
    @Operation(summary = "获取所有系统配置", description = "以 key-value map 形式返回全部配置项")
    public Result<Map<String, String>> getAll() {
        log.info("获取所有系统配置");
        return Result.success(configService.getAllConfig());
    }

    /**
     * 批量保存/更新系统配置
     */
    @PutMapping
    @Operation(summary = "保存系统配置", description = "批量 upsert 系统配置项")
    public Result<Void> save(@RequestBody Map<String, String> configs) {
        log.info("保存系统配置, count: {}", configs.size());
        configService.saveConfig(configs, null); // TODO: extract userId from JWT context
        return Result.success();
    }

    /**
     * 查询系统是否已初始化
     */
    @GetMapping("/initialized")
    @Operation(summary = "查询初始化状态", description = "返回系统是否已完成初始化设置")
    public Result<Boolean> isInitialized() {
        return Result.success(configService.isInitialized());
    }

    /**
     * 标记系统初始化完成（可附带最终配置）
     */
    @PostMapping("/initialize")
    @Operation(summary = "完成系统初始化", description = "可选地保存最终配置并将 initialized 标记为 true")
    public Result<Void> markInitialized(
            @RequestBody(required = false) Map<String, String> finalConfigs) {
        log.info("标记系统初始化完成");
        if (finalConfigs != null && !finalConfigs.isEmpty()) {
            configService.saveConfig(finalConfigs, null);
        }
        configService.markInitialized();
        return Result.success();
    }
}
