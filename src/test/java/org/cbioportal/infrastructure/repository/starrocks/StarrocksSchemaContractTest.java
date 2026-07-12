package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cbioportal.infrastructure.health.StarrocksReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksSchemaContractTest {

  private static final List<String> REQUIRED_TABLES =
      List.of(
          "cancer_study",
          "clinical_data_derived",
          "clinical_event_derived",
          "gene",
          "generic_assay_data_derived",
          "genomic_event_derived",
          "mutation",
          "mutation_derived",
          "patient",
          "sample",
          "sample_derived",
          "schema_migrations");

  @Test
  void schemaAndFixtureRecreateTwiceOnPinnedStarrocks() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();
      FixtureCounts firstLoad;
      try (Connection connection =
              DriverManager.getConnection(
                  cluster.jdbcUrl(), cluster.username(), cluster.password());
          Statement statement = connection.createStatement()) {
        firstLoad = fixtureCounts(statement);
      }

      cluster.recreateSchema();
      cluster.loadFixture();

      try (Connection connection =
              DriverManager.getConnection(
                  cluster.jdbcUrl(), cluster.username(), cluster.password());
          Statement statement = connection.createStatement()) {
        assertThat(tableCount(statement)).isEqualTo(63);
        for (String table : REQUIRED_TABLES) {
          assertThat(tableExists(statement, table)).as(table).isTrue();
        }
        assertThat(fixtureCounts(statement)).isEqualTo(firstLoad);
        assertThat(firstLoad).isEqualTo(new FixtureCounts(2, 6, 8, 5, 3, 2, 10, 14, 1, 3));
        assertThat(queryCount(statement, "clinical_sample", "attr_id = 'SPECIAL'")).isEqualTo(6);
        assertThat(
                queryCount(statement, "clinical_sample", "attr_id = 'SPECIAL' AND attr_value = ''"))
            .isEqualTo(2);
        assertThat(
                queryCount(
                    statement, "clinical_sample", "attr_id = 'SPECIAL' AND attr_value = 'NA'"))
            .isEqualTo(2);
        assertThat(queryCount(statement, "clinical_patient", "attr_id = 'OS_MONTHS'")).isEqualTo(5);
        assertThat(queryCount(statement, "sample_profile", "panel_id IS NULL")).isEqualTo(6);
        assertThat(queryCount(statement, "mutation", "entrez_gene_id = 7157")).isEqualTo(3);
        assertThat(queryCount(statement, "clinical_event", "event_type = 'TREATMENT'"))
            .isEqualTo(2);
        assertThat(queryCount(statement, "generic_assay_meta_derived", "properties IS NOT NULL"))
            .isEqualTo(2);
        assertThat(
                queryCount(
                    statement,
                    "genetic_profile",
                    "generic_assay_type IS NOT NULL AND datatype = 'LIMIT-VALUE'"))
            .isEqualTo(2);
        assertThat(queryCount(statement, "genomic_event_derived", "off_panel = 1")).isEqualTo(1);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(cluster.jdbcUrl());
        dataSource.setUsername(cluster.username());
        dataSource.setPassword(cluster.password());
        StarrocksReadinessService readinessService = new StarrocksReadinessService(dataSource, "1");
        assertThat(readinessService.check())
            .satisfies(
                readiness -> {
                  assertThat(readiness.databaseVersion()).as(readiness.toString()).isNotBlank();
                  assertThat(readiness.backend()).isEqualTo("starrocks");
                  assertThat(readiness.actualMigration()).isEqualTo("1");
                  assertThat(readiness.missingTables()).isEmpty();
                  assertThat(readiness.ready()).as(readiness.toString()).isTrue();
                });

        statement.execute("DROP TABLE mutation_derived");
        assertThat(readinessService.check())
            .satisfies(
                readiness -> {
                  assertThat(readiness.ready()).isFalse();
                  assertThat(readiness.missingTables()).containsExactly("mutation_derived");
                });
      }
    }
  }

  @Test
  void schemaScriptsContainNoClickhouseDialect() throws Exception {
    for (String path :
        List.of("db-scripts/starrocks/schema.sql", "db-scripts/starrocks/derived.sql")) {
      String sql = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
      assertThat(sql)
          .as(path)
          .doesNotContain(
              "MergeTree",
              "ReplacingMergeTree",
              "LowCardinality(",
              "Nullable(",
              "DateTime64(",
              "OPTIMIZE TABLE",
              "arrayJoin",
              "multiIf(",
              "cbioportal.",
              "radiant_",
              "RADIANT_");
    }
  }

  @Test
  void readinessTableInventoryMatchesSchemaScripts() throws Exception {
    Pattern createTable = Pattern.compile("(?m)^CREATE TABLE `?([a-z_]+)`?");
    List<String> schemaTables = new ArrayList<>();
    for (String path :
        List.of("db-scripts/starrocks/schema.sql", "db-scripts/starrocks/derived.sql")) {
      String sql = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
      Matcher matcher = createTable.matcher(sql);
      while (matcher.find()) {
        schemaTables.add(matcher.group(1));
      }
    }

    assertThat(schemaTables)
        .doesNotHaveDuplicates()
        .containsExactlyInAnyOrderElementsOf(StarrocksReadinessService.REQUIRED_TABLES);
  }

  @Test
  void fixtureContainsNoEnvironmentSpecificDatabaseQualifier() throws Exception {
    String sql =
        new ClassPathResource("starrocks/seed.sql").getContentAsString(StandardCharsets.UTF_8);
    assertThat(sql).doesNotContain("cbioportal.", "radiant_", "RADIANT_");
  }

  private static int tableCount(Statement statement) throws Exception {
    try (ResultSet resultSet =
        statement.executeQuery(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'cbioportal' AND table_type = 'BASE TABLE'")) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static boolean tableExists(Statement statement, String table) throws Exception {
    try (ResultSet resultSet =
        statement.executeQuery(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'cbioportal' AND table_name = '"
                + table
                + "'")) {
      resultSet.next();
      return resultSet.getInt(1) == 1;
    }
  }

  private static FixtureCounts fixtureCounts(Statement statement) throws Exception {
    return new FixtureCounts(
        queryCount(statement, "cancer_study", "1 = 1"),
        queryCount(statement, "patient", "1 = 1"),
        queryCount(statement, "sample", "1 = 1"),
        queryCount(statement, "mutation", "1 = 1"),
        queryCount(statement, "alteration_driver_annotation", "1 = 1"),
        queryCount(statement, "structural_variant", "1 = 1"),
        queryCount(statement, "clinical_event_data_derived", "1 = 1"),
        queryCount(statement, "generic_assay_data_derived", "1 = 1"),
        queryCount(statement, "allele_specific_copy_number", "1 = 1"),
        queryCount(statement, "cna_event", "1 = 1"));
  }

  private static int queryCount(Statement statement, String table, String predicate)
      throws Exception {
    try (ResultSet resultSet =
        statement.executeQuery("SELECT COUNT(*) FROM `" + table + "` WHERE " + predicate)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private record FixtureCounts(
      int studies,
      int patients,
      int samples,
      int mutations,
      int driverAnnotations,
      int structuralVariants,
      int clinicalEventData,
      int genericAssayData,
      int alleleSpecificCopyNumbers,
      int cnaEvents) {}
}
