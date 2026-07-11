package org.cbioportal.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ColumnstoreBackendProperties.class)
public class ColumnstoreBackendValidation {

  @Bean
  InitializingBean validateColumnstoreBackend(ColumnstoreBackendProperties properties) {
    return () -> {
      if (properties.getBackend() == null) {
        throw new IllegalStateException(
            "columnstore.backend must be one of: clickhouse, starrocks");
      }
    };
  }
}
