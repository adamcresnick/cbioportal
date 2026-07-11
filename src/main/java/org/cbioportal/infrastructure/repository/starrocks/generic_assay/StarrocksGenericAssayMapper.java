package org.cbioportal.infrastructure.repository.starrocks.generic_assay;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.MolecularProfile;
import org.cbioportal.legacy.model.meta.GenericAssayMeta;

public interface StarrocksGenericAssayMapper {

  List<MolecularProfile> getGenericAssayProfiles();

  List<MolecularProfile> getFilteredMolecularProfilesByAlterationType(
      StudyViewFilterContext studyViewFilterContext, String alterationType);

  List<StarrocksGenericAssayCountRow> getGenericAssayDataCountRows(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("profileType") String profileType,
      @Param("stableIds") List<String> stableIds,
      @Param("includeZeroNa") boolean includeZeroNa);

  List<String> getGenericAssayStableIdsByProfileIds(@Param("profileIds") List<String> profileIds);

  List<GenericAssayMeta> getGenericAssayMetaByStableIds(@Param("stableIds") List<String> stableIds);

  List<GenericAssayMeta> getGenericAssayMetaByProfileIds(
      @Param("profileIds") List<String> profileIds, @Param("stableIds") List<String> stableIds);
}
