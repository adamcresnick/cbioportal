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

StarRocks mode currently has concrete repositories for cancer studies, patients, and samples. These adapters cover study metadata and resources, patient/sample counts, sample lists, ID/summary/detailed projections, stable sorting and pagination, and the base Study View sample universe for study, explicit sample, case-list, and molecular-profile selections.

Clinical, event, treatment, mutation, CNA, structural-variant, molecular-value, generic-assay, co-expression, and enrichment filter families remain owned by subsequent domain ports. Passing one of those filters through a base adapter raises `StarrocksUnsupportedStudyViewFilterException`; it is never silently ignored. The remaining domain repository interfaces continue to use the explicit not-implemented fallback until their concrete adapter is merged.
