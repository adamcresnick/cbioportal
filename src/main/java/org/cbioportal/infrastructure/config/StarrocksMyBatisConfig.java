package org.cbioportal.infrastructure.config;

import java.io.IOException;
import javax.sql.DataSource;
import org.cbioportal.legacy.model.Sample;
import org.cbioportal.legacy.persistence.mybatis.typehandler.SampleTypeTypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
@MapperScan(
    value = {
      "org.cbioportal.infrastructure.repository.starrocks",
      "org.cbioportal.legacy.persistence.mybatis"
    },
    sqlSessionFactoryRef = "sqlSessionFactory")
public class StarrocksMyBatisConfig {

  @Bean
  ConfigurationCustomizer mybatisConfigurationCustomizer() {
    return new ConfigurationCustomizer() {
      @Override
      public void customize(org.apache.ibatis.session.Configuration configuration) {
        configuration
            .getTypeHandlerRegistry()
            .register(Sample.SampleType.class, new SampleTypeTypeHandler());
      }
    };
  }

  @Bean("sqlSessionFactory")
  public SqlSessionFactoryBean sqlSessionFactory(
      DataSource dataSource, ApplicationContext applicationContext) throws IOException {
    SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
    sessionFactory.setDataSource(dataSource);

    // Include StarRocks mapper XML and legacy mapper XML, but never ClickHouse mapper XML.
    sessionFactory.addMapperLocations(
        applicationContext.getResources("classpath:mappers/starrocks/**/*.xml"));
    sessionFactory.addMapperLocations(
        applicationContext.getResources(
            "classpath:org/cbioportal/legacy/persistence/mybatis/*.xml"));

    sessionFactory.setTypeHandlers(new SampleTypeTypeHandler());
    return sessionFactory;
  }

  @Bean
  public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
