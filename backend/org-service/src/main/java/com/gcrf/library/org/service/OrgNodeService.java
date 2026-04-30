package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;

import java.util.List;

/**
 * 组织节点服务接口
 *
 * <p>Query methods (findRoots, findChildren, findById, findSubtree) are implemented in Task 9.
 * Mutation stubs (create, update, delete, move) throw {@link UnsupportedOperationException}
 * and will be filled in Tasks 10-13.
 *
 * @author GCRF Team
 * @since 2026-04-30
 */
public interface OrgNodeService {

    /**
     * 查询所有根节点（parent_id IS NULL）。
     *
     * @return 根节点列表，不含 children
     */
    List<OrgTreeNodeVO> findRoots();

    /**
     * 查询指定父节点的直属子节点。
     *
     * @param parentId 父节点 ID
     * @return 子节点列表，不含 children
     */
    List<OrgTreeNodeVO> findChildren(Long parentId);

    /**
     * 按 ID 查询单个节点。
     *
     * @param id 节点 ID
     * @return OrgNodeVO（含元数据）
     * @throws com.gcrf.library.common.exception.BusinessException 节点不存在时抛出
     */
    OrgNodeVO findById(Long id);

    /**
     * 查询以指定节点为根的完整子树（含根节点自身）。
     *
     * @param rootId 根节点 ID
     * @return 以 rootId 为根的 OrgTreeNodeVO 树
     * @throws com.gcrf.library.common.exception.BusinessException 节点不存在时抛出
     */
    OrgTreeNodeVO findSubtree(Long rootId);

    /**
     * 创建组织节点（Task 10）。
     */
    OrgNodeVO create(OrgNodeCreateDTO dto);

    /**
     * 更新组织节点名称/状态/元数据（Task 11）。
     */
    OrgNodeVO update(Long id, OrgNodeUpdateDTO dto);

    /**
     * 删除组织节点（叶节点检查，Task 12）。
     */
    void delete(Long id);

    /**
     * 移动节点到新父节点下（含子树 path 重算，Task 13）。
     */
    OrgNodeVO move(Long id, Long newParentId);
}
