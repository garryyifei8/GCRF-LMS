-- backend/opac-service/src/main/resources/db/migration/region/V002__opac_search_log.sql
CREATE TABLE IF NOT EXISTS search_log (
    id              BIGSERIAL    PRIMARY KEY,
    keyword         TEXT         NOT NULL,
    keyword_lower   TEXT         GENERATED ALWAYS AS (lower(keyword)) STORED,
    client_ip       VARCHAR(64),
    result_count    INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_log_created_at ON search_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_log_keyword_lower ON search_log (keyword_lower);
