CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE IF NOT EXISTS org_node (
    id              BIGSERIAL    PRIMARY KEY,
    parent_id       BIGINT       REFERENCES org_node(id) ON DELETE RESTRICT,
    type            VARCHAR(30)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  UNIQUE NOT NULL,
    path            LTREE        NOT NULL,
    tenant_schema   VARCHAR(50),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    metadata        JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_org_node_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_org_node_path_gist ON org_node USING GIST (path);
CREATE INDEX IF NOT EXISTS idx_org_node_parent ON org_node(parent_id);
CREATE INDEX IF NOT EXISTS idx_org_node_type ON org_node(type);
CREATE INDEX IF NOT EXISTS idx_org_node_tenant ON org_node(tenant_schema) WHERE tenant_schema IS NOT NULL;

CREATE TABLE IF NOT EXISTS org_node_type (
    code           VARCHAR(30)  PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL,
    parent_types   TEXT[]       NOT NULL DEFAULT '{}',
    display_order  INT          NOT NULL DEFAULT 0
);
