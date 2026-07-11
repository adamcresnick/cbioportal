package org.cbioportal.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
import org.cbioportal.infrastructure.repository.starrocks.StarrocksDomainNotImplementedException;
import org.cbioportal.legacy.persistence.mybatisclickhouse.StudyViewMyBatisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StarrocksRepositoryFallbackConfigTest {

  private static final List<Class<?>> DOMAIN_REPOSITORY_TYPES =
      List.of(
          AlterationRepository.class,
          CancerStudyRepository.class,
          ClinicalAttributesRepository.class,
          ClinicalDataRepository.class,
          ClinicalEventRepository.class,
          CoExpressionRepository.class,
          GenericAssayRepository.class,
          GenomicDataRepository.class,
          MutationRepository.class,
          PatientRepository.class,
          SampleRepository.class,
          TreatmentRepository.class);

  private static final List<Class<?>> CLICKHOUSE_REPOSITORY_TYPES =
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
          StudyViewMyBatisRepository.class);

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(StarrocksRepositoryFallbackConfig.class);

  @Test
  void starrocksBackendProvidesExactlyOneFallbackForEveryDomainRepository() {
    contextRunner
        .withPropertyValues("columnstore.backend=starrocks")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNull();
              DOMAIN_REPOSITORY_TYPES.forEach(
                  repositoryType ->
                      assertThat(context.getBeansOfType(repositoryType))
                          .as(repositoryType.getSimpleName())
                          .hasSize(1));

              assertThatThrownBy(
                      () -> context.getBean(SampleRepository.class).getFilteredSamples(null))
                  .isInstanceOf(StarrocksDomainNotImplementedException.class)
                  .hasMessageContaining(SampleRepository.class.getName())
                  .hasMessageContaining("getFilteredSamples");
            });
  }

  @Test
  void fallbackConfigurationIsInactiveForDefaultClickhouseBackend() {
    contextRunner.run(
        context ->
            DOMAIN_REPOSITORY_TYPES.forEach(
                repositoryType -> assertThat(context).doesNotHaveBean(repositoryType)));
  }

  @Test
  void realStarrocksRepositoryBeanReplacesItsFallback() {
    ClinicalEventRepository realRepository = filter -> List.of();

    contextRunner
        .withPropertyValues("columnstore.backend=starrocks")
        .withBean(ClinicalEventRepository.class, () -> realRepository)
        .run(
            context -> {
              assertThat(context.getBeansOfType(ClinicalEventRepository.class)).hasSize(1);
              assertThat(context.getBean(ClinicalEventRepository.class)).isSameAs(realRepository);
              assertThat(context).doesNotHaveBean("starrocksClinicalEventRepositoryFallback");
            });
  }

  @Test
  void everyClickhouseDomainRepositoryRequiresClickhouseBackend() {
    CLICKHOUSE_REPOSITORY_TYPES.forEach(
        repositoryType -> {
          ConditionalOnProperty condition =
              repositoryType.getAnnotation(ConditionalOnProperty.class);
          assertThat(condition).as(repositoryType.getSimpleName()).isNotNull();
          assertThat(condition.name()).containsExactly("columnstore.backend");
          assertThat(condition.havingValue()).isEqualTo("clickhouse");
          assertThat(condition.matchIfMissing()).isTrue();
        });
  }
}
