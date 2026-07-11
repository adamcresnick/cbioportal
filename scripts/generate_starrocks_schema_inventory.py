#!/usr/bin/env python3
"""Generate the checked StarRocks schema contract inventory from DDL and fixture SQL."""

from __future__ import annotations

import argparse
import csv
import io
import re
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DDL_PATHS = (
    ROOT / "src/main/resources/db-scripts/starrocks/schema.sql",
    ROOT / "src/main/resources/db-scripts/starrocks/derived.sql",
)
SEED_PATH = ROOT / "src/test/resources/starrocks/seed.sql"
DEFAULT_OUTPUT = ROOT / "docs/starrocks-schema-contract.tsv"
DEFAULT_MAPPER_OUTPUT = ROOT / "docs/starrocks-mapper-table-inventory.tsv"
MAPPER_ROOTS = (
    ROOT / "src/main/resources/org/cbioportal/legacy/persistence/mybatis",
    ROOT / "src/main/resources/org/cbioportal/persistence/mybatisclickhouse",
    ROOT / "src/main/resources/mappers/clickhouse",
    ROOT / "src/main/resources/mappers/starrocks",
)

TABLE_PATTERN = re.compile(
    r"CREATE TABLE\s+(?P<table>\w+)\s*\((?P<body>.*?)\)\s*ENGINE=OLAP\s*"
    r"DUPLICATE KEY\((?P<key>[^)]+)\)\s*"
    r"DISTRIBUTED BY HASH\((?P<distribution>[^)]+)\)\s*BUCKETS\s+(?P<buckets>\d+)",
    re.DOTALL,
)
INSERT_PATTERN = re.compile(
    r"INSERT INTO\s+`?(?P<table>\w+)`?\s*"
    r"(?:\([^;]*?\)\s*)?VALUES\s*(?P<values>.*?);",
    re.DOTALL | re.IGNORECASE,
)
SELECT_INSERT_PATTERN = re.compile(
    r"INSERT INTO\s+`?(?P<table>\w+)`?\s*"
    r"(?:\([^;]*?\)\s*)?"
    r"--\s*fixture-rows:\s*(?P<count>\d+)\s*"
    r"SELECT\b.*?;",
    re.DOTALL | re.IGNORECASE,
)
COLUMN_PATTERN = re.compile(r"^\s*`(?P<column>[^`]+)`\s+(?P<definition>.+?)(?:,)?\s*$")
TABLE_REFERENCE_PATTERN = re.compile(
    r"\b(?:FROM|JOIN)\s+`?(?P<table>[A-Za-z_][A-Za-z0-9_]*)`?", re.IGNORECASE
)
CTE_PATTERN = re.compile(r"\b(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s+AS\s*\(", re.IGNORECASE)


def fixture_row_counts(seed_sql: str) -> Counter[str]:
    counts: Counter[str] = Counter()
    value_matches = list(INSERT_PATTERN.finditer(seed_sql))
    select_matches = list(SELECT_INSERT_PATTERN.finditer(seed_sql))
    declared_insert_count = len(re.findall(r"\bINSERT INTO\b", seed_sql, re.IGNORECASE))
    parsed_insert_count = len(value_matches) + len(select_matches)
    if parsed_insert_count != declared_insert_count:
        raise ValueError(
            f"Parsed {parsed_insert_count} of {declared_insert_count} fixture INSERT statements"
        )
    for match in value_matches:
        counts[match.group("table")] += count_top_level_tuples(match.group("values"))
    for match in select_matches:
        counts[match.group("table")] += int(match.group("count"))
    return counts


def count_top_level_tuples(values_sql: str) -> int:
    count = 0
    depth = 0
    quoted = False
    index = 0
    while index < len(values_sql):
        character = values_sql[index]
        if character == "'":
            if quoted and index + 1 < len(values_sql) and values_sql[index + 1] == "'":
                index += 2
                continue
            quoted = not quoted
        elif not quoted and character == "(":
            if depth == 0:
                count += 1
            depth += 1
        elif not quoted and character == ")":
            depth -= 1
            if depth < 0:
                raise ValueError("Unbalanced fixture tuple")
        index += 1
    if quoted or depth != 0:
        raise ValueError("Unbalanced quote or tuple in fixture VALUES clause")
    return count


