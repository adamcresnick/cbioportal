package org.cbioportal.infrastructure.repository.starrocks.treatment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.domain.treatment.repository.TreatmentRepository;
import org.cbioportal.legacy.model.ClinicalEventSample;
import org.cbioportal.legacy.model.PatientTreatment;
import org.cbioportal.legacy.model.SampleTreatment;
import org.cbioportal.shared.enums.ProjectionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "columnstore.backend",
    havingValue = "starrocks",
    matchIfMissing = false)
public class StarrocksTreatmentRepository implements TreatmentRepository {
  private final StarrocksTreatmentMapper mapper;

  public StarrocksTreatmentRepository(StarrocksTreatmentMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<PatientTreatment> getPatientTreatments(
      StudyViewFilterContext studyViewFilterContext) {
    return mapper.getPatientTreatments(studyViewFilterContext);
  }

  @Override
  public int getTotalPatientTreatmentCount(StudyViewFilterContext studyViewFilterContext) {
    return mapper.getPatientTreatmentCounts(studyViewFilterContext);
  }

  @Override
  public List<SampleTreatment> getSampleTreatments(
      StudyViewFilterContext studyViewFilterContext, ProjectionType projection) {
    Map<String, TreatmentSamples> treatments = new LinkedHashMap<>();
    for (StarrocksSampleTreatmentRow row : mapper.getSampleTreatmentRows(studyViewFilterContext)) {
      TreatmentSamples samples =
          treatments.computeIfAbsent(row.treatment(), ignored -> new TreatmentSamples());
      ClinicalEventSample sample = toClinicalEventSample(row);
      if ("Pre".equals(row.temporalRelation())) {
        samples.pre.add(sample);
      } else {
        samples.post.add(sample);
      }
    }
    boolean detailed = projection == ProjectionType.DETAILED;
    return treatments.entrySet().stream()
        .map(
            entry ->
                new SampleTreatment(
                    entry.getKey(),
                    entry.getValue().pre.size(),
                    entry.getValue().post.size(),
                    detailed ? List.copyOf(entry.getValue().pre) : List.of(),
                    detailed ? List.copyOf(entry.getValue().post) : List.of()))
        .toList();
  }

  @Override
  public int getTotalSampleTreatmentCount(StudyViewFilterContext studyViewFilterContext) {
    return mapper.getTotalSampleTreatmentCounts(studyViewFilterContext);
  }

  private static ClinicalEventSample toClinicalEventSample(StarrocksSampleTreatmentRow row) {
    ClinicalEventSample sample = new ClinicalEventSample();
    sample.setSampleId(row.sampleId());
    sample.setPatientId(row.patientId());
    sample.setTimeTaken(row.timeTaken());
    sample.setStudyId(row.studyId());
    return sample;
  }

  private static final class TreatmentSamples {
    private final List<ClinicalEventSample> pre = new ArrayList<>();
    private final List<ClinicalEventSample> post = new ArrayList<>();
  }
}
