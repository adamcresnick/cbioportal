package org.cbioportal.infrastructure.repository.starrocks.generic_assay;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.generic_assay.repository.GenericAssayRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.infrastructure.repository.starrocks.StarrocksStudyViewFilterSupport;
import org.cbioportal.legacy.model.ClinicalDataCount;
import org.cbioportal.legacy.model.GenericAssayDataCount;
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

  private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)");
  private static final Comparator<String> NATURAL_STABLE_ID_ORDER =
      Comparator.comparing(
              StarrocksGenericAssayRepository::leadingNumber,
              Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Comparator.naturalOrder());

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
    if (CollectionUtils.isEmpty(filters)) {
      return List.of();
    }
    StarrocksStudyViewFilterSupport.requireImplementedFilterFamilies(context);
    String profileType = filters.getFirst().getProfileType();
    List<String> stableIds = filters.stream().map(GenericAssayDataBinFilter::getStableId).toList();
    return mapper.getGenericAssayDataCountRows(context, profileType, stableIds, true).stream()
        .map(row -> clinicalDataCount(row, profileType))
        .toList();
  }

  @Override
  public List<GenericAssayDataCountItem> getGenericAssayDataCounts(
      StudyViewFilterContext context, List<GenericAssayDataFilter> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return List.of();
    }
    StarrocksStudyViewFilterSupport.requireImplementedFilterFamilies(context);
    String profileType = filters.getFirst().getProfileType();
    List<String> stableIds = filters.stream().map(GenericAssayDataFilter::getStableId).toList();
    return groupCounts(mapper.getGenericAssayDataCountRows(context, profileType, stableIds, true));
  }

  @Override
  public List<GenericAssayDataCountItem> getGenericAssayDataCountsByProfileType(
      StudyViewFilterContext context, String profileType) {
    StarrocksStudyViewFilterSupport.requireImplementedFilterFamilies(context);
    return groupCounts(mapper.getGenericAssayDataCountRows(context, profileType, null, false));
  }

  @Override
  public List<String> getGenericAssayStableIdsByProfileIds(List<String> molecularProfileIds) {
    if (CollectionUtils.isEmpty(molecularProfileIds)) {
      return List.of();
    }
    return mapper.getGenericAssayStableIdsByProfileIds(molecularProfileIds).stream()
        .sorted(NATURAL_STABLE_ID_ORDER)
        .toList();
  }

  @Override
  public List<GenericAssayMeta> getGenericAssayMetaByStableIds(List<String> stableIds) {
    if (CollectionUtils.isEmpty(stableIds)) {
      return List.of();
    }
    return sortMeta(mapper.getGenericAssayMetaByStableIds(stableIds));
  }

  @Override
  public List<GenericAssayMeta> getGenericAssayMetaByProfileIds(
      List<String> profileIds, List<String> stableIds) {
    if (CollectionUtils.isEmpty(profileIds)) {
      return List.of();
    }
    return sortMeta(mapper.getGenericAssayMetaByProfileIds(profileIds, stableIds));
  }

  private static ClinicalDataCount clinicalDataCount(
      StarrocksGenericAssayCountRow row, String profileType) {
    ClinicalDataCount result = new ClinicalDataCount();
    result.setAttributeId(row.getStableId() + profileType);
    result.setValue(row.getValue());
    result.setCount(row.getCount());
    return result;
  }

  private static List<GenericAssayDataCountItem> groupCounts(
      List<StarrocksGenericAssayCountRow> rows) {
    Map<String, List<GenericAssayDataCount>> grouped = new LinkedHashMap<>();
    for (StarrocksGenericAssayCountRow row : rows) {
      grouped
          .computeIfAbsent(row.getStableId(), ignored -> new ArrayList<>())
          .add(new GenericAssayDataCount(row.getValue(), row.getCount()));
    }
    return grouped.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(NATURAL_STABLE_ID_ORDER))
        .map(entry -> new GenericAssayDataCountItem(entry.getKey(), entry.getValue()))
        .toList();
  }

  private static List<GenericAssayMeta> sortMeta(List<GenericAssayMeta> values) {
    return values.stream()
        .sorted(Comparator.comparing(GenericAssayMeta::getStableId, NATURAL_STABLE_ID_ORDER))
        .toList();
  }

  private static BigInteger leadingNumber(String stableId) {
    Matcher matcher = LEADING_NUMBER.matcher(stableId);
    return matcher.find() ? new BigInteger(matcher.group(1)) : null;
  }
}
