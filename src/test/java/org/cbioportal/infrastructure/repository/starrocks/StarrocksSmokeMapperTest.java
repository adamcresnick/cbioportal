package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.PortalApplication;
import org.cbioportal.domain.alteration.repository.AlterationRepository;
import org.cbioportal.domain.cancerstudy.repository.CancerStudyRepository;
import org.cbioportal.domain.clinical_attributes.repository.ClinicalAttributesRepository;
import org.cbioportal.domain.clinical_data.repository.ClinicalDataRepository;
import org.cbioportal.domain.clinical_event.repository.ClinicalEventRepository;
import org.cbioportal.domain.coexpression.repository.CoExpressionRepository;
import org.cbioportal.domain.generic_assay.repository.GenericAssayRepository;
import org.cbioportal.domain.genomic_data.repository.GenomicDataRepository;
import org.cbioportal.domain.mutation.repository.MutationRepository;
import org.cbioportal.domain.patient.repository.PatientRepository;
import org.cbioportal.domain.sample.repository.SampleRepository;
import org.cbioportal.domain.treatment.repository.TreatmentRepository;
import org.cbioportal.infrastructure.repository.clickhouse.alteration.ClickhouseAlterationRepository;
import org.cbioportal.infrastructure.repository.clickhouse.cancerstudy.ClickhouseCancerStudyRepository;
import org.cbioportal.infrastructure.repository.clickhouse.clinical_attributes.ClickhouseClinicalAttributesRepository;
import org.cbioportal.infrastructure.repository.clickhouse.clinical_data.ClickhouseClinicalDataRepository;
import org.cbioportal.infrastructure.repository.clickhouse.clinical_event.ClickhouseClinicalEventRepository;
import org.cbioportal.infrastructure.repository.clickhouse.coexpression.ClickhouseCoExpressionRepository;
import org.cbioportal.infrastructure.repository.clickhouse.generic_assay.ClickhouseGenericAssayRepository;
import org.cbioportal.infrastructure.repository.clickhouse.genomic_data.ClickhouseGenomicDataRepository;
import org.cbioportal.infrastructure.repository.clickhouse.mutation.ClickhouseMutationRepository;
import org.cbioportal.infrastructure.repository.clickhouse.patient.ClickhousePatientRepository;
import org.cbioportal.infrastructure.repository.clickhouse.sample.ClickhouseSampleRepository;
import org.cbioportal.infrastructure.repository.clickhouse.treatment.ClickhouseTreatmentRepository;
import org.cbioportal.legacy.persistence.mybatisclickhouse.StudyViewMyBatisRepository;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksSmokeMapperTest {

  @Test
  void selectOneAndVersionAgainstRealStarrocks() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();

      DriverManagerDataSource dataSource = new DriverManagerDataSource();
      dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
      dataSource.setUrl(cluster.jdbcUrl());
      dataSource.setUsername(cluster.username());
      dataSource.setPassword(cluster.password());

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
        assertThat(mapper.currentVersion()).startsWith(StarrocksTestCluster.VERSION);
      }

      try (ConfigurableApplicationContext context =
          new SpringApplicationBuilder(PortalApplication.class)
              .web(WebApplicationType.NONE)
              .run(
                  "--columnstore.backend=starrocks",
                  "--spring.datasource.url=" + cluster.jdbcUrl(),
                  "--spring.datasource.username=" + cluster.username(),
                  "--spring.datasource.password=" + cluster.password(),
                  "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                  "--authenticate=false",
                  "--bitly.access.token=",
                  "--patient_view.url=",
                  "--sample_view.url=")) {
        assertNoClickhouseDomainRepositories(context);
        assertSingleDomainRepository(context, AlterationRepository.class);
        assertSingleDomainRepository(context, CancerStudyRepository.class);
        assertSingleDomainRepository(context, ClinicalAttributesRepository.class);
        assertSingleDomainRepository(context, ClinicalDataRepository.class);
        assertSingleDomainRepository(context, ClinicalEventRepository.class);
        assertSingleDomainRepository(context, CoExpressionRepository.class);
        assertSingleDomainRepository(context, GenericAssayRepository.class);
        assertSingleDomainRepository(context, GenomicDataRepository.class);
        assertSingleDomainRepository(context, MutationRepository.class);
        assertSingleDomainRepository(context, PatientRepository.class);
        assertSingleDomainRepository(context, SampleRepository.class);
        assertSingleDomainRepository(context, TreatmentRepository.class);
      }
    }
  }

  private static void assertSingleDomainRepository(
      ConfigurableApplicationContext context, Class<?> repositoryType) {
    assertThat(context.getBeansOfType(repositoryType))
        .as(repositoryType.getSimpleName())
        .hasSize(1);
  }

  private static void assertNoClickhouseDomainRepositories(ConfigurableApplicationContext context) {
    List.of(
            ClickhouseAlterationRepository.class,
            ClickhouseCancerStudyRepository.class,
            ClickhouseClinicalAttributesRepository.class,
            ClickhouseClinicalDataRepository.class,
            ClickhouseClinicalEventRepository.class,
            ClickhouseCoExpressionRepository.class,
            ClickhouseGenericAssayRepository.class,
            ClickhouseGenomicDataRepository.class,
            ClickhouseMutationRepository.class,
            ClickhousePatientRepository.class,
            ClickhouseSampleRepository.class,
            ClickhouseTreatmentRepository.class,
            StudyViewMyBatisRepository.class)
        .forEach(
            repositoryType ->
                assertThat(context.getBeansOfType(repositoryType))
                    .as(repositoryType.getSimpleName())
                    .isEmpty());
  }
}
