package org.cbioportal.infrastructure.repository.starrocks.alteration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.alteration.repository.AlterationRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.legacy.model.AlterationCountByGene;
import org.cbioportal.legacy.model.AlterationFilter;
import org.cbioportal.legacy.model.CopyNumberCountByGene;
import org.cbioportal.legacy.model.EnrichmentType;
import org.cbioportal.legacy.model.EntityToPanel;
import org.cbioportal.legacy.model.GenePanelToGene;
import org.cbioportal.legacy.model.MolecularProfile;
import org.cbioportal.legacy.persistence.helper.AlterationFilterHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksAlterationRepository implements AlterationRepository {

  private final StarrocksAlterationMapper mapper;

  public StarrocksAlterationRepository(StarrocksAlterationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<AlterationCountByGene> getMutatedGenes(StudyViewFilterContext context) {
    return mapper.getMutatedGenes(
        context, AlterationFilterHelper.build(context.alterationFilter()));
  }

  @Override
  public List<AlterationCountByGene> getStructuralVariantGenes(StudyViewFilterContext context) {
    return mapper.getStructuralVariantGenes(
        context, AlterationFilterHelper.build(context.alterationFilter()));
  }

  @Override
  public List<CopyNumberCountByGene> getCnaGenes(StudyViewFilterContext context) {
    return mapper.getCnaGenes(context, AlterationFilterHelper.build(context.alterationFilter()));
  }

  @Override
  public Map<String, Integer> getTotalProfiledCounts(
      StudyViewFilterContext context,
      String alterationType,
      List<MolecularProfile> molecularProfiles) {
    return mapper.getTotalProfiledCounts(context, alterationType, molecularProfiles).stream()
        .collect(
            Collectors.groupingBy(
                AlterationCountByGene::getHugoGeneSymbol,
                Collectors.summingInt(AlterationCountByGene::getNumberOfProfiledCases)));
  }

  @Override
  public Map<String, Set<String>> getMatchingGenePanelIds(
      StudyViewFilterContext context, String alterationType) {
    return mapper.getMatchingGenePanelIds(context, alterationType).stream()
        .collect(
            Collectors.groupingBy(
                GenePanelToGene::getHugoGeneSymbol,
                Collectors.mapping(GenePanelToGene::getGenePanelId, Collectors.toSet())));
  }

  @Override
  public List<EntityToPanel> getEntityToGenePanels(
      List<String> entityIds, List<String> profileIds, EnrichmentType enrichmentType) {
    if (CollectionUtils.isEmpty(entityIds) || CollectionUtils.isEmpty(profileIds)) {
      return List.of();
    }
    return enrichmentType == EnrichmentType.SAMPLE
        ? mapper.getSampleToGenePanels(entityIds, profileIds)
        : mapper.getPatientToGenePanels(entityIds, profileIds);
  }

  @Override
  public Map<String, Map<String, GenePanelToGene>> getGenePanelsToGenes() {
    return mapper.getGenePanelGenes().stream()
        .collect(
            Collectors.groupingBy(
                GenePanelToGene::getGenePanelId,
                Collectors.toMap(
                    GenePanelToGene::getHugoGeneSymbol,
                    panelGene -> panelGene,
                    (existing, replacement) -> existing)));
  }

  @Override
  public int getEntityProfileCountWithoutPanelData(
      StudyViewFilterContext context, String alterationType) {
    return mapper.getSampleProfileCountWithoutPanelData(context, alterationType);
  }

  @Override
  public List<AlterationCountByGene> getAlterationCountByGeneGivenSamplesAndMolecularProfiles(
      Collection<String> samples,
      Collection<String> molecularProfiles,
      AlterationFilter alterationFilter) {
    if (CollectionUtils.isEmpty(samples) || CollectionUtils.isEmpty(molecularProfiles)) {
      return List.of();
    }
    return mapper.getAlterationCountByGeneGivenSamplesAndMolecularProfiles(
        samples, molecularProfiles, AlterationFilterHelper.build(alterationFilter));
  }

  @Override
  public List<AlterationCountByGene> getAlterationCountByGeneGivenPatientsAndMolecularProfiles(
      Collection<String> patients,
      Collection<String> molecularProfiles,
      AlterationFilter alterationFilter) {
    if (CollectionUtils.isEmpty(patients) || CollectionUtils.isEmpty(molecularProfiles)) {
      return List.of();
    }
    return mapper.getAlterationCountByGeneGivenPatientsAndMolecularProfiles(
        patients, molecularProfiles, AlterationFilterHelper.build(alterationFilter));
  }

  @Override
  public List<MolecularProfile> getAllMolecularProfiles() {
    return mapper.getAllMolecularProfiles();
  }
}
