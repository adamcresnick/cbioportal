package org.cbioportal.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ColumnstoreBackendPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(ColumnstoreBackendValidation.class);

  @Test
  void defaultsToClickhouseWhenBackendIsUnset() {
    contextRunner.run(
        context -> {
          assertThat(context.getStartupFailure()).isNull();
          assertThat(context).hasSingleBean(ColumnstoreBackendProperties.class);
          assertThat(context.getBean(ColumnstoreBackendProperties.class).getBackend())
              .isEqualTo(ColumnstoreBackend.CLICKHOUSE);
        });
  }

  @Test
  void bindsStarrocksBackend() {
    contextRunner
        .withPropertyValues("columnstore.backend=starrocks")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNull();
              assertThat(context.getBean(ColumnstoreBackendProperties.class).getBackend())
                  .isEqualTo(ColumnstoreBackend.STARROCKS);
            });
  }

  @Test
  void failsStartupForInvalidBackend() {
    contextRunner
        .withPropertyValues("columnstore.backend=postgres")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNotNull();
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining("columnstore.backend");
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining("clickhouse, starrocks");
            });
  }
}