def inventory_rows() -> list[dict[str, str | int]]:
    fixture_counts = fixture_row_counts(SEED_PATH.read_text())
    rows: list[dict[str, str | int]] = []
    seen_tables: set[str] = set()
    declared_tables: set[str] = set()
    for ddl_path in DDL_PATHS:
        ddl_sql = ddl_path.read_text()
        declared_tables.update(
            match.group(1)
            for match in re.finditer(r"^CREATE TABLE\s+(\w+)", ddl_sql, re.MULTILINE)
        )
        for table_match in TABLE_PATTERN.finditer(ddl_sql):
            table = table_match.group("table")
            if table in seen_tables:
                raise ValueError(f"Duplicate table contract: {table}")
            seen_tables.add(table)
            key = normalize_identifier_list(table_match.group("key"))
            distribution = normalize_identifier_list(table_match.group("distribution"))
            parsed_column_count = 0
            for line in table_match.group("body").splitlines():
                column_match = COLUMN_PATTERN.match(line)
                if not column_match:
                    continue
                parsed_column_count += 1
                definition = column_match.group("definition").rstrip(",")
                definition = definition.split(" COMMENT ", 1)[0]
                nullable = "NO" if " NOT NULL" in definition else "YES"
                sql_type = definition.replace(" NOT NULL", "").replace(" NULL", "")
                rows.append(
                    {
                        "table": table,
                        "column": column_match.group("column"),
                        "type": sql_type,
                        "nullable": nullable,
                        "key_model": "DUPLICATE KEY",
                        "sort_key": key,
                        "distribution": f"HASH({distribution})/{table_match.group('buckets')}",
                        "fixture_rows": fixture_counts[table],
                    }
                )
            declared_column_count = len(
                re.findall(r"^\s*`[^`]+`\s+", table_match.group("body"), re.MULTILINE)
            )
            if parsed_column_count != declared_column_count:
                raise ValueError(
                    f"Parsed {parsed_column_count} of {declared_column_count} columns for {table}"
                )
    if seen_tables != declared_tables:
        raise ValueError(
            "DDL parser coverage mismatch; "
            f"missing={sorted(declared_tables - seen_tables)}, "
            f"unexpected={sorted(seen_tables - declared_tables)}"
        )
    unknown_fixture_tables = set(fixture_counts) - seen_tables
    if unknown_fixture_tables:
        raise ValueError(f"Fixture references unknown tables: {sorted(unknown_fixture_tables)}")
    return rows


def normalize_identifier_list(value: str) -> str:
    return ",".join(part.strip().strip("`") for part in value.split(","))


def render_inventory() -> str:
    output = io.StringIO()
    fields = (
        "table",
        "column",
        "type",
        "nullable",
        "key_model",
        "sort_key",
        "distribution",
        "fixture_rows",
    )
    writer = csv.DictWriter(output, fields, dialect="excel-tab", lineterminator="\n")
    writer.writeheader()
    writer.writerows(inventory_rows())
    return output.getvalue()


def schema_tables() -> set[str]:
    return {row["table"] for row in inventory_rows()}


def mapper_table_usage() -> dict[str, set[str]]:
    usage: dict[str, set[str]] = {}
    for mapper_root in MAPPER_ROOTS:
        for mapper_path in mapper_root.rglob("*.xml"):
            sql = re.sub(r"<!--.*?-->", " ", mapper_path.read_text(), flags=re.DOTALL)
            sql = re.sub(r"--[^\n]*", " ", sql)
            cte_names = {match.group("name").lower() for match in CTE_PATTERN.finditer(sql)}
            for match in TABLE_REFERENCE_PATTERN.finditer(sql):
                table = match.group("table").lower()
                if table in cte_names or table == "information_schema":
                    continue
                usage.setdefault(table, set()).add(str(mapper_path.relative_to(ROOT)))
    missing = set(usage) - schema_tables()
    if missing:
        raise ValueError(f"Mapper tables missing from StarRocks DDL: {sorted(missing)}")
    return usage


def render_mapper_inventory() -> str:
    output = io.StringIO()
    writer = csv.writer(output, dialect="excel-tab", lineterminator="\n")
    writer.writerow(("table", "mapper_count", "mappers"))
    for table, mappers in sorted(mapper_table_usage().items()):
        writer.writerow((table, len(mappers), ",".join(sorted(mappers))))
    return output.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Fail when the inventory is stale")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--mapper-output", type=Path, default=DEFAULT_MAPPER_OUTPUT)
    arguments = parser.parse_args()
    rendered = render_inventory()
    rendered_mappers = render_mapper_inventory()
    if arguments.check:
        current = arguments.output.read_text() if arguments.output.exists() else ""
        current_mappers = (
            arguments.mapper_output.read_text() if arguments.mapper_output.exists() else ""
        )
        stale = False
        if current != rendered:
            print(
                f"{arguments.output.relative_to(ROOT)} is stale; regenerate it with {Path(__file__).name}",
                file=sys.stderr,
            )
            stale = True
        if current_mappers != rendered_mappers:
            print(
                f"{arguments.mapper_output.relative_to(ROOT)} is stale; regenerate it with {Path(__file__).name}",
                file=sys.stderr,
            )
            stale = True
        return int(stale)
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(rendered)
    arguments.mapper_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.mapper_output.write_text(rendered_mappers)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
