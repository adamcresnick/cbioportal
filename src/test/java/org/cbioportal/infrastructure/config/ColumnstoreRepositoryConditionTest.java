package org.cbioportal.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import org.cbioportal.infrastructure.repository.starrocks.alteration.StarrocksAlterationRepository;
import org.cbioportal.infrastructure.repository.starrocks.cancerstudy.StarrocksCancerStudyRepository;
import org.cbioportal.infrastructure.repository.starrocks.clinical_attributes.StarrocksClinicalAttributesRepository;
import org.cbioportal.infrastructure.repository.starrocks.clinical_data.StarrocksClinicalDataRepository;
import org.cbioportal.infrastructure.repository.starrocks.clinical_event.StarrocksClinicalEventRepository;
import org.cbioportal.infrastructure.repository.starrocks.coexpression.StarrocksCoExpressionRepository;
import org.cbioportal.infrastructure.repository.starrocks.generic_assay.StarrocksGenericAssayRepository;
import org.cbioportal.infrastructure.repository.starrocks.genomic_data.StarrocksGenomicDataRepository;
import org.cbioportal.infrastructure.repository.starrocks.mutation.StarrocksMutationRepository;
import org.cbioportal.infrastructure.repository.starrocks.patient.StarrocksPatientRepository;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleRepository;
import org.cbioportal.infrastructure.repository.starrocks.treatment.StarrocksTreatmentRepository;
import org.cbioportal.legacy.persistence.mybatisclickhouse.StudyViewMyBatisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

class ColumnstoreRepositoryConditionTest {

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

  private static final List<Class<?>> STARROCKS_REPOSITORY_TYPES =
      List.of(
          StarrocksAlterationRepository.class,
          StarrocksCancerStudyRepository.class,
          StarrocksClinicalAttributesRepository.class,
          StarrocksClinicalDataRepository.class,
          StarrocksClinicalEventRepository.class,
          StarrocksCoExpressionRepository.class,
          StarrocksGenericAssayRepository.class,
          StarrocksGenomicDataRepository.class,
          StarrocksMutationRepository.class,
          StarrocksPatientRepository.class,
          StarrocksSampleRepository.class,
          StarrocksTreatmentRepository.class);

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

  @Test
  void everyStarrocksDomainRepositoryRequiresStarrocksBackend() {
    STARROCKS_REPOSITORY_TYPES.forEach(
        repositoryType -> {
          ConditionalOnProperty condition =
              repositoryType.getAnnotation(ConditionalOnProperty.class);
          assertThat(condition).as(repositoryType.getSimpleName()).isNotNull();
          assertThat(condition.name()).containsExactly("columnstore.backend");
          assertThat(condition.havingValue()).isEqualTo("starrocks");
          assertThat(condition.matchIfMissing()).isFalse();
        });
  }
}
