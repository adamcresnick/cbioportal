package org.cbioportal.infrastructure.repository.starrocks.generic_assay;

import java.util.List;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.MolecularProfile;

public interface StarrocksGenericAssayMapper {

  List<MolecularProfile> getGenericAssayProfiles();

  List<MolecularProfile> getFilteredMolecularProfilesByAlterationType(
      StudyViewFilterContext studyViewFilterContext, String alterationType);
}
