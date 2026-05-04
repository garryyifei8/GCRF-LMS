package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;

/**
 * 学校 Provisioning 服务接口
 *
 * <p>负责端到端创建一所学校：
 * <ol>
 *   <li>在 org_node 表中插入 SCHOOL 类型节点</li>
 *   <li>派生 schema 名称（school_&lt;6位补零id&gt;）</li>
 *   <li>在 PostgreSQL 中创建 schema</li>
 *   <li>回写 org_node.tenant_schema</li>
 *   <li>运行 per-school Flyway 模板（建 5 张核心表）</li>
 *   <li>向 &lt;schema&gt;.school_meta 写入初始元数据行</li>
 * </ol>
 *
 * @author Claude Code
 * @since 2026-04-30
 */
public interface SchoolProvisioningService {

    /**
     * 端到端创建一所学校并完成 schema 初始化。
     *
     * @param dto 学校创建请求
     * @return 已创建且回写了 tenantSchema 的 OrgNodeVO
     */
    OrgNodeVO createSchool(SchoolCreateDTO dto);
}
