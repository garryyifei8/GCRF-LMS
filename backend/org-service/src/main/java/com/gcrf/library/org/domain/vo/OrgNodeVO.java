package com.gcrf.library.org.domain.vo;

import com.gcrf.library.org.domain.entity.OrgNode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织节点响应 VO
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
public class OrgNodeVO {

    private Long id;

    private Long parentId;

    private String type;

    private String name;

    private String code;

    private String path;

    private String tenantSchema;

    private String status;

    private String metadata;

    private LocalDateTime createdAt;

    /**
     * 将实体转换为 VO
     */
    public static OrgNodeVO from(OrgNode entity) {
        if (entity == null) {
            return null;
        }
        OrgNodeVO vo = new OrgNodeVO();
        vo.id = entity.getId();
        vo.parentId = entity.getParentId();
        vo.type = entity.getType();
        vo.name = entity.getName();
        vo.code = entity.getCode();
        vo.path = entity.getPath();
        vo.tenantSchema = entity.getTenantSchema();
        vo.status = entity.getStatus();
        vo.metadata = entity.getMetadata();
        vo.createdAt = entity.getCreatedAt();
        return vo;
    }
}
