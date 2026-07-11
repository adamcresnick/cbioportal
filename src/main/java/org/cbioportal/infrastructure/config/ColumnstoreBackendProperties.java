package org.cbioportal.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "columnstore")
public class ColumnstoreBackendProperties {

  private ColumnstoreBackend backend = ColumnstoreBackend.CLICKHOUSE;

  public ColumnstoreBackend getBackend() {
    return backend;
  }

  public void setBackend(String backend) {
    this.backend = ColumnstoreBackend.fromPropertyValue(backend);
  }
}
