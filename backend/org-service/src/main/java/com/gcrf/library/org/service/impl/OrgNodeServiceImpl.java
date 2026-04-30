package com.gcrf.library.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.domain.vo.OrgTreeNodeVO;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织节点服务实现
 *
 * <p>Task 9: query methods only.
 * create/update/delete/move are stubbed with {@link UnsupportedOperationException}
 * to be implemented in Tasks 10-13.
 *
 * @author GCRF Team
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgNodeServiceImpl implements OrgNodeService {

    private final OrgNodeMapper mapper;

    /**
     * Allowed parent types per node type.
     * An empty set means only root placement (no parent) is allowed.
     */
    private static final Map<String, Set<String>> ALLOWED_PARENTS = Map.of(
        "REGION",     Set.of(),
        "DISTRICT",   Set.of("REGION"),
        "SCHOOL",     Set.of("REGION", "DISTRICT"),
        "SUB_SCHOOL", Set.of("SCHOOL"),
        "BRANCH",     Set.of("SCHOOL", "SUB_SCHOOL"),
        "STAGE",      Set.of("SCHOOL", "SUB_SCHOOL"),
        "GRADE",      Set.of("SCHOOL", "SUB_SCHOOL", "STAGE"),
        "CLASS",      Set.of("GRADE")
    );

    // ===================== Query methods =====================

    @Override
    public List<OrgTreeNodeVO> findRoots() {
        log.debug("findRoots called");
        return toTreeFlat(mapper.findRoots());
    }

    @Override
    public List<OrgTreeNodeVO> findChildren(Long parentId) {
        log.debug("findChildren: parentId={}", parentId);
        return toTreeFlat(mapper.findByParent(parentId));
    }

    @Override
    public OrgNodeVO findById(Long id) {
        log.debug("findById: id={}", id);
        OrgNode e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException("org node not found: " + id);
        }
        return OrgNodeVO.from(e);
    }

    @Override
    public OrgTreeNodeVO findSubtree(Long rootId) {
        log.debug("findSubtree: rootId={}", rootId);
        OrgNode root = mapper.selectById(rootId);
        if (root == null) {
            throw new BusinessException("org node not found: " + rootId);
        }
        List<OrgNode> all = mapper.findSubtree(root.getPath());
        return assembleTree(all, root.getId());
    }

    // ===================== Mutation stubs (Tasks 10-13) =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgNodeVO create(OrgNodeCreateDTO dto) {
        // 1. Validate type vs parent relationship
        OrgNode parent = null;
        if (dto.getParentId() != null) {
            parent = mapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("parent not found: " + dto.getParentId());
            }
        }
        Set<String> allowedParents = ALLOWED_PARENTS.getOrDefault(dto.getType(), Set.of());
        String parentType = parent == null ? null : parent.getType();
        boolean typeOk = parent == null ? allowedParents.isEmpty()
                                        : allowedParents.contains(parentType);
        if (!typeOk) {
            throw new BusinessException(dto.getType() + " cannot be child of " + parentType);
        }

        // 2. Enforce max depth of 6 levels
        if (parent != null && parent.getPath().split("\\.").length >= 6) {
            throw new BusinessException("max org tree depth (6) exceeded");
        }

        // 3. Enforce unique code
        OrgNode existing = mapper.selectOne(
            new LambdaQueryWrapper<OrgNode>().eq(OrgNode::getCode, dto.getCode()));
        if (existing != null) {
            throw new BusinessException("code already exists: " + dto.getCode());
        }

        // 4. Insert with placeholder path "0", then update path based on auto-generated id
        OrgNode entity = new OrgNode();
        entity.setParentId(dto.getParentId());
        entity.setType(dto.getType());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setPath("0"); // placeholder; replaced immediately after insert to capture id
        entity.setStatus("ACTIVE");
        entity.setMetadata(dto.getMetadata() == null ? "{}" : dto.getMetadata());
        mapper.insert(entity);

        // Build ltree path: root → just the id; child → parent.path + "." + id
        String path = parent == null
            ? String.valueOf(entity.getId())
            : parent.getPath() + "." + entity.getId();
        entity.setPath(path);
        mapper.updateById(entity);

        log.debug("created org node id={} type={} path={}", entity.getId(), entity.getType(), path);
        return OrgNodeVO.from(entity);
    }

    @Override
    public OrgNodeVO update(Long id, OrgNodeUpdateDTO dto) {
        throw new UnsupportedOperationException("implemented in Task 11");
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException("implemented in Task 12");
    }

    @Override
    public OrgNodeVO move(Long id, Long newParentId) {
        throw new UnsupportedOperationException("implemented in Task 13");
    }

    // ===================== Private helpers =====================

    /**
     * Converts a flat list of {@link OrgNode} entities to a flat list of {@link OrgTreeNodeVO}.
     * No parent-child wiring; children list remains empty.
     * Used by findRoots and findChildren which return a peer-level list.
     */
    private List<OrgTreeNodeVO> toTreeFlat(List<OrgNode> nodes) {
        List<OrgTreeNodeVO> result = new ArrayList<>(nodes.size());
        for (OrgNode n : nodes) {
            result.add(toVO(n));
        }
        return result;
    }

    /**
     * Assembles a tree structure from a flat list of {@link OrgNode} entities.
     * All nodes in the list are expected to belong to the subtree rooted at {@code rootId}.
     *
     * @param nodes  all nodes in the subtree (may include the root itself)
     * @param rootId the ID of the desired tree root
     * @return root {@link OrgTreeNodeVO} with children populated, or null if root not in list
     */
    private OrgTreeNodeVO assembleTree(List<OrgNode> nodes, Long rootId) {
        Map<Long, OrgTreeNodeVO> byId = new HashMap<>(nodes.size() * 2);
        OrgTreeNodeVO rootVo = null;
        for (OrgNode n : nodes) {
            OrgTreeNodeVO v = toVO(n);
            byId.put(v.getId(), v);
            if (v.getId().equals(rootId)) {
                rootVo = v;
            }
        }
        for (OrgTreeNodeVO v : byId.values()) {
            if (v.getId().equals(rootId)) {
                continue;
            }
            OrgTreeNodeVO parent = byId.get(v.getParentId());
            if (parent != null) {
                parent.getChildren().add(v);
            }
        }
        return rootVo;
    }

    /** Maps a single {@link OrgNode} entity to an {@link OrgTreeNodeVO} (children not wired). */
    private OrgTreeNodeVO toVO(OrgNode n) {
        OrgTreeNodeVO v = new OrgTreeNodeVO();
        v.setId(n.getId());
        v.setParentId(n.getParentId());
        v.setType(n.getType());
        v.setName(n.getName());
        v.setCode(n.getCode());
        v.setPath(n.getPath());
        v.setTenantSchema(n.getTenantSchema());
        v.setStatus(n.getStatus());
        return v;
    }
}
