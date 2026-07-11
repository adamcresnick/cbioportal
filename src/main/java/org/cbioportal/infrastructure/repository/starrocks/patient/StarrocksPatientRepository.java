package org.cbioportal.infrastructure.repository.starrocks.patient;

import java.util.List;
import org.cbioportal.domain.patient.repository.PatientRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.infrastructure.repository.starrocks.StarrocksStudyViewFilterSupport;
import org.cbioportal.legacy.model.CaseListDataCount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "columnstore.backend",
    havingValue = "starrocks",
    matchIfMissing = false)
public class StarrocksPatientRepository implements PatientRepository {

  private final StarrocksPatientMapper mapper;

  public StarrocksPatientRepository(StarrocksPatientMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public int getFilteredPatientCount(StudyViewFilterContext studyViewFilterContext) {
    StarrocksStudyViewFilterSupport.requireBaseFilterFamilies(studyViewFilterContext);
    return mapper.getPatientCount(studyViewFilterContext);
  }

  @Override
  public List<CaseListDataCount> getCaseListDataCounts(
      StudyViewFilterContext studyViewFilterContext) {
    StarrocksStudyViewFilterSupport.requireBaseFilterFamilies(studyViewFilterContext);
    return mapper.getCaseListDataCounts(studyViewFilterContext);
  }
}
