#!/usr/bin/env python3
"""Generate the checked StarRocks schema contract inventory from DDL and fixture SQL."""

from __future__ import annotations

import argparse
import csv
import functools
import io
import re
import sys
import xml.etree.ElementTree as ElementTree
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
DEFAULT_STATEMENT_OUTPUT = ROOT / "docs/starrocks-mapper-statement-inventory.tsv"
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
WRITE_TABLE_REFERENCE_PATTERN = re.compile(
    r"\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+`?(?P<table>[A-Za-z_][A-Za-z0-9_]*)`?",
    re.IGNORECASE,
)
CTE_PATTERN = re.compile(r"\b(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s+AS\s*\(", re.IGNORECASE)
STATEMENT_TAGS = {"select", "insert", "update", "delete"}
NON_TABLE_REFERENCES = {"select", "values"}
INDIRECT_STARROCKS_TESTS = {
    "org.cbioportal.infrastructure.repository.starrocks.coexpression.StarrocksCoExpressionMapper.getCoExpressionAggregates": (
        "StarrocksGenomicAggregateMapperTest#allAlterationAndGenomicAggregateStatementsExecuteAgainstFixture"
    ),
    "org.cbioportal.infrastructure.repository.starrocks.generic_assay.StarrocksGenericAssayMapper.getGenericAssayDataCountRows": (
        "StarrocksGenomicAggregateMapperTest#allAlterationAndGenomicAggregateStatementsExecuteAgainstFixture"
    ),
}


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


def mapper_documents() -> list[tuple[Path, ElementTree.Element]]:
    documents: list[tuple[Path, ElementTree.Element]] = []
    for mapper_root in MAPPER_ROOTS:
        for mapper_path in sorted(mapper_root.rglob("*.xml")):
            documents.append((mapper_path, ElementTree.parse(mapper_path).getroot()))
    return documents


def mapper_backend(mapper_path: Path) -> str:
    relative = str(mapper_path.relative_to(ROOT))
    if "/mappers/starrocks/" in f"/{relative}":
        return "starrocks"
    if "/mappers/clickhouse/" in f"/{relative}":
        return "clickhouse"
    if "/persistence/mybatisclickhouse/" in f"/{relative}":
        return "clickhouse-legacy"
    if "/legacy/persistence/mybatis/" in f"/{relative}":
        return "shared"
    raise ValueError(f"Unknown mapper backend: {relative}")


def expanded_element_sql(
    element: ElementTree.Element,
    namespace: str,
    fragments: dict[str, ElementTree.Element],
    stack: tuple[str, ...] = (),
    properties: dict[str, str] | None = None,
) -> str:
    properties = properties or {}

    def substitute(value: str) -> str:
        return re.sub(
            r"\$\{(?P<name>[A-Za-z_][A-Za-z0-9_]*)\}",
            lambda match: properties.get(match.group("name"), match.group(0)),
            value,
        )

    chunks = [element.text or ""]
    for child in element:
        if child.tag == "include":
            include_properties = dict(properties)
            for property_element in child.findall("property"):
                include_properties[property_element.attrib["name"]] = substitute(
                    property_element.attrib["value"]
                )
            reference = substitute(child.attrib["refid"])
            reference = re.sub(
                r"\$\{(?P<name>[A-Za-z_][A-Za-z0-9_]*)\}",
                lambda match: include_properties.get(match.group("name"), match.group(0)),
                reference,
            )
            if "${" in reference:
                raise ValueError(f"Unresolved mapper include property: {reference}")
            if "." not in reference:
                reference = f"{namespace}.{reference}"
            if reference in stack:
                raise ValueError(f"Recursive mapper include: {' -> '.join((*stack, reference))}")
            fragment = fragments.get(reference)
            if fragment is None:
                raise ValueError(f"Unknown mapper include: {reference}")
            fragment_namespace = reference.rsplit(".", 1)[0]
            chunks.append(
                expanded_element_sql(
                    fragment,
                    fragment_namespace,
                    fragments,
                    (*stack, reference),
                    include_properties,
                )
            )
        else:
            chunks.append(expanded_element_sql(child, namespace, fragments, stack, properties))
        chunks.append(child.tail or "")
    return " ".join(chunks)


