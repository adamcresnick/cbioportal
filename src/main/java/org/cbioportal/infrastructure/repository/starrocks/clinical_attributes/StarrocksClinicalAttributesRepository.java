package org.cbioportal.infrastructure.repository.starrocks.clinical_attributes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cbioportal.domain.clinical_attributes.ClinicalAttribute;
import org.cbioportal.domain.clinical_attributes.repository.ClinicalAttributesRepository;
import org.cbioportal.legacy.persistence.enums.DataSource;
import org.cbioportal.legacy.web.parameter.ClinicalDataType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "columnstore.backend",
    havingValue = "starrocks",
    matchIfMissing = false)
public class StarrocksClinicalAttributesRepository implements ClinicalAttributesRepository {

  private Map<DataSource, List<ClinicalAttribute>> clinicalAttributesMap =
      new EnumMap<>(DataSource.class);

  private final StarrocksClinicalAttributesMapper mapper;

  public StarrocksClinicalAttributesRepository(StarrocksClinicalAttributesMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<ClinicalAttribute> getClinicalAttributesForStudies(List<String> studyIds) {
    if (studyIds == null || studyIds.isEmpty()) {
      return List.of();
    }
    return mapper.getClinicalAttributesForStudies(studyIds);
  }

  @Override
  public List<ClinicalAttribute> getClinicalAttributesForStudiesDetailed(List<String> studyIds) {
    if (studyIds == null || studyIds.isEmpty()) {
      return List.of();
    }
    return mapper.getClinicalAttributesForStudiesDetailed(studyIds);
  }

  @Override
  public Map<String, ClinicalDataType> getClinicalAttributeDatatypeMap() {
    if (clinicalAttributesMap.isEmpty()) {
      buildClinicalAttributeNameMap();
    }

    Map<String, ClinicalDataType> attributeDatatypeMap = new HashMap<>();

    clinicalAttributesMap
        .getOrDefault(DataSource.SAMPLE, List.of())
        .forEach(
            attribute -> attributeDatatypeMap.put(attribute.attrId(), ClinicalDataType.SAMPLE));

    clinicalAttributesMap
        .getOrDefault(DataSource.PATIENT, List.of())
        .forEach(
            attribute -> attributeDatatypeMap.put(attribute.attrId(), ClinicalDataType.PATIENT));

    return attributeDatatypeMap;
  }

  private void buildClinicalAttributeNameMap() {
    clinicalAttributesMap =
        mapper.getClinicalAttributes().stream()
            .collect(
                Collectors.groupingBy(
                    ca ->
                        Boolean.TRUE.equals(ca.patientAttribute())
                            ? DataSource.PATIENT
                            : DataSource.SAMPLE));
  }
}
