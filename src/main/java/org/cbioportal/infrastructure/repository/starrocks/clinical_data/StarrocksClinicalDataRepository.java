package org.cbioportal.infrastructure.repository.starrocks.clinical_data;

import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.clinical_data.ClinicalData;
import org.cbioportal.domain.clinical_data.ClinicalDataType;
import org.cbioportal.domain.clinical_data.repository.ClinicalDataRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.ClinicalDataCountItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * StarRocks implementation of the ClinicalDataRepository interface.
 *
 * <p>This repository provides access to clinical data stored in StarRocks column store, optimized
 * for analytical queries. It delegates to MyBatis mappers for SQL query execution and handles
 * empty-collection edge cases to prevent unnecessary database calls.
 *
 * @see ClinicalDataRepository
 * @see StarrocksClinicalDataMapper
 */
@Repository
@ConditionalOnProperty(
    name = "columnstore.backend",
    havingValue = "starrocks",
    matchIfMissing = false)
public class StarrocksClinicalDataRepository implements ClinicalDataRepository {

  private final StarrocksClinicalDataMapper mapper;

  /**
   * Constructor for dependency injection.
   *
   * @param mapper MyBatis mapper for executing StarRocks queries
   */
  public StarrocksClinicalDataRepository(StarrocksClinicalDataMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to MyBatis mapper to execute optimized StarRocks query with study view filters.
   */
  @Override
  public List<ClinicalData> getPatientClinicalData(
      StudyViewFilterContext studyViewFilterContext, List<String> filteredAttributes) {
    return mapper.getPatientClinicalDataByStudyViewFilter(
        studyViewFilterContext, filteredAttributes);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to MyBatis mapper to execute optimized StarRocks query with study view filters.
   */
  @Override
  public List<ClinicalData> getSampleClinicalData(
      StudyViewFilterContext studyViewFilterContext, List<String> filteredAttributes) {
    return mapper.getSampleClinicalDataByStudyViewFilter(
        studyViewFilterContext, filteredAttributes);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to MyBatis mapper to execute count aggregation query with study view filters.
   */
  @Override
  public List<ClinicalDataCountItem> getClinicalDataCounts(
      StudyViewFilterContext studyViewFilterContext,
      List<String> sampleAttributeIds,
      List<String> patientAttributeIds,
      List<String> conflictingAttributeIds) {
    if (allEmpty(sampleAttributeIds, patientAttributeIds, conflictingAttributeIds)) {
      return List.of();
    }
    return mapper.getClinicalDataCountsByStudyViewFilter(
        studyViewFilterContext, sampleAttributeIds, patientAttributeIds, conflictingAttributeIds);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This overload is optimized for enrichment analysis by accepting pre-computed unique IDs to
   * avoid additional ID resolution queries. Delegates to MyBatis mapper for efficient batch
   * aggregation.
   */
  @Override
  public List<ClinicalDataCountItem> getClinicalDataCountsForEnrichments(
      List<String> sampleUniqueIds,
      List<String> patientUniqueIds,
      List<String> sampleAttributeIds,
      List<String> patientAttributeIds,
      List<String> conflictingAttributeIds) {
    if ((CollectionUtils.isEmpty(sampleUniqueIds) && CollectionUtils.isEmpty(patientUniqueIds))
        || allEmpty(sampleAttributeIds, patientAttributeIds, conflictingAttributeIds)) {
      return Collections.emptyList();
    }
    return mapper.getClinicalDataCountsForEnrichments(
        sampleUniqueIds,
        patientUniqueIds,
        sampleAttributeIds,
        patientAttributeIds,
        conflictingAttributeIds);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns early with empty list if no unique IDs provided to avoid unnecessary database call.
   */
  @Override
  public List<ClinicalData> fetchClinicalDataId(
      List<String> uniqueIds,
      List<String> attributeIds,
      List<String> studyIds,
      ClinicalDataType clinicalDataType) {
    if (CollectionUtils.isEmpty(uniqueIds)) {
      return Collections.emptyList();
    }
    return mapper.fetchClinicalDataId(
        uniqueIds, attributeIds, studyIds, clinicalDataType.toString());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns early with empty list if no unique IDs provided to avoid unnecessary database call.
   */
  @Override
  public List<ClinicalData> fetchClinicalDataSummary(
      List<String> uniqueIds,
      List<String> attributeIds,
      List<String> studyIds,
      ClinicalDataType clinicalDataType) {
    if (CollectionUtils.isEmpty(uniqueIds)) {
      return Collections.emptyList();
    }
    return mapper.fetchClinicalDataSummary(
        uniqueIds, attributeIds, studyIds, clinicalDataType.toString());
  }

  /**
   * {@inheritDoc}
   *
   * <p>This overload is optimized for enrichment analysis by accepting pre-computed unique IDs and
   * categorized attributes. Executes a single optimized query that unions sample-level,
   * patient-level, and conflicting attribute data.
   */
  @Override
  public List<ClinicalData> fetchClinicalDataSummaryForEnrichments(
      List<String> sampleUniqueIds,
      List<String> patientUniqueIds,
      List<String> sampleAttributeIds,
      List<String> patientAttributeIds,
      List<String> conflictingAttributeIds) {
    if ((CollectionUtils.isEmpty(sampleUniqueIds) && CollectionUtils.isEmpty(patientUniqueIds))
        || allEmpty(sampleAttributeIds, patientAttributeIds, conflictingAttributeIds)) {
      return Collections.emptyList();
    }
    return mapper.fetchClinicalDataSummaryForEnrichments(
        sampleUniqueIds,
        patientUniqueIds,
        sampleAttributeIds,
        patientAttributeIds,
        conflictingAttributeIds);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns early with empty list if no unique IDs provided to avoid unnecessary database call.
   */
  @Override
  public List<ClinicalData> fetchClinicalDataDetailed(
      List<String> uniqueIds,
      List<String> attributeIds,
      List<String> studyIds,
      ClinicalDataType clinicalDataType) {
    if (CollectionUtils.isEmpty(uniqueIds)) {
      return Collections.emptyList();
    }
    return mapper.fetchClinicalDataDetailed(
        uniqueIds, attributeIds, studyIds, clinicalDataType.toString());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns 0 if no unique IDs provided or if count result is null from the database.
   */
  @Override
  public Integer fetchClinicalDataMeta(
      List<String> uniqueIds,
      List<String> attributeIds,
      List<String> studyIds,
      ClinicalDataType clinicalDataType) {
    if (CollectionUtils.isEmpty(uniqueIds)) {
      return 0;
    }
    Integer cnt =
        mapper.fetchClinicalDataMeta(
            uniqueIds, attributeIds, studyIds, clinicalDataType.toString());
    return cnt == null ? 0 : cnt;
  }

  @SafeVarargs
  private static boolean allEmpty(List<String>... values) {
    for (List<String> value : values) {
      if (CollectionUtils.isNotEmpty(value)) {
        return false;
      }
    }
    return true;
  }
}
