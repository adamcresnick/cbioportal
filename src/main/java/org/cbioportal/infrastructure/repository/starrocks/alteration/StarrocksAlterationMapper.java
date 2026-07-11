package org.cbioportal.infrastructure.repository.starrocks.alteration;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.AlterationCountByGene;
import org.cbioportal.legacy.model.CopyNumberCountByGene;
import org.cbioportal.legacy.model.EntityToPanel;
import org.cbioportal.legacy.model.GenePanelToGene;
import org.cbioportal.legacy.model.MolecularProfile;
import org.cbioportal.legacy.persistence.helper.AlterationFilterHelper;

public interface StarrocksAlterationMapper {

  List<AlterationCountByGene> getMutatedGenes(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationFilterHelper") AlterationFilterHelper alterationFilterHelper);

  List<CopyNumberCountByGene> getCnaGenes(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationFilterHelper") AlterationFilterHelper alterationFilterHelper);

  List<AlterationCountByGene> getStructuralVariantGenes(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationFilterHelper") AlterationFilterHelper alterationFilterHelper);

  List<GenePanelToGene> getMatchingGenePanelIds(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationType") String alterationType);

  List<AlterationCountByGene> getTotalProfiledCounts(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationType") String alterationType,
      @Param("molecularProfiles") List<MolecularProfile> molecularProfiles);

  int getSampleProfileCountWithoutPanelData(
      @Param("studyViewFilterContext") StudyViewFilterContext studyViewFilterContext,
      @Param("alterationType") String alterationType);

  List<GenePanelToGene> getGenePanelGenes();

  List<EntityToPanel> getSampleToGenePanels(
      @Param("entityIds") List<String> entityIds, @Param("profileIds") List<String> profileIds);

  List<EntityToPanel> getPatientToGenePanels(
      @Param("entityIds") List<String> entityIds, @Param("profileIds") List<String> profileIds);

  List<AlterationCountByGene> getAlterationCountByGeneGivenSamplesAndMolecularProfiles(
      @Param("entities") Collection<String> entities,
      @Param("molecularProfiles") Collection<String> molecularProfiles,
      @Param("alterationFilterHelper") AlterationFilterHelper alterationFilterHelper);

  List<AlterationCountByGene> getAlterationCountByGeneGivenPatientsAndMolecularProfiles(
      @Param("entities") Collection<String> entities,
      @Param("molecularProfiles") Collection<String> molecularProfiles,
      @Param("alterationFilterHelper") AlterationFilterHelper alterationFilterHelper);

  List<MolecularProfile> getAllMolecularProfiles();
}
