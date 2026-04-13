package com.gcrf.library.reader.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.dto.request.ReaderTypeCreateRequest;
import com.gcrf.library.reader.dto.request.ReaderTypeUpdateRequest;
import com.gcrf.library.reader.dto.response.ReaderTypeVO;
import com.gcrf.library.reader.service.ReaderTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 读者类型管理控制器
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Tag(name = "读者类型管理", description = "读者类型CRUD相关接口")
@RestController
@RequestMapping("/api/v1/readers/types")
@RequiredArgsConstructor
public class ReaderTypeController {

    private final ReaderTypeService readerTypeService;

    /**
     * 获取所有读者类型列表
     */
    @Operation(summary = "获取读者类型列表", description = "获取所有可用的读者类型列表")
    @GetMapping
    public Result<List<ReaderTypeVO>> listAllTypes() {
        log.info("获取读者类型列表");
        List<ReaderTypeVO> result = readerTypeService.listAllTypes();
        return Result.success(result);
    }

    /**
     * 根据ID获取读者类型详情
     */
    @Operation(summary = "根据ID获取读者类型", description = "根据ID查询读者类型详细信息")
    @Parameter(name = "id", description = "读者类型ID", required = true)
    @GetMapping("/{id}")
    public Result<ReaderTypeVO> getTypeById(@PathVariable Long id) {
        log.info("查询读者类型详情: id={}", id);
        ReaderTypeVO result = readerTypeService.getTypeById(id);
        return Result.success(result);
    }

    /**
     * 创建读者类型
     */
    @Operation(summary = "创建读者类型", description = "创建新的读者类型")
    @PostMapping
    public Result<ReaderTypeVO> createType(@Valid @RequestBody ReaderTypeCreateRequest request) {
        log.info("创建读者类型: typeCode={}, typeName={}", request.getTypeCode(), request.getTypeName());
        ReaderTypeVO result = readerTypeService.createType(request);
        return Result.success(result);
    }

    /**
     * 更新读者类型
     */
    @Operation(summary = "更新读者类型", description = "更新读者类型信息")
    @Parameter(name = "id", description = "读者类型ID", required = true)
    @PutMapping("/{id}")
    public Result<ReaderTypeVO> updateType(
            @PathVariable Long id,
            @Valid @RequestBody ReaderTypeUpdateRequest request) {
        log.info("更新读者类型: id={}", id);
        // 确保路径参数和请求体的ID一致
        request.setId(id);
        ReaderTypeVO result = readerTypeService.updateType(request);
        return Result.success(result);
    }

    /**
     * 删除读者类型
     */
    @Operation(summary = "删除读者类型", description = "删除读者类型（逻辑删除）")
    @Parameter(name = "id", description = "读者类型ID", required = true)
    @DeleteMapping("/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        log.info("删除读者类型: id={}", id);
        readerTypeService.deleteType(id);
        return Result.success();
    }
}
