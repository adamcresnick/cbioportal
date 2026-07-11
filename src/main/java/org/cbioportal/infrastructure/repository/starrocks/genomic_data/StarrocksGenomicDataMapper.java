package org.cbioportal.infrastructure.repository.starrocks.genomic_data;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.ClinicalDataCount;
import org.cbioportal.legacy.model.GenomicDataCount;
import org.cbioportal.legacy.model.GenomicDataCountItem;
import org.cbioportal.legacy.web.parameter.GenomicDataBinFilter;
import org.cbioportal.legacy.web.parameter.GenomicDataFilter;

public interface StarrocksGenomicDataMapper {

  List<GenomicDataCount> getMolecularProfileSampleCounts(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext);

  List<ClinicalDataCount> getGenomicDataBinCounts(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("genomicDataBinFilters") List<GenomicDataBinFilter> genomicDataBinFilters);

  List<GenomicDataCountItem> getCNACounts(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("genomicDataFilters") List<GenomicDataFilter> genomicDataFilters);

  Map<String, Integer> getMutationCounts(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("genomicDataFilter") GenomicDataFilter genomicDataFilter);

  List<GenomicDataCountItem> getMutationCountsByType(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("genomicDataFilters") List<GenomicDataFilter> genomicDataFilters,
      @Param("includeSampleIds") boolean includeSampleIds,
      @Param("hugoGeneSymbol") String hugoGeneSymbol);
}
