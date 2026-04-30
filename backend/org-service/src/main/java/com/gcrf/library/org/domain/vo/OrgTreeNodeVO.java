package com.gcrf.library.org.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织树节点响应 VO
 * 用于组织结构树形展示
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class OrgTreeNodeVO {

    private Long id;

    private Long parentId;

    private String type;

    private String name;

    private String code;

    private String path;

    private String tenantSchema;

    private String status;

    private List<OrgTreeNodeVO> children = new ArrayList<>();
}
