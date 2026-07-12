# StarRocks Backend Deployment

This contract targets cBioPortal `v7.0.5` and StarRocks `3.5.19`. It is independent of any RADIANT catalog, database, or study schema.

## Contract files

- `src/main/resources/db-scripts/starrocks/schema.sql`: 51 normalized/reference tables.
- `src/main/resources/db-scripts/starrocks/derived.sql`: 12 derived read contracts.
- `src/test/resources/starrocks/seed.sql`: deterministic two-study integration fixture.
- `docs/starrocks-schema-contract.tsv`: generated column, key, distribution, nullability, and fixture inventory.
- `docs/starrocks-mapper-table-inventory.tsv`: generated mapper-to-table coverage.
- `docs/starrocks-mapper-statement-inventory.tsv`: generated statement-level backend/loading, expanded table lineage, test-reference, and StarRocks verification status.
- `docs/starrocks-frontend-api-inventory.tsv`: generated public/internal API operations used by the pinned frontend, including production/test classification and source call sites.

Every table uses the StarRocks OLAP duplicate-key model, hash distribution, one bucket, and replication `1`. Those topology values are intentionally suitable for tests and single-BE developer deployments. Production sizing must revise bucket and replication properties for its BE count and workload before applying the DDL.

## Clean bootstrap

The schema scripts deliberately drop and recreate their owned tables. This is the documented idempotency model for the foundation phase: rerunning the complete bootstrap produces the same empty contract, and `--with-fixture` then produces the same deterministic test data.

```bash
export STARROCKS_HOST=127.0.0.1
export STARROCKS_PORT=9030
export STARROCKS_USER=root
export STARROCKS_PASSWORD=''
export STARROCKS_DATABASE=cbioportal
scripts/bootstrap_starrocks.sh --with-fixture
```

The script requires a MySQL-compatible command-line client. Override its path with `MYSQL_CLIENT` when it is not named `mysql`.

Do not run this clean-recreate bootstrap against a database containing data that must be preserved.

## Complete local deployment

The local stack uses separate official StarRocks FE and BE `3.5.19` images, the cBioPortal executable JAR with embedded frontend `v7.0.5`, session-service `0.6.1`, and MongoDB `7.0`. It requires Docker with Compose v2; Maven, Java, Node, and a host MySQL client are not required for this path.

```bash
scripts/starrocks/start_local_stack.sh
```

The command builds cBioPortal, starts the complete stack, waits for schema-aware readiness, loads the deterministic non-PHI fixture, and runs the HTTP deployment smoke suite. The portal is then available at `http://localhost:8080`.

The local initializer drops and recreates its owned schema on every start. It must not be copied into a shared or production deployment. FE metadata is kept under the ignored `dev/starrocks/.data` host directory so the FE can enforce its free-space startup check even when Docker Desktop's internal disk is constrained. Destructive fixture BE storage uses a 2 GB tmpfs and is never persistent. Remove all local containers, volumes, and FE metadata with:

```bash
scripts/starrocks/stop_local_stack.sh
```

## Readiness contract

`GET /api/health/starrocks` is available without authentication only when `columnstore.backend=starrocks`. It returns HTTP `200` only when all of these conditions hold:

- FE accepts a query and returns `current_version()`;
- the selected backend is `starrocks`;
- `schema_migrations` reports the configured required migration, currently `1`;
- all 63 normalized and derived contract tables exist.

Connection failure, migration mismatch, or any missing table returns HTTP `503` with the failed contract fields. Orchestrators must use this endpoint for readiness, not the generic process-liveness endpoint.

## Production baseline

Use `dev/starrocks/application.production.properties.example` as the application configuration template. `STARROCKS_HOST`, `STARROCKS_PORT`, and `STARROCKS_DATABASE` are required so a missing deployment value cannot silently select a generic FE port. Credentials must be supplied by the deployment secret manager. The template requires TLS identity verification, bounded Hikari pool settings, a 30-second statement timeout, and migration-aware readiness.

Migration `1` can be established only on a new empty database:

```bash
export STARROCKS_HOST=starrocks-fe.example.org
export STARROCKS_PORT=9030
export STARROCKS_USER=deployment_admin
export STARROCKS_PASSWORD='from-secret-manager'
export STARROCKS_DATABASE=cbioportal
scripts/migrate_starrocks.sh --baseline-empty
```

The migration command no-ops when version `1` is already present and refuses every non-empty unversioned database. It never applies the destructive local fixture. Future schema changes must add a new ordered migration and advance `starrocks.readiness.required-migration` in the same release.

Create a dedicated application identity after the baseline. It needs read access to all contract tables and write access only to cBioPortal's local user/token tables:

```sql
CREATE USER 'cbioportal'@'%' IDENTIFIED BY '<secret>';
GRANT SELECT ON ALL TABLES IN DATABASE cbioportal TO USER 'cbioportal'@'%';
GRANT INSERT, DELETE ON TABLE cbioportal.users, cbioportal.authorities, cbioportal.data_access_tokens TO USER 'cbioportal'@'%';
```

