package com.gcrf.library.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.org.domain.entity.OrgNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 组织节点 Mapper
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Mapper
public interface OrgNodeMapper extends BaseMapper<OrgNode> {

    /**
     * 查询指定根路径下的所有子树节点
     *
     * @param rootPath 根节点的 ltree 路径
     * @return 子树中的所有节点
     */
    @Select("SELECT * FROM org_node WHERE path <@ #{rootPath}::ltree ORDER BY path")
    List<OrgNode> findSubtree(@Param("rootPath") String rootPath);

    /**
     * 查询指定父节点的所有直属子节点
     *
     * @param parentId 父节点 ID
     * @return 子节点列表
     */
    @Select("SELECT * FROM org_node WHERE parent_id = #{parentId} ORDER BY id")
    List<OrgNode> findByParent(@Param("parentId") Long parentId);

    /**
     * 查询所有根节点
     *
     * @return 根节点列表
     */
    @Select("SELECT * FROM org_node WHERE parent_id IS NULL ORDER BY id")
    List<OrgNode> findRoots();

    /**
     * 移动子树：将所有 path 以 oldPath 开头的节点改为以 newPath 开头
     *
     * @param oldPath 旧的 ltree 路径
     * @param newPath 新的 ltree 路径
     * @return 更新的行数
     */
    @Update("""
        UPDATE org_node
           SET path = (#{newPath}::ltree || subpath(path, nlevel(#{oldPath}::ltree)))
         WHERE path <@ #{oldPath}::ltree
        """)
    int moveSubtree(@Param("oldPath") String oldPath, @Param("newPath") String newPath);
}
