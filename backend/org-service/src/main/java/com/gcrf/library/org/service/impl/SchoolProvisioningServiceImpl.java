package com.gcrf.library.org.service.impl;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.tenant.PerSchoolFlywayService;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgNodeService;
import com.gcrf.library.org.service.SchoolProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * 学校 Provisioning 服务实现
 *
 * <p>事务边界说明：
 * <ul>
 *   <li>{@code createSchool} 本身 <strong>不加</strong> {@code @Transactional}，作为外层协调方法。</li>
 *   <li>内部静态 Spring bean {@code OrgNodeTxHelper} 加 {@code @Transactional}，
 *       负责在单一事务内完成：创建 org_node、派生 schema 名、CREATE SCHEMA、回写 tenant_schema，
 *       事务提交后返回 schema 名和 entity。</li>
 *   <li>事务提交后再调用 {@code PerSchoolFlywayService.migrateSchool}，
 *       Flyway 使用独立连接，可以看到已提交的 schema，不存在连接争用问题。</li>
 *   <li>最后向 {@code <schema>.school_meta} 写入种子行（自动提交）。</li>
 * </ul>
 *
 * <p>必须通过独立 Spring bean（{@code OrgNodeTxHelper}）来封装事务方法，
 * 因为 Spring AOP 无法拦截同一 bean 内部的方法调用（self-invocation 问题）。
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolProvisioningServiceImpl implements SchoolProvisioningService {

    private final OrgNodeTxHelper txHelper;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    /**
     * 端到端创建学校。此方法不持有事务；事务由 {@link OrgNodeTxHelper#provisionOrgNode} 管理。
     * Flyway 迁移和种子写入在事务提交后独立执行。
     */
    @Override
    public OrgNodeVO createSchool(SchoolCreateDTO dto) {
        // Steps 1-4: committed atomically inside a @Transactional Spring proxy call
        OrgNodeTxHelper.ProvisionResult result = txHelper.provisionOrgNode(dto);
        String schema = result.schema();
        OrgNode entity = result.entity();

        // Step 5: run per-school Flyway migration (opens its own connection, safe after commit)
        new PerSchoolFlywayService(dataSource).migrateSchool(schema);

        // Step 6: seed school_meta with school code + name (auto-committed)
        jdbc.update(
            "INSERT INTO " + schema + ".school_meta (school_code, school_name) VALUES (?, ?)",
            dto.getCode(), dto.getName());

        log.info("school provisioned: id={}, schema={}, code={}", entity.getId(), schema, dto.getCode());
        return OrgNodeVO.from(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner transaction helper — must be a separate Spring bean so that
    // @Transactional is applied via AOP proxy (self-invocation bypass).
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 事务辅助 bean，在单一事务内完成 org_node 创建、schema 创建和 tenant_schema 回写。
     */
    @Service
    @RequiredArgsConstructor
    public static class OrgNodeTxHelper {

        private final OrgNodeService orgService;
        private final OrgNodeMapper mapper;
        private final JdbcTemplate jdbc;

        /**
         * 在事务内完成：
         * <ol>
         *   <li>创建 SCHOOL 类型的 org_node</li>
         *   <li>派生 schema 名称（school_&lt;id 6位补零&gt;）</li>
         *   <li>CREATE SCHEMA（DDL 在 PG 中参与事务）</li>
         *   <li>回写 org_node.tenant_schema</li>
         * </ol>
         *
         * @param dto 学校创建请求
         * @return 包含 schema 名称和已更新实体的结果对象
         */
        @Transactional(rollbackFor = Exception.class)
        public ProvisionResult provisionOrgNode(SchoolCreateDTO dto) {
            // 1. Create org node of type SCHOOL
            OrgNodeCreateDTO nodeDto = new OrgNodeCreateDTO();
            nodeDto.setParentId(dto.getParentId());
            nodeDto.setType("SCHOOL");
            nodeDto.setName(dto.getName());
            nodeDto.setCode(dto.getCode());
            OrgNodeVO created = orgService.create(nodeDto);

            // 2. Derive schema name: school_<id zero-padded to 6 digits>
            String schema = "school_" + String.format("%06d", created.getId());

            // Guard against schema name injection (should never happen given IdType.AUTO)
            if (!schema.matches("^school_\\d+$")) {
                throw new BusinessException("derived schema name is invalid: " + schema);
            }

            // 3. Create the schema in PostgreSQL (DDL participates in the current transaction)
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schema);

            // 4. Update org_node.tenant_schema = schema
            OrgNode entity = mapper.selectById(created.getId());
            entity.setTenantSchema(schema);
            mapper.updateById(entity);

            return new ProvisionResult(schema, entity);
        }

        /** 承载 provisionOrgNode 的两个返回值的不可变传输对象。 */
        public record ProvisionResult(String schema, OrgNode entity) {}
    }
}