def referenced_tables(sql: str) -> list[str]:
    sql = re.sub(r"<!--.*?-->", " ", sql, flags=re.DOTALL)
    sql = re.sub(r"--[^\n]*", " ", sql)
    cte_names = {match.group("name").lower() for match in CTE_PATTERN.finditer(sql)}
    tables = {
        match.group("table").lower()
        for pattern in (TABLE_REFERENCE_PATTERN, WRITE_TABLE_REFERENCE_PATTERN)
        for match in pattern.finditer(sql)
        if match.group("table").lower() not in cte_names
        and match.group("table").lower() not in NON_TABLE_REFERENCES
        and match.group("table").lower() != "information_schema"
    }
    return sorted(tables)


def mask_java_non_code(source: str) -> str:
    masked = list(source)
    index = 0
    state = "code"
    while index < len(source):
        if state == "code" and source.startswith("//", index):
            masked[index : index + 2] = "  "
            index += 2
            state = "line_comment"
        elif state == "code" and source.startswith("/*", index):
            masked[index : index + 2] = "  "
            index += 2
            state = "block_comment"
        elif state == "code" and source.startswith('"""', index):
            masked[index : index + 3] = "   "
            index += 3
            state = "text_block"
        elif state == "code" and source[index] in {'"', "'"}:
            state = "string" if source[index] == '"' else "character"
            masked[index] = " "
            index += 1
        elif state == "line_comment":
            if source[index] == "\n":
                state = "code"
            else:
                masked[index] = " "
            index += 1
        elif state == "block_comment":
            if source.startswith("*/", index):
                masked[index : index + 2] = "  "
                index += 2
                state = "code"
            else:
                if source[index] != "\n":
                    masked[index] = " "
                index += 1
        elif state == "text_block":
            if source.startswith('"""', index):
                masked[index : index + 3] = "   "
                index += 3
                state = "code"
            else:
                if source[index] != "\n":
                    masked[index] = " "
                index += 1
        elif state in {"string", "character"}:
            delimiter = '"' if state == "string" else "'"
            if source[index] == "\\" and index + 1 < len(source):
                masked[index : index + 2] = "  "
                index += 2
            else:
                if source[index] == delimiter:
                    state = "code"
                if source[index] != "\n":
                    masked[index] = " "
                index += 1
        else:
            index += 1
    return "".join(masked)


def test_method_spans(test_source: str) -> list[tuple[str, int, int]]:
    masked = mask_java_non_code(test_source)
    method_pattern = re.compile(
        r"@Test\s+(?:public\s+|protected\s+|private\s+)?(?:static\s+)?"
        r"void\s+(?P<name>\w+)\s*\([^)]*\)\s*(?:throws\s+[^\{]+)?\{",
        re.DOTALL,
    )
    spans: list[tuple[str, int, int]] = []
    for match in method_pattern.finditer(masked):
        opening_brace = match.end() - 1
        depth = 0
        for index in range(opening_brace, len(masked)):
            if masked[index] == "{":
                depth += 1
            elif masked[index] == "}":
                depth -= 1
                if depth == 0:
                    spans.append((match.group("name"), opening_brace, index))
                    break
        else:
            raise ValueError(f"Unbalanced @Test method: {match.group('name')}")
    return spans


@functools.lru_cache(maxsize=None)
def indexed_test_sources(starrocks_only: bool) -> tuple[tuple[Path, str, tuple], ...]:
    indexed = []
    for test_path in sorted((ROOT / "src/test/java").rglob("*Test.java")):
        if starrocks_only and not test_path.name.startswith("Starrocks"):
            continue
        source = test_path.read_text()
        indexed.append((test_path, source, tuple(test_method_spans(source))))
    return tuple(indexed)


def static_test_references(namespace: str, statement_id: str, starrocks_only: bool) -> list[str]:
    mapper_type = namespace.rsplit(".", 1)[-1]
    references: list[str] = []
    for test_path, source, spans in indexed_test_sources(starrocks_only):
        if mapper_type not in source:
            continue
        occurrences = list(re.finditer(rf"\.\s*{re.escape(statement_id)}\s*\(", source))
        if not occurrences:
            continue
        if len(spans) == 1:
            references.append(f"{test_path.stem}#{spans[0][0]}")
            continue
        for occurrence in occurrences:
            references.extend(
                f"{test_path.stem}#{method}"
                for method, start, end in spans
                if start <= occurrence.start() <= end
            )
    return sorted(set(references))


def known_test_method_references(starrocks_only: bool) -> set[str]:
    return {
        f"{test_path.stem}#{method}"
        for test_path, _, spans in indexed_test_sources(starrocks_only)
        for method, _, _ in spans
    }


