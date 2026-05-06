# ADR-005: Flyway history table per service for shared schemas

**Status**: Accepted
**Date**: 2026-05-06
**Decider**: GCRF 项目组
**Related**: [ADR-001 multi-tenant strategy](ADR-001-multi-tenant-strategy.md)

---

## Context

`gcrf_region` is a single PostgreSQL schema shared by multiple services
(org-service, opac-service, future standard-service / wechat-service). Each
service ships its own Flyway migrations under `db/migration/region/V*.sql`.

By default Flyway records every applied migration in
`gcrf_region.flyway_schema_history`. When two services both ship `V001` (with
different content), the second service starts up, sees `V001` already in the
history table with a different checksum, and fails with
`FlywayValidateException: Migration checksum mismatch`.

A second issue emerged in plan-C1 deploy: `baseline-on-migrate: true` on a
schema that already has tables (created by another service) causes Flyway
to baseline at the latest version and silently skip our own V001 — the
function our service depends on never gets created.

## Decision

For every service that migrates into `gcrf_region`:

1. Set `spring.flyway.table: flyway_schema_history_<service>`
   (e.g. `flyway_schema_history_opac`, `flyway_schema_history_standard`).
2. Set `spring.flyway.baseline-on-migrate: false`.
3. Keep `spring.flyway.create-schemas: true` and `schemas: gcrf_region`.

This means each service tracks its own migration history independently. The
schema itself is shared; the history is private.

## Consequences

### Positive

- Services can ship V001 / V002 / ... independently without checksum collision
- A new service deployed against a populated `gcrf_region` schema runs its
  full migration set (no silent skip)
- Each service's history is auditable separately

### Negative

- Slight visual clutter: `gcrf_region.flyway_schema_history_*` x N tables
- Cross-service ordering is not enforced by Flyway — if service A's V005
  must run before service B's V003, that's a manual coordination problem
  (mitigation: services own disjoint table sets within the schema, so this
  rarely matters)

## Verification

`opac-service` started fresh against a `gcrf_region` schema previously
populated by `org-service`. With `baseline-on-migrate: false` and
`table: flyway_schema_history_opac`, Flyway:

1. Created `flyway_schema_history_opac` (empty)
2. Applied `V001__opac_search_setup.sql` (created `pg_trgm` extension,
   `book_search_mview` with GIN trigram indexes on title/author/isbn,
   btree indexes on classification/school_schema, and
   `refresh_book_search_mview()` function)
3. Recorded V001 in `flyway_schema_history_opac`

`org-service`'s history in `flyway_schema_history` is unaffected.

---

**Last Updated**: 2026-05-06
