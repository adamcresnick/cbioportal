#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import unittest
import xml.etree.ElementTree as ElementTree
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("generate_starrocks_schema_inventory.py")
SPEC = importlib.util.spec_from_file_location("starrocks_inventory", SCRIPT_PATH)
assert SPEC and SPEC.loader
INVENTORY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(INVENTORY)


class StarrocksInventoryGeneratorTest(unittest.TestCase):
    def test_expands_property_selected_nested_include(self):
        statement = ElementTree.fromstring(
            """
            <select id="statement">
              <include refid="outer">
                <property name="selectedFragment" value="leaf"/>
              </include>
            </select>
            """
        )
        outer = ElementTree.fromstring(
            '<sql id="outer"><include refid="${selectedFragment}"/></sql>'
        )
        leaf = ElementTree.fromstring('<sql id="leaf">FROM sample</sql>')

        sql = INVENTORY.expanded_element_sql(
            statement,
            "example.Mapper",
            {"example.Mapper.outer": outer, "example.Mapper.leaf": leaf},
        )

        self.assertEqual(INVENTORY.referenced_tables(sql), ["sample"])

    def test_test_method_spans_ignore_braces_in_comments_and_text_blocks(self):
        source = '''
          @Test
          void executesStatement() {
            // A comment with a closing brace }
            String json = """{"value": "}"}""";
            mapper.getRows();
          }
        '''

        spans = INVENTORY.test_method_spans(source)

        self.assertEqual([span[0] for span in spans], ["executesStatement"])
        self.assertIn("mapper.getRows()", source[spans[0][1] : spans[0][2]])

    def test_every_native_starrocks_statement_has_real_starrocks_test_reference(self):
        rows = INVENTORY.statement_inventory_rows()
        native_rows = [row for row in rows if row["backend"] == "starrocks"]

        self.assertEqual(len(native_rows), 60)
        self.assertTrue(native_rows)
        self.assertEqual(
            [row["namespace"] + "." + row["statement_id"] for row in native_rows
             if row["starrocks_verification"] != "VERIFIED"],
            [],
        )


if __name__ == "__main__":
    unittest.main()
