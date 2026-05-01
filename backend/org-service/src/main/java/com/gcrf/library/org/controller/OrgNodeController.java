package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeMoveDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;
import com.gcrf.library.org.service.OrgNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织节点管理控制器
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Slf4j
@Tag(name = "组织管理", description = "组织结构和节点管理相关接口")
@RestController
@RequestMapping("/api/v1/org/nodes")
@RequiredArgsConstructor
public class OrgNodeController {

    private final OrgNodeService service;

    /**
     * 查询根节点或子节点列表
     */
    @Operation(summary = "查询组织节点列表", description = "查询所有根节点（不指定parentId）或特定父节点的子节点")
    @Parameter(name = "parentId", description = "父节点ID，不指定时查询根节点", required = false)
    @GetMapping
    public Result<List<OrgTreeNodeVO>> list(@RequestParam(required = false) Long parentId) {
        log.info("查询组织节点列表: parentId={}", parentId);
        List<OrgTreeNodeVO> result = parentId == null ? service.findRoots() : service.findChildren(parentId);
        return Result.success(result);
    }

    /**
     * 按 ID 查询单个节点
     */
    @Operation(summary = "查询单个组织节点", description = "根据节点ID查询详细信息")
    @Parameter(name = "id", description = "节点ID", required = true)
    @GetMapping("/{id}")
    public Result<OrgNodeVO> getById(@PathVariable Long id) {
        log.info("查询单个组织节点: id={}", id);
        OrgNodeVO result = service.findById(id);
        return Result.success(result);
    }

    /**
     * 查询完整子树
     */
    @Operation(summary = "查询组织子树", description = "查询以指定节点为根的完整子树结构")
    @Parameter(name = "id", description = "节点ID", required = true)
    @GetMapping("/{id}/subtree")
    public Result<OrgTreeNodeVO> subtree(@PathVariable Long id) {
        log.info("查询组织子树: id={}", id);
        OrgTreeNodeVO result = service.findSubtree(id);
        return Result.success(result);
    }

    /**
     * 创建组织节点
     */
    @Operation(summary = "创建组织节点", description = "创建新的组织节点")
    @PostMapping
    public Result<OrgNodeVO> create(@Valid @RequestBody OrgNodeCreateDTO dto) {
        log.info("创建组织节点: type={}, code={}, name={}, parentId={}",
                 dto.getType(), dto.getCode(), dto.getName(), dto.getParentId());
        OrgNodeVO result = service.create(dto);
        return Result.success(result);
    }

    /**
     * 更新组织节点
     */
    @Operation(summary = "更新组织节点", description = "更新组织节点的名称、状态或元数据")
    @Parameter(name = "id", description = "节点ID", required = true)
    @PutMapping("/{id}")
    public Result<OrgNodeVO> update(@PathVariable Long id,
                                    @Valid @RequestBody OrgNodeUpdateDTO dto) {
        log.info("更新组织节点: id={}, name={}", id, dto.getName());
        OrgNodeVO result = service.update(id, dto);
        return Result.success(result);
    }

    /**
     * 删除组织节点
     */
    @Operation(summary = "删除组织节点", description = "删除组织节点（必须是叶节点）")
    @Parameter(name = "id", description = "节点ID", required = true)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除组织节点: id={}", id);
        service.delete(id);
        return Result.success();
    }

    /**
     * 移动节点
     */
    @Operation(summary = "移动组织节点", description = "移动组织节点到新的父节点下")
    @Parameter(name = "id", description = "节点ID", required = true)
    @PostMapping("/{id}/move")
    public Result<OrgNodeVO> move(@PathVariable Long id,
                                  @Valid @RequestBody OrgNodeMoveDTO dto) {
        log.info("移动组织节点: id={}, newParentId={}", id, dto.getNewParentId());
        OrgNodeVO result = service.move(id, dto.getNewParentId());
        return Result.success(result);
    }
}
