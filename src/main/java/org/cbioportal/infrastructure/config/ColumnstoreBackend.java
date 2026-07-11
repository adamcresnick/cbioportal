package org.cbioportal.infrastructure.config;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ColumnstoreBackend {
  CLICKHOUSE("clickhouse"),
  STARROCKS("starrocks");

  private final String propertyValue;

  ColumnstoreBackend(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public static ColumnstoreBackend fromPropertyValue(String value) {
    return Arrays.stream(values())
        .filter(backend -> backend.propertyValue.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "columnstore.backend must be one of: " + allowedPropertyValues()));
  }

  private static String allowedPropertyValues() {
    return Arrays.stream(values())
        .map(ColumnstoreBackend::getPropertyValue)
        .collect(Collectors.joining(", "));
  }
}
