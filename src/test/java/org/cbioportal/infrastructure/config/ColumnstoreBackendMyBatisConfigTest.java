package org.cbioportal.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.infrastructure.repository.clickhouse.sample.ClickhouseSampleMapper;
import org.cbioportal.infrastructure.repository.starrocks.StarrocksSmokeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ColumnstoreBackendMyBatisConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(DataSource.class, DriverManagerDataSource::new)
          .withUserConfiguration(
              ColumnstoreBackendValidation.class,
              ClickhouseMyBatisConfig.class,
              StarrocksMyBatisConfig.class);

  @Test
  void defaultBackendUsesClickhouseMyBatisConfig() {
    contextRunner.run(
        context -> {
          assertThat(context.getStartupFailure()).isNull();
          assertThat(context).hasSingleBean(ClickhouseMyBatisConfig.class);
          assertThat(context).doesNotHaveBean(StarrocksMyBatisConfig.class);
          assertThat(context.getBeansOfType(SqlSessionFactory.class)).hasSize(1);
        });
  }

  @Test
  void starrocksBackendUsesOnlyStarrocksMyBatisConfig() {
    contextRunner
        .withPropertyValues("columnstore.backend=starrocks")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNull();
              assertThat(context).hasSingleBean(StarrocksMyBatisConfig.class);
              assertThat(context).doesNotHaveBean(ClickhouseMyBatisConfig.class);
              assertThat(context.getBeansOfType(SqlSessionFactory.class)).hasSize(1);
              assertThat(context).hasSingleBean(StarrocksSmokeMapper.class);
              assertThat(context).doesNotHaveBean(ClickhouseSampleMapper.class);
            });
  }
}
