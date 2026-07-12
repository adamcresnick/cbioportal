package org.cbioportal.infrastructure.repository.starrocks.genomic_data;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.genomic_data.repository.GenomicDataRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.ClinicalDataCount;
import org.cbioportal.legacy.model.GenomicDataCount;
import org.cbioportal.legacy.model.GenomicDataCountItem;
import org.cbioportal.legacy.web.parameter.GenomicDataBinFilter;
import org.cbioportal.legacy.web.parameter.GenomicDataFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksGenomicDataRepository implements GenomicDataRepository {

  private final StarrocksGenomicDataMapper mapper;

  public StarrocksGenomicDataRepository(StarrocksGenomicDataMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<GenomicDataCount> getMolecularProfileSampleCounts(StudyViewFilterContext context) {
    return mapper.getMolecularProfileSampleCounts(context);
  }

  @Override
  public List<ClinicalDataCount> getGenomicDataBinCounts(
      StudyViewFilterContext context, List<GenomicDataBinFilter> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return List.of();
    }
    return mapper.getGenomicDataBinCounts(context, filters);
  }

  @Override
  public List<GenomicDataCountItem> getCNACounts(
      StudyViewFilterContext context, List<GenomicDataFilter> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return List.of();
    }
    return mapper.getCNACounts(context, filters);
  }

  @Override
  public Map<String, Integer> getMutationCounts(
      StudyViewFilterContext context, GenomicDataFilter filter) {
    return mapper.getMutationCounts(context, filter);
  }

  @Override
  public List<GenomicDataCountItem> getMutationCountsByType(
      StudyViewFilterContext context,
      List<GenomicDataFilter> filters,
      boolean includeSampleIds,
      String hugoGeneSymbol) {
    if (CollectionUtils.isEmpty(filters) && hugoGeneSymbol == null) {
      return List.of();
    }
    return mapper.getMutationCountsByType(context, filters, includeSampleIds, hugoGeneSymbol);
  }
}
