package org.cbioportal.infrastructure.repository.starrocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class StarrocksTestCluster implements AutoCloseable {

  static final String VERSION = "3.5.19";
  private static final String USERNAME = "root";
  private static final String PASSWORD = "";
  private static final String DATABASE = "cbioportal";
  private static final int QUERY_PORT = 9030;
  private static final int BE_HTTP_PORT = 8040;

  private final Network network = Network.newNetwork();
  private final GenericContainer<?> frontend =
      new GenericContainer<>(DockerImageName.parse("starrocks/fe-ubuntu:" + VERSION))
          .withNetwork(network)
          .withNetworkAliases("starrocks-fe")
          .withExposedPorts(QUERY_PORT)
          .withTmpFs(Map.of("/opt/starrocks/fe/meta", "rw,size=6g"))
          .withCommand(
              "bash",
              "-c",
              """
              printf '\nstorage_usage_hard_limit_percent = 100\nstorage_usage_hard_limit_reserve_bytes = 0\nstorage_usage_soft_limit_percent = 100\nstorage_usage_soft_limit_reserve_bytes = 0\n' >> /opt/starrocks/fe/conf/fe.conf
              exec bash /opt/starrocks/fe/bin/start_fe.sh --host_type FQDN
              """)
          .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
  private final GenericContainer<?> backend =
      new GenericContainer<>(DockerImageName.parse("starrocks/be-ubuntu:" + VERSION))
          .withNetwork(network)
          .withNetworkAliases("starrocks-be")
          .withExposedPorts(BE_HTTP_PORT)
          .withTmpFs(Map.of("/opt/starrocks/be/storage", "rw,size=2g"))
          .withCommand(
              "bash",
              "-c",
              """
              printf '\nstorage_flood_stage_usage_percent = 100\nstorage_flood_stage_left_capacity_bytes = 0\nstorage_high_watermark_usage_percent = 100\nstorage_min_left_capacity_bytes = 0\n' >> /opt/starrocks/be/conf/be.conf
              exec bash /opt/starrocks/be/bin/start_be.sh --host_type FQDN
              """)
          .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

  void start() throws SQLException, InterruptedException {
    frontend.start();
    backend.start();

    try (Connection connection = openConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "ADMIN SET FRONTEND CONFIG (\"storage_usage_hard_limit_percent\" = \"99\")");
      statement.execute(
          "ADMIN SET FRONTEND CONFIG (\"storage_usage_hard_limit_reserve_bytes\" = \"0\")");
      statement.execute(
          "ADMIN SET FRONTEND CONFIG (\"storage_usage_soft_limit_percent\" = \"99\")");
      statement.execute(
          "ADMIN SET FRONTEND CONFIG (\"storage_usage_soft_limit_reserve_bytes\" = \"0\")");
      statement.execute("ALTER SYSTEM ADD BACKEND \"starrocks-be:9050\"");
    }

    waitForBackend();
    createStartupSchema();
  }

  String jdbcUrl() {
    return rootJdbcUrl() + DATABASE;
  }

  String username() {
    return USERNAME;
  }

  String password() {
    return PASSWORD;
  }

  void recreateSchema() throws SQLException {
    executeSqlScript("db-scripts/starrocks/schema.sql");
    executeSqlScript("db-scripts/starrocks/derived.sql");
  }

  void loadFixture() throws SQLException {
    executeSqlScript("starrocks/seed.sql");
  }

  private void executeSqlScript(String classpathLocation) throws SQLException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl(), USERNAME, PASSWORD)) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(classpathLocation));
    }
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection(rootJdbcUrl(), USERNAME, PASSWORD);
  }

  private String rootJdbcUrl() {
    return "jdbc:mysql://" + frontend.getHost() + ":" + frontend.getMappedPort(QUERY_PORT) + "/";
  }

  private void createStartupSchema() throws SQLException {
    try (Connection connection = openConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE " + DATABASE);
      statement.execute(
          """
          CREATE TABLE cbioportal.type_of_cancer (
            type_of_cancer_id VARCHAR(63) NOT NULL,
            name VARCHAR(255) NOT NULL,
            dedicated_color VARCHAR(63) NOT NULL,
            short_name VARCHAR(63) NULL,
            parent VARCHAR(63) NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(type_of_cancer_id)
          DISTRIBUTED BY HASH(type_of_cancer_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.gene (
            entrez_gene_id BIGINT NOT NULL,
            hugo_gene_symbol VARCHAR(255) NOT NULL,
            genetic_entity_id BIGINT NOT NULL,
            type VARCHAR(63) NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(entrez_gene_id)
          DISTRIBUTED BY HASH(entrez_gene_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.gene_alias (
            entrez_gene_id BIGINT NOT NULL,
            gene_alias VARCHAR(255) NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(entrez_gene_id, gene_alias)
          DISTRIBUTED BY HASH(entrez_gene_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.reference_genome (
            reference_genome_id BIGINT NOT NULL,
            species VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            build_name VARCHAR(255) NOT NULL,
            genome_size BIGINT NULL,
            url VARCHAR(1024) NOT NULL,
            release_date DATETIME NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(reference_genome_id)
          DISTRIBUTED BY HASH(reference_genome_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.cancer_study (
            cancer_study_id BIGINT NOT NULL,
            cancer_study_identifier VARCHAR(255) NULL,
            type_of_cancer_id VARCHAR(63) NOT NULL,
            name VARCHAR(255) NOT NULL,
            description VARCHAR(65533) NOT NULL,
            `public` INT NOT NULL,
            pmid VARCHAR(255) NULL,
            citation VARCHAR(1024) NULL,
            `groups` VARCHAR(1024) NULL,
            status BIGINT NULL,
            import_date DATETIME NULL,
            reference_genome_id BIGINT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(cancer_study_id)
          DISTRIBUTED BY HASH(cancer_study_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.sample_list (
            list_id BIGINT NOT NULL,
            stable_id VARCHAR(255) NOT NULL,
            category VARCHAR(255) NOT NULL,
            cancer_study_id BIGINT NOT NULL,
            name VARCHAR(255) NOT NULL,
            description VARCHAR(1024) NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(list_id)
          DISTRIBUTED BY HASH(list_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.sample_list_list (
            list_id BIGINT NOT NULL,
            sample_id BIGINT NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(list_id, sample_id)
          DISTRIBUTED BY HASH(list_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.patient (
            internal_id BIGINT NOT NULL,
            stable_id VARCHAR(255) NOT NULL,
            cancer_study_id BIGINT NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(internal_id)
          DISTRIBUTED BY HASH(internal_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.sample (
            internal_id BIGINT NOT NULL,
            stable_id VARCHAR(255) NOT NULL,
            sample_type VARCHAR(63) NOT NULL,
            patient_id BIGINT NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(internal_id)
          DISTRIBUTED BY HASH(internal_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.resource_definition (
            resource_id VARCHAR(255) NOT NULL,
            display_name VARCHAR(255) NOT NULL,
            description VARCHAR(1024) NULL,
            resource_type VARCHAR(63) NOT NULL,
            open_by_default INT NULL,
            priority BIGINT NOT NULL,
            cancer_study_id BIGINT NOT NULL,
            custom_metadata VARCHAR(65533) NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(resource_id)
          DISTRIBUTED BY HASH(resource_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.resource_patient (
            internal_id BIGINT NOT NULL,
            resource_id VARCHAR(255) NOT NULL,
            url VARCHAR(2048) NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(internal_id, resource_id)
          DISTRIBUTED BY HASH(internal_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
      statement.execute(
          """
          CREATE TABLE cbioportal.resource_sample (
            internal_id BIGINT NOT NULL,
            resource_id VARCHAR(255) NOT NULL,
            url VARCHAR(2048) NOT NULL
          ) ENGINE=OLAP
          DUPLICATE KEY(internal_id, resource_id)
          DISTRIBUTED BY HASH(internal_id) BUCKETS 1
          PROPERTIES ("replication_num" = "1")
          """);
    }
  }

  private void waitForBackend() throws SQLException, InterruptedException {
    long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
    while (System.nanoTime() < deadline) {
      try (Connection connection = openConnection();
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SHOW BACKENDS")) {
        while (resultSet.next()) {
          String totalCapacity = resultSet.getString("TotalCapacity");
          String availableCapacity = resultSet.getString("AvailCapacity");
          if (resultSet.getBoolean("Alive")
              && "OK".equals(resultSet.getString("StatusCode"))
              && !totalCapacity.startsWith("0.000")
              && !availableCapacity.startsWith("1.000 B")) {
            return;
          }
        }
      }
      Thread.sleep(500);
    }
    throw new IllegalStateException("StarRocks backend did not become healthy within two minutes");
  }

  @Override
  public void close() {
    try {
      backend.stop();
    } finally {
      try {
        frontend.stop();
      } finally {
        network.close();
      }
    }
  }
}