Production FE and BE sizing, replication, buckets, storage, TLS certificates, backups, and monitoring remain deployment-specific. The checked-in one-bucket, replication-one DDL is the executable application contract and must be revised through reviewed migrations for a multi-BE production topology.

## Release gates

After starting the stack, run:

```bash
scripts/starrocks/run_deployment_smoke.py http://localhost:8080
scripts/starrocks/run_frontend_e2e.sh
scripts/starrocks/run_performance_budget.py http://localhost:8080
scripts/starrocks/verify_no_clickhouse_runtime.sh
```

To compare a compatibility ClickHouse deployment with the same logical fixture against StarRocks:

```bash
scripts/starrocks/run_api_parity.py \
  --reference-url http://clickhouse-portal:8080 \
  --starrocks-url http://starrocks-portal:8080
```

The parity runner canonicalizes ordering and build metadata. Any remaining response difference requires a checked-in exception with endpoint, field, owner, rationale, and expiry condition; this release has no approved exceptions.

The fixture latency budgets are in `scripts/starrocks/performance-budgets.json`. They are regression tripwires, not production capacity claims. Pilot cohort load tests must set production budgets before launch.

## Rollback

StarRocks and ClickHouse backend selection is configuration-only and mutually exclusive. To roll back an application release:

1. stop new cBioPortal traffic;
2. point the application secret/configuration back to the preserved ClickHouse JDBC URL and driver;
3. set `columnstore.backend=clickhouse`;
4. redeploy the previous known-good application image;
5. verify `/api/health` and the release smoke suite before restoring traffic.

Do not run either clean-bootstrap script during rollback. StarRocks data and migration history remain intact for diagnosis or a later forward recovery. Database restore procedures must be tested independently by the deployment owner.

## Freshness gates

Regenerate the schema and mapper-table inventories after changing DDL, fixture rows, or mapper table references:

```bash
scripts/generate_starrocks_schema_inventory.py
python3 -m unittest scripts/test_generate_starrocks_schema_inventory.py
scripts/generate_starrocks_schema_inventory.py --check
```

Regenerate the frontend API inventory from an exact checkout of `cbioportal-frontend` `v7.0.5`:

```bash
npm ci --prefix scripts/starrocks-inventory --ignore-scripts
CBIOPORTAL_FRONTEND_ROOT=../cbioportal-frontend-v7.0.5 \
  scripts/generate_starrocks_frontend_api_inventory.sh
CBIOPORTAL_FRONTEND_ROOT=../cbioportal-frontend-v7.0.5 \
  scripts/generate_starrocks_frontend_api_inventory.sh --check
```

The generator rejects any frontend checkout other than commit `5eab200650ecc2f111b94fea31f56a2f50a6ad22`, unknown client methods, duplicate OpenAPI operation IDs, and stale output.

The statement inventory is also a gap ledger. All 236 statements loaded in StarRocks mode have a statically traceable real-StarRocks test reference; zero rows are `UNVERIFIED`.

`StarrocksSchemaContractTest` starts the official FE and BE `3.5.19` images, applies the complete schema and fixture twice, checks all 63 tables and edge-case row counts, and rejects ClickHouse dialect tokens.

## Domain coverage

StarRocks mode currently has concrete repositories for cancer studies, patients, samples, clinical attributes, clinical data, clinical events, treatments, mutation retrieval, alteration aggregates, genomic-data aggregates, generic assays, and co-expression.

The implemented surface includes study metadata and resources, patient/sample counts, sample lists, clinical ID/summary/detailed projections, clinical counts and enrichment inputs, event counts, patient timeline projections, event-derived survival data, patient/sample treatment reports, mutation ID/summary/detailed/meta projections, mutation/CNA/structural-variant gene aggregates, profiled and gene-panel enrichment counts, molecular-profile sample counts, CNA and mutation chart counts, numeric genomic bins, generic-assay counts/bins/metadata/profile discovery, co-expression scoring, and stable sorting and pagination. Co-expression computes average-tie ranks and correlation inside StarRocks, returning one aggregate row per gene without depending on ClickHouse array functions or materializing the full pair set in application memory.

The Study View sample universe supports study, explicit sample, case-list, molecular-profile, clinical, clinical-event, custom-data, patient-treatment, sample-treatment, treatment-group, treatment-target, gene, mutation, CNA/numeric molecular-value, structural-variant, generic-assay value, and generic-assay selection filters. Custom-data filters preserve precomputed sample inclusion and explicit/missing `NA` behavior. Treatment targets use the cBioPortal comma-delimited value contract; agent and agent-class filters remain exact matches. Generic-assay filters preserve sample-level and patient-level propagation and explicit/missing `NA` behavior. The public structural-variant fetch mapper uses portable bound list parameters for both MySQL and StarRocks.

All column-store domain repository interfaces now have concrete StarRocks implementations. Backend selection is fail-fast and exclusive: `columnstore.backend=starrocks` loads the StarRocks repositories, while the default `clickhouse` value continues to load only ClickHouse repositories. Generated endpoint and statement inventories, schema-aware readiness, deployment smoke, frontend Playwright, latency budgets, API parity tooling, and no-ClickHouse runtime checks are release gates.
