package org.cbioportal.infrastructure.repository.starrocks.generic_assay;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.generic_assay.repository.GenericAssayRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.infrastructure.repository.starrocks.StarrocksDomainNotImplementedException;
import org.cbioportal.infrastructure.repository.starrocks.StarrocksStudyViewFilterSupport;
import org.cbioportal.legacy.model.ClinicalDataCount;
import org.cbioportal.legacy.model.GenericAssayDataCountItem;
import org.cbioportal.legacy.model.MolecularProfile;
import org.cbioportal.legacy.model.meta.GenericAssayMeta;
import org.cbioportal.legacy.web.parameter.GenericAssayDataBinFilter;
import org.cbioportal.legacy.web.parameter.GenericAssayDataFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksGenericAssayRepository implements GenericAssayRepository {

  private final StarrocksGenericAssayMapper mapper;

  public StarrocksGenericAssayRepository(StarrocksGenericAssayMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<MolecularProfile> getGenericAssayProfiles() {
    return mapper.getGenericAssayProfiles();
  }

  @Override
  public List<MolecularProfile> getFilteredMolecularProfilesByAlterationType(
      StudyViewFilterContext context, String alterationType) {
    StarrocksStudyViewFilterSupport.requireImplementedFilterFamilies(context);
    return mapper.getFilteredMolecularProfilesByAlterationType(context, alterationType);
  }

  @Override
  public List<ClinicalDataCount> getGenericAssayDataBinCounts(
      StudyViewFilterContext context, List<GenericAssayDataBinFilter> filters) {
    throw notImplemented("getGenericAssayDataBinCounts");
  }

  @Override
  public List<GenericAssayDataCountItem> getGenericAssayDataCounts(
      StudyViewFilterContext context, List<GenericAssayDataFilter> filters) {
    throw notImplemented("getGenericAssayDataCounts");
  }

  @Override
  public List<GenericAssayDataCountItem> getGenericAssayDataCountsByProfileType(
      StudyViewFilterContext context, String profileType) {
    throw notImplemented("getGenericAssayDataCountsByProfileType");
  }

  @Override
  public List<String> getGenericAssayStableIdsByProfileIds(List<String> molecularProfileIds) {
    if (CollectionUtils.isEmpty(molecularProfileIds)) {
      return List.of();
    }
    throw notImplemented("getGenericAssayStableIdsByProfileIds");
  }

  @Override
  public List<GenericAssayMeta> getGenericAssayMetaByStableIds(List<String> stableIds) {
    if (CollectionUtils.isEmpty(stableIds)) {
      return List.of();
    }
    throw notImplemented("getGenericAssayMetaByStableIds");
  }

  @Override
  public List<GenericAssayMeta> getGenericAssayMetaByProfileIds(
      List<String> profileIds, List<String> stableIds) {
    if (CollectionUtils.isEmpty(profileIds)) {
      return List.of();
    }
    throw notImplemented("getGenericAssayMetaByProfileIds");
  }

  private static StarrocksDomainNotImplementedException notImplemented(String operation) {
    return new StarrocksDomainNotImplementedException(GenericAssayRepository.class, operation);
  }
}
