package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksSmokeMapperTest {

  @Test
  void selectOneAgainstConfiguredStarrocks() throws Exception {
    String jdbcUrl = System.getenv("STARROCKS_JDBC_URL");
    String username = System.getenv("STARROCKS_USERNAME");
    String password = System.getenv("STARROCKS_PASSWORD");

    assumeTrue(
        hasText(jdbcUrl) && hasText(username),
        "Set STARROCKS_JDBC_URL and STARROCKS_USERNAME to run the StarRocks smoke test");

    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(jdbcUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password == null ? "" : password);

    SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
    sessionFactoryBean.setDataSource(dataSource);
    sessionFactoryBean.setMapperLocations(
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:mappers/starrocks/**/*.xml"));

    SqlSessionFactory sqlSessionFactory = sessionFactoryBean.getObject();
    assertThat(sqlSessionFactory).isNotNull();

    try (SqlSession session = sqlSessionFactory.openSession()) {
      StarrocksSmokeMapper mapper = session.getMapper(StarrocksSmokeMapper.class);
      assertThat(mapper.selectOne()).isEqualTo(1);
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
