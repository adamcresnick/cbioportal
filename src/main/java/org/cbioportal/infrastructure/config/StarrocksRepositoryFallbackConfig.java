package org.cbioportal.infrastructure.config;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import org.cbioportal.infrastructure.repository.starrocks.StarrocksDomainNotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksRepositoryFallbackConfig {

  @Bean
  @ConditionalOnMissingBean(AlterationRepository.class)
  AlterationRepository starrocksAlterationRepositoryFallback() {
    return unimplementedRepository(AlterationRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(CancerStudyRepository.class)
  CancerStudyRepository starrocksCancerStudyRepositoryFallback() {
    return unimplementedRepository(CancerStudyRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(ClinicalAttributesRepository.class)
  ClinicalAttributesRepository starrocksClinicalAttributesRepositoryFallback() {
    return unimplementedRepository(ClinicalAttributesRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(ClinicalDataRepository.class)
  ClinicalDataRepository starrocksClinicalDataRepositoryFallback() {
    return unimplementedRepository(ClinicalDataRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(ClinicalEventRepository.class)
  ClinicalEventRepository starrocksClinicalEventRepositoryFallback() {
    return unimplementedRepository(ClinicalEventRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(CoExpressionRepository.class)
  CoExpressionRepository starrocksCoExpressionRepositoryFallback() {
    return unimplementedRepository(CoExpressionRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(GenericAssayRepository.class)
  GenericAssayRepository starrocksGenericAssayRepositoryFallback() {
    return unimplementedRepository(GenericAssayRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(GenomicDataRepository.class)
  GenomicDataRepository starrocksGenomicDataRepositoryFallback() {
    return unimplementedRepository(GenomicDataRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(MutationRepository.class)
  MutationRepository starrocksMutationRepositoryFallback() {
    return unimplementedRepository(MutationRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(PatientRepository.class)
  PatientRepository starrocksPatientRepositoryFallback() {
    return unimplementedRepository(PatientRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(SampleRepository.class)
  SampleRepository starrocksSampleRepositoryFallback() {
    return unimplementedRepository(SampleRepository.class);
  }

  @Bean
  @ConditionalOnMissingBean(TreatmentRepository.class)
  TreatmentRepository starrocksTreatmentRepositoryFallback() {
    return unimplementedRepository(TreatmentRepository.class);
  }

  private static <T> T unimplementedRepository(Class<T> repositoryType) {
    Object proxy =
        Proxy.newProxyInstance(
            repositoryType.getClassLoader(),
            new Class<?>[] {repositoryType},
            (instance, method, arguments) -> invoke(repositoryType, instance, method, arguments));
    return repositoryType.cast(proxy);
  }

  private static Object invoke(
      Class<?> repositoryType, Object instance, Method method, Object[] arguments) {
    if (method.getDeclaringClass() == Object.class) {
      return switch (method.getName()) {
        case "equals" -> instance == arguments[0];
        case "hashCode" -> System.identityHashCode(instance);
        case "toString" -> "UnimplementedStarrocksRepository[" + repositoryType.getName() + "]";
        default -> throw new IllegalStateException("Unexpected Object method: " + method);
      };
    }
    throw new StarrocksDomainNotImplementedException(repositoryType, method);
  }
}