def statement_inventory_rows() -> list[dict[str, str]]:
    documents = mapper_documents()
    fragments: dict[str, ElementTree.Element] = {}
    for _, mapper in documents:
        namespace = mapper.attrib["namespace"]
        for element in mapper:
            if element.tag == "sql":
                fragments[f"{namespace}.{element.attrib['id']}"] = element

    rows: list[dict[str, str]] = []
    unknown_indirect_tests = set(INDIRECT_STARROCKS_TESTS.values()) - known_test_method_references(
        True
    )
    if unknown_indirect_tests:
        raise ValueError(f"Unknown indirect StarRocks test references: {sorted(unknown_indirect_tests)}")
    ddl_tables = schema_tables()
    seen_statements: set[str] = set()
    for mapper_path, mapper in documents:
        namespace = mapper.attrib["namespace"]
        backend = mapper_backend(mapper_path)
        for element in mapper:
            if element.tag not in STATEMENT_TAGS:
                continue
            statement_id = element.attrib["id"]
            qualified_id = f"{namespace}.{statement_id}"
            if qualified_id in seen_statements:
                raise ValueError(f"Duplicate mapper statement: {qualified_id}")
            seen_statements.add(qualified_id)
            tables = referenced_tables(expanded_element_sql(element, namespace, fragments))
            missing_tables = set(tables) - ddl_tables
            if missing_tables:
                raise ValueError(
                    f"Mapper statement {qualified_id} references tables missing from StarRocks DDL: "
                    f"{sorted(missing_tables)}"
                )
            loaded_in_starrocks = backend in {"shared", "starrocks"}
            all_tests = static_test_references(namespace, statement_id, False)
            starrocks_tests = static_test_references(namespace, statement_id, True)
            indirect_test = INDIRECT_STARROCKS_TESTS.get(qualified_id)
            if indirect_test:
                starrocks_tests = sorted(set((*starrocks_tests, indirect_test)))
                all_tests = sorted(set((*all_tests, indirect_test)))
            verification = (
                "VERIFIED"
                if loaded_in_starrocks and starrocks_tests
                else "UNVERIFIED"
                if loaded_in_starrocks
                else "NOT_APPLICABLE"
            )
            rows.append(
                {
                    "backend": backend,
                    "loaded_in_starrocks": str(loaded_in_starrocks).lower(),
                    "namespace": namespace,
                    "statement_id": statement_id,
                    "command": element.tag.upper(),
                    "tables": ",".join(tables),
                    "source": str(mapper_path.relative_to(ROOT)),
                    "integration_test_methods": ",".join(all_tests) or "UNVERIFIED",
                    "starrocks_test_methods": ",".join(starrocks_tests) or "UNVERIFIED",
                    "starrocks_verification": verification,
                }
            )
    return sorted(rows, key=lambda row: (row["backend"], row["namespace"], row["statement_id"]))


def render_statement_inventory() -> str:
    output = io.StringIO()
    fields = (
        "backend",
        "loaded_in_starrocks",
        "namespace",
        "statement_id",
        "command",
        "tables",
        "source",
        "integration_test_methods",
        "starrocks_test_methods",
        "starrocks_verification",
    )
    writer = csv.DictWriter(output, fields, dialect="excel-tab", lineterminator="\n")
    writer.writeheader()
    writer.writerows(statement_inventory_rows())
    return output.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Fail when the inventory is stale")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--mapper-output", type=Path, default=DEFAULT_MAPPER_OUTPUT)
    parser.add_argument("--statement-output", type=Path, default=DEFAULT_STATEMENT_OUTPUT)
    arguments = parser.parse_args()
    rendered = render_inventory()
    rendered_mappers = render_mapper_inventory()
    rendered_statements = render_statement_inventory()
    if arguments.check:
        current = arguments.output.read_text() if arguments.output.exists() else ""
        current_mappers = (
            arguments.mapper_output.read_text() if arguments.mapper_output.exists() else ""
        )
        current_statements = (
            arguments.statement_output.read_text() if arguments.statement_output.exists() else ""
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
        if current_statements != rendered_statements:
            print(
                f"{arguments.statement_output.relative_to(ROOT)} is stale; regenerate it with {Path(__file__).name}",
                file=sys.stderr,
            )
            stale = True
        return int(stale)
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(rendered)
    arguments.mapper_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.mapper_output.write_text(rendered_mappers)
    arguments.statement_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.statement_output.write_text(rendered_statements)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
