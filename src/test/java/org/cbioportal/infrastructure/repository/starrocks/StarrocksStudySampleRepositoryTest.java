package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.cbioportal.domain.cancerstudy.CancerStudyMetadata;
import org.cbioportal.domain.sample.Sample;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.infrastructure.repository.starrocks.cancerstudy.StarrocksCancerStudyMapper;
import org.cbioportal.infrastructure.repository.starrocks.cancerstudy.StarrocksCancerStudyRepository;
import org.cbioportal.infrastructure.repository.starrocks.patient.StarrocksPatientMapper;
import org.cbioportal.infrastructure.repository.starrocks.patient.StarrocksPatientRepository;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleMapper;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleRepository;
import org.cbioportal.legacy.model.GeneFilter;
import org.cbioportal.shared.SortAndSearchCriteria;
import org.cbioportal.shared.enums.ProjectionType;
import org.junit.jupiter.api.Test;

class StarrocksStudySampleRepositoryTest {

  @Test
  void cancerStudyRepositoryDelegatesAllContractShapes() {
    StarrocksCancerStudyMapper mapper = mock(StarrocksCancerStudyMapper.class);
    StarrocksCancerStudyRepository repository = new StarrocksCancerStudyRepository(mapper);
    SortAndSearchCriteria criteria = new SortAndSearchCriteria(null, null, null, null, null);
    CancerStudyMetadata study = mock(CancerStudyMetadata.class);
    StudyViewFilterContext filter = mock(StudyViewFilterContext.class);

    when(mapper.getCancerStudiesMetadata(criteria, List.of())).thenReturn(List.of(study));
    when(mapper.getCancerStudiesMetadata(any(SortAndSearchCriteria.class), eq(List.of("study"))))
        .thenReturn(List.of(study));
    when(mapper.getCancerStudiesMetadataSummary(criteria, List.of())).thenReturn(List.of(study));
    when(mapper.getFilteredStudyIds(filter)).thenReturn(List.of("study"));

    assertThat(repository.getCancerStudiesMetadata(criteria)).containsExactly(study);
    assertThat(repository.getCancerStudyMetadata("study")).isSameAs(study);
    assertThat(repository.getCancerStudiesMetadataSummary(criteria)).containsExactly(study);
    assertThat(repository.getFilteredStudyIds(filter)).containsExactly("study");
    assertThat(repository.getResourceCountsForAllStudies()).isEmpty();
    verify(mapper).getResourceCountsForAllStudies();
  }

  @Test
  void patientRepositoryDelegatesCounts() {
    StarrocksPatientMapper mapper = mock(StarrocksPatientMapper.class);
    StarrocksPatientRepository repository = new StarrocksPatientRepository(mapper);
    StudyViewFilterContext filter = mock(StudyViewFilterContext.class);
    when(mapper.getPatientCount(filter)).thenReturn(3);

    assertThat(repository.getFilteredPatientCount(filter)).isEqualTo(3);
    assertThat(repository.getCaseListDataCounts(filter)).isEmpty();
    verify(mapper).getCaseListDataCounts(filter);
  }

  @Test
  void sampleRepositorySelectsProjectionAndCalculatesOffset() {
    StarrocksSampleMapper mapper = mock(StarrocksSampleMapper.class);
    StarrocksSampleRepository repository = new StarrocksSampleRepository(mapper);
    Sample sample = mock(Sample.class);
    when(mapper.getDetailedSamples(List.of("study"), null, null, null, 25, 50, "stableId", "DESC"))
        .thenReturn(List.of(sample));
    when(mapper.getSummarySamplesBySampleListIds(List.of("study_all"))).thenReturn(List.of(sample));

    assertThat(
            repository.getAllSamplesInStudy(
                "study", ProjectionType.DETAILED, 25, 2, "stableId", "DESC"))
        .containsExactly(sample);
    assertThat(repository.fetchSamplesBySampleListIds(List.of("study_all"), ProjectionType.SUMMARY))
        .containsExactly(sample);
    assertThat(repository.fetchSamples(List.of("study"), List.of("sample"), ProjectionType.ID))
        .isEmpty();

    verify(mapper).getSamples(List.of("study"), null, List.of("sample"), null, 0, 0, null, null);

    StudyViewFilterContext unsupportedFilter = mock(StudyViewFilterContext.class);
    when(unsupportedFilter.geneFilters()).thenReturn(List.of(new GeneFilter()));
    assertThatThrownBy(() -> repository.getFilteredSamples(unsupportedFilter))
        .isInstanceOf(StarrocksUnsupportedStudyViewFilterException.class)
        .hasMessageContaining("geneFilters");
  }
}
