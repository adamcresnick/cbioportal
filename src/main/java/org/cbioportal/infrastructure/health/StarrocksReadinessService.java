package org.cbioportal.infrastructure.health;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksReadinessService {

  public static final List<String> REQUIRED_TABLES =
      List.of(
          "allele_specific_copy_number",
          "alteration_driver_annotation",
          "authorities",
          "cancer_study",
          "cancer_study_tags",
          "clinical_attribute_meta",
          "clinical_data_derived",
          "clinical_event",
          "clinical_event_data",
          "clinical_event_data_derived",
          "clinical_event_derived",
          "clinical_patient",
          "clinical_sample",
          "cna_event",
          "copy_number_seg",
          "copy_number_seg_file",
          "data_access_tokens",
          "gene",
          "gene_alias",
          "gene_panel",
          "gene_panel_list",
          "gene_panel_to_gene_derived",
          "generic_assay_data_derived",
          "generic_assay_meta_derived",
          "generic_assay_profile_entity_derived",
          "generic_entity_properties",
          "geneset",
          "geneset_gene",
          "geneset_hierarchy_leaf",
          "geneset_hierarchy_node",
          "genetic_alteration",
          "genetic_alteration_derived",
          "genetic_entity",
          "genetic_profile",
          "genetic_profile_link",
          "genetic_profile_samples",
          "genomic_event_derived",
          "gistic",
          "gistic_to_gene",
          "info",
          "mut_sig",
          "mutation",
          "mutation_count_by_keyword",
          "mutation_derived",
          "mutation_event",
          "patient",
          "reference_genome",
          "reference_genome_gene",
          "resource_definition",
          "resource_patient",
          "resource_sample",
          "resource_study",
          "sample",
          "sample_cna_event",
          "sample_derived",
          "sample_list",
          "sample_list_list",
          "sample_profile",
          "sample_to_gene_panel_derived",
          "schema_migrations",
          "structural_variant",
          "type_of_cancer",
          "users");

  private final DataSource dataSource;
  private final String requiredMigration;

  public StarrocksReadinessService(
      DataSource dataSource,
      @Value("${starrocks.readiness.required-migration:1}") String requiredMigration) {
    this.dataSource = dataSource;
    this.requiredMigration = requiredMigration;
  }

  public StarrocksReadiness check() {
    try (Connection connection = dataSource.getConnection()) {
      String product = "StarRocks";
      String version = currentVersion(connection);
      String migration = migrationVersion(connection);
      List<String> missingTables = missingTables(connection);
      boolean ready =
          version != null
              && !version.isBlank()
              && requiredMigration.equals(migration)
              && missingTables.isEmpty();
      return new StarrocksReadiness(
          ready,
          ready ? "UP" : "DOWN",
          "starrocks",
          product,
          version,
          requiredMigration,
          migration,
          missingTables,
          null);
    } catch (SQLException ignored) {
      return new StarrocksReadiness(
          false,
          "DOWN",
          "starrocks",
          null,
          null,
          requiredMigration,
          null,
          REQUIRED_TABLES,
          "StarRocks readiness query failed");
    }
  }

  private static String migrationVersion(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT version FROM schema_migrations ORDER BY installed_on DESC LIMIT 1")) {
      return resultSet.next() ? resultSet.getString(1) : null;
    }
  }

  private static String currentVersion(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT CURRENT_VERSION()")) {
      return resultSet.next() ? resultSet.getString(1) : null;
    }
  }

  private static List<String> missingTables(Connection connection) throws SQLException {
    Set<String> actual = new HashSet<>();
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")) {
      while (resultSet.next()) {
        actual.add(resultSet.getString(1).toLowerCase(Locale.ROOT));
      }
    }
    List<String> missing = new ArrayList<>(REQUIRED_TABLES);
    missing.removeAll(actual);
    return List.copyOf(missing);
  }

  public record StarrocksReadiness(
      boolean ready,
      String status,
      String backend,
      String databaseProduct,
      String databaseVersion,
      String requiredMigration,
      String actualMigration,
      List<String> missingTables,
      String error) {}
}
