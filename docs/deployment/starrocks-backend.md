# StarRocks Backend Bootstrap

This contract targets cBioPortal `v7.0.5` and StarRocks `3.5.19`. It is independent of any RADIANT catalog, database, or study schema.

## Contract files

- `src/main/resources/db-scripts/starrocks/schema.sql`: 51 normalized/reference tables.
- `src/main/resources/db-scripts/starrocks/derived.sql`: 12 derived read contracts.
- `src/test/resources/starrocks/seed.sql`: deterministic two-study integration fixture.
- `docs/starrocks-schema-contract.tsv`: generated column, key, distribution, nullability, and fixture inventory.
- `docs/starrocks-mapper-table-inventory.tsv`: generated mapper-to-table coverage.

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

Do not run this clean-recreate bootstrap against a database containing data that must be preserved. Incremental production migrations and readiness enforcement are delivered in the deployment PR after the repository ports stabilize the final contracts.

## Freshness gates

Regenerate both checked inventories after changing DDL, fixture rows, or mapper table references:

```bash
scripts/generate_starrocks_schema_inventory.py
scripts/generate_starrocks_schema_inventory.py --check
```

`StarrocksSchemaContractTest` starts the official FE and BE `3.5.19` images, applies the complete schema and fixture twice, checks all 63 tables and edge-case row counts, and rejects ClickHouse dialect tokens.

## Domain coverage

StarRocks mode currently has concrete repositories for cancer studies, patients, samples, clinical attributes, clinical data, clinical events, treatments, mutation retrieval, alteration aggregates, genomic-data aggregates, generic assays, and co-expression.

The implemented surface includes study metadata and resources, patient/sample counts, sample lists, clinical ID/summary/detailed projections, clinical counts and enrichment inputs, event counts, patient timeline projections, event-derived survival data, patient/sample treatment reports, mutation ID/summary/detailed/meta projections, mutation/CNA/structural-variant gene aggregates, profiled and gene-panel enrichment counts, molecular-profile sample counts, CNA and mutation chart counts, numeric genomic bins, generic-assay counts/bins/metadata/profile discovery, co-expression scoring, and stable sorting and pagination. Co-expression computes average-tie ranks and correlation inside StarRocks, returning one aggregate row per gene without depending on ClickHouse array functions or materializing the full pair set in application memory.

The Study View sample universe supports study, explicit sample, case-list, molecular-profile, clinical, clinical-event, custom-data, patient-treatment, sample-treatment, treatment-group, treatment-target, gene, mutation, CNA/numeric molecular-value, structural-variant, generic-assay value, and generic-assay selection filters. Custom-data filters preserve precomputed sample inclusion and explicit/missing `NA` behavior. Treatment targets use the cBioPortal comma-delimited value contract; agent and agent-class filters remain exact matches. Generic-assay filters preserve sample-level and patient-level propagation and explicit/missing `NA` behavior. The public structural-variant fetch mapper uses portable bound list parameters for both MySQL and StarRocks.

All column-store domain repository interfaces now have concrete StarRocks implementations. Backend selection is fail-fast and exclusive: `columnstore.backend=starrocks` loads the StarRocks repositories, while the default `clickhouse` value continues to load only ClickHouse repositories. Generated endpoint and statement coverage inventories plus deployment end-to-end gates remain required before this branch can be treated as production-ready.
