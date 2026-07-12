package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.SqlSession;
import org.cbioportal.legacy.model.CNA;
import org.cbioportal.legacy.model.DataAccessToken;
import org.cbioportal.legacy.model.GeneFilterQuery;
import org.cbioportal.legacy.model.GeneMolecularAlteration;
import org.cbioportal.legacy.model.GenericAssayMolecularAlteration;
import org.cbioportal.legacy.model.User;
import org.cbioportal.legacy.persistence.mybatis.AlterationDriverAnnotationMapper;
import org.cbioportal.legacy.persistence.mybatis.CancerTypeMapper;
import org.cbioportal.legacy.persistence.mybatis.ClinicalAttributeMapper;
import org.cbioportal.legacy.persistence.mybatis.ClinicalDataMapper;
import org.cbioportal.legacy.persistence.mybatis.CopyNumberSegmentMapper;
import org.cbioportal.legacy.persistence.mybatis.DataAccessTokenMapper;
import org.cbioportal.legacy.persistence.mybatis.DiscreteCopyNumberMapper;
import org.cbioportal.legacy.persistence.mybatis.GeneMapper;
import org.cbioportal.legacy.persistence.mybatis.GenericAssayMapper;
import org.cbioportal.legacy.persistence.mybatis.GenesetHierarchyMapper;
import org.cbioportal.legacy.persistence.mybatis.GenesetMapper;
import org.cbioportal.legacy.persistence.mybatis.InfoMapper;
import org.cbioportal.legacy.persistence.mybatis.MolecularDataMapper;
import org.cbioportal.legacy.persistence.mybatis.MolecularProfileMapper;
import org.cbioportal.legacy.persistence.mybatis.PatientMapper;
import org.cbioportal.legacy.persistence.mybatis.ReferenceGenomeGeneMapper;
import org.cbioportal.legacy.persistence.mybatis.ResourceDataMapper;
import org.cbioportal.legacy.persistence.mybatis.ResourceDefinitionMapper;
import org.cbioportal.legacy.persistence.mybatis.SampleListMapper;
import org.cbioportal.legacy.persistence.mybatis.SampleMapper;
import org.cbioportal.legacy.persistence.mybatis.SecurityMapper;
import org.cbioportal.legacy.persistence.mybatis.SignificantCopyNumberRegionMapper;
import org.cbioportal.legacy.persistence.mybatis.SignificantlyMutatedGeneMapper;
import org.cbioportal.legacy.persistence.mybatis.StaticDataTimestampMapper;
import org.cbioportal.legacy.persistence.mybatis.StudyMapper;
import org.cbioportal.legacy.persistence.mybatis.TreatmentMapper;
import org.cbioportal.legacy.persistence.mybatis.VariantCountMapper;
import org.junit.jupiter.api.Test;

class StarrocksRemainingSharedMapperCompatibilityTest {

  private static final String STUDY_A = "sr_study_a";
  private static final String STUDY_B = "sr_study_b";

  @Test
  void allRemainingSharedMapperStatementsExecuteAgainstRealStarrocks() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session =
          StarrocksSharedMapperCompatibilityTest.sessionFactory(cluster).openSession(true)) {
        assertReferenceAndStudyContracts(session);
        assertClinicalAndCaseContracts(session);
        assertAssayAndAnalysisContracts(session);
        assertSecurityAndTokenContracts(session);
      }
    }
  }

  private static void assertReferenceAndStudyContracts(SqlSession session) {
    GeneMapper genes = session.getMapper(GeneMapper.class);
    assertThat(genes.getGenes(null, null, "SUMMARY", null, null, null, null)).hasSize(3);
    assertThat(genes.getMetaGenes(null, null).getTotalCount()).isEqualTo(3);
    assertThat(genes.getGeneByGeneticEntityId(1, "SUMMARY").getHugoGeneSymbol()).isEqualTo("TP53");
    assertThat(genes.getGeneByEntrezGeneId(7157, "SUMMARY").getHugoGeneSymbol()).isEqualTo("TP53");
    assertThat(genes.getGeneByHugoGeneSymbol("TP53", "SUMMARY").getEntrezGeneId()).isEqualTo(7157);
    assertThat(genes.getAliasesOfGeneByEntrezGeneId(7157)).containsExactly("P53");
    assertThat(genes.getAliasesOfGeneByHugoGeneSymbol("TP53")).containsExactly("P53");
    assertThat(genes.getAllAliases()).hasSize(3);
    assertThat(genes.getGenesByEntrezGeneIds(List.of(7157, 1956), "SUMMARY")).hasSize(2);
    assertThat(genes.getGenesByHugoGeneSymbols(List.of("TP53", "EGFR"), "SUMMARY")).hasSize(2);
    assertThat(genes.getMetaGenesByEntrezGeneIds(List.of(7157, 1956)).getTotalCount()).isEqualTo(2);
    assertThat(genes.getMetaGenesByHugoGeneSymbols(List.of("TP53", "EGFR")).getTotalCount())
        .isEqualTo(2);

    CancerTypeMapper cancerTypes = session.getMapper(CancerTypeMapper.class);
    assertThat(cancerTypes.getAllCancerTypes("SUMMARY", null, null, null, null)).hasSize(3);
    assertThat(cancerTypes.getMetaCancerTypes().getTotalCount()).isEqualTo(3);
    assertThat(cancerTypes.getCancerType("sr_a", "SUMMARY").getName())
        .isEqualTo("StarRocks Study A");

    StudyMapper studies = session.getMapper(StudyMapper.class);
    assertThat(studies.getStudies(List.of(STUDY_A), null, "SUMMARY", null, null, null, null))
        .hasSize(1);
    assertThat(studies.getMetaStudies(List.of(STUDY_A), null).getTotalCount()).isEqualTo(1);
    assertThat(studies.getStudy(STUDY_A, "SUMMARY").getCancerStudyIdentifier()).isEqualTo(STUDY_A);
    assertThat(studies.getTags(STUDY_A).getCancerStudyId()).isEqualTo(1);
    assertThat(studies.getTagsForMultipleStudies(List.of(STUDY_A, STUDY_B))).hasSize(2);
    assertThat(studies.getResourceCounts(List.of(STUDY_A))).isNotEmpty();

    SampleListMapper sampleLists = session.getMapper(SampleListMapper.class);
    assertThat(sampleLists.getAllSampleLists(List.of(STUDY_A), "SUMMARY", null, null, null, null))
        .hasSize(3);
    assertThat(sampleLists.getMetaSampleLists(STUDY_A).getTotalCount()).isEqualTo(3);
    assertThat(sampleLists.getSampleList(STUDY_A + "_all", "SUMMARY").getStableId())
        .endsWith("_all");
    assertThat(sampleLists.getSampleLists(List.of(STUDY_A + "_all"), "SUMMARY")).hasSize(1);
    assertThat(sampleLists.getAllSampleIdsInSampleList(STUDY_A + "_all")).hasSize(4);
    assertThat(sampleLists.getSampleListSampleIds(List.of(11))).hasSize(4);

    MolecularProfileMapper profiles = session.getMapper(MolecularProfileMapper.class);
    assertThat(
            profiles.getAllMolecularProfilesInStudies(
                List.of(STUDY_A), "SUMMARY", null, null, null, null))
        .hasSize(6);
    assertThat(profiles.getMetaMolecularProfilesInStudies(List.of(STUDY_A)).getTotalCount())
        .isEqualTo(6);
    assertThat(profiles.getMolecularProfile(STUDY_A + "_mrna", "SUMMARY").getStableId())
        .endsWith("_mrna");
    assertThat(profiles.getMolecularProfiles(Set.of(STUDY_A + "_mrna"), "SUMMARY")).hasSize(1);
    assertThat(profiles.getMetaMolecularProfiles(Set.of(STUDY_A + "_mrna")).getTotalCount())
        .isEqualTo(1);
    assertThat(profiles.getMolecularProfilesReferredBy(STUDY_A + "_mrna", "SUMMARY")).hasSize(1);
    assertThat(profiles.getMolecularProfilesReferringTo(STUDY_A + "_gistic", "SUMMARY")).hasSize(1);

    ReferenceGenomeGeneMapper referenceGenes = session.getMapper(ReferenceGenomeGeneMapper.class);
    assertThat(referenceGenes.getAllGenesByGenomeName("hg38", "SUMMARY")).hasSize(3);
    assertThat(
            referenceGenes.getGenesByHugoGeneSymbolsAndGenomeName(
                List.of("TP53"), "hg38", "SUMMARY"))
        .hasSize(1);
    assertThat(referenceGenes.getGenesByGenomeName(List.of(7157), "hg38", "SUMMARY")).hasSize(1);
    assertThat(referenceGenes.getReferenceGenomeGene(7157, "hg38", "SUMMARY")).isNotNull();
    assertThat(referenceGenes.getReferenceGenomeGeneByEntityId(1, "hg38", "SUMMARY")).isNotNull();

    ResourceDefinitionMapper definitions = session.getMapper(ResourceDefinitionMapper.class);
    assertThat(definitions.getResourceDefinition(STUDY_A, "fixture-study", "SUMMARY")).isNotNull();
    assertThat(
            definitions.getResourceDefinitions(List.of(STUDY_A), "SUMMARY", null, null, null, null))
        .hasSize(3);
    ResourceDataMapper resources = session.getMapper(ResourceDataMapper.class);
    assertThat(
            resources.getResourceDataForStudy(
                STUDY_A, "fixture-study", "SUMMARY", null, null, null, null))
        .hasSize(1);
    assertThat(
            resources.getResourceDataOfPatientInStudy(
                STUDY_A, "PA1", "fixture-patient", "SUMMARY", null, null, null, null))
        .hasSize(1);
    assertThat(
            resources.getResourceDataOfSampleInStudy(
                STUDY_A, "SA1", "fixture-sample", "SUMMARY", null, null, null, null))
        .hasSize(1);
  }

  private static void assertClinicalAndCaseContracts(SqlSession session) {
    ClinicalAttributeMapper attributes = session.getMapper(ClinicalAttributeMapper.class);
    assertThat(
            attributes.getClinicalAttributes(List.of(STUDY_A), "SUMMARY", null, null, null, null))
        .isNotEmpty();
    assertThat(attributes.getMetaClinicalAttributes(List.of(STUDY_A)).getTotalCount())
        .isGreaterThan(0);
    assertThat(attributes.getClinicalAttribute(STUDY_A, "AGE", "SUMMARY")).isNotNull();
    assertThat(attributes.getClinicalAttributeCountsBySampleListId(STUDY_A + "_all")).isNotEmpty();
    assertThat(
            attributes.getClinicalAttributesByStudyIdsAndAttributeIds(
                List.of(STUDY_A), List.of("AGE")))
        .hasSize(1);

    ClinicalDataMapper clinical = session.getMapper(ClinicalDataMapper.class);
    assertThat(
            clinical
                .getMetaSampleClinicalData(List.of(STUDY_A), List.of("SA1"), List.of("PURITY"))
                .getTotalCount())
        .isEqualTo(1);
    assertThat(
            clinical
                .getMetaPatientClinicalData(List.of(STUDY_A), List.of("PA1"), List.of("AGE"))
                .getTotalCount())
        .isEqualTo(1);
    assertThat(
            clinical.fetchSampleClinicalDataCounts(
                List.of(STUDY_A), List.of("SA1"), List.of("PURITY")))
        .isNotEmpty();
    assertThat(
            clinical.fetchPatientClinicalDataCounts(
                List.of(STUDY_A), List.of("PA1"), List.of("AGE"), "SUMMARY"))
        .isNotEmpty();
    assertThat(
            clinical.getPatientClinicalDataDetailedToSample(
                List.of(STUDY_A),
                List.of("PA1"),
                List.of("AGE"),
                "DETAILED",
                null,
                null,
                null,
                null))
        .isNotEmpty();
    assertThat(
            clinical.getSampleClinicalTable(
                List.of(STUDY_A),
                List.of("SA1"),
                "SUMMARY",
                null,
                null,
                null,
                null,
                null,
                null,
                null))
        .isNotEmpty();
    assertThat(
            clinical.getSampleClinicalTableCount(
                List.of(STUDY_A), List.of("SA1"), "SUMMARY", null, null, null))
        .isGreaterThan(0);
    assertThat(clinical.getSampleClinicalDataBySampleInternalIds(List.of(1001))).isNotEmpty();
    assertThat(clinical.getPatientClinicalDataBySampleInternalIds(List.of(1001))).isNotEmpty();

    PatientMapper patients = session.getMapper(PatientMapper.class);
    assertThat(patients.getMetaPatients(List.of(STUDY_A), List.of("PA1"), null).getTotalCount())
        .isEqualTo(1);
    assertThat(patients.getPatient(STUDY_A, "PA1", "SUMMARY")).isNotNull();

    SampleMapper samples = session.getMapper(SampleMapper.class);
    assertThat(samples.getSampleByInternalId(1001, "SUMMARY")).isNotNull();
    assertThat(samples.getSamplesByInternalIds(List.of(1001), "SUMMARY")).hasSize(1);
    assertThat(samples.getSamplesOfPatients(STUDY_A, List.of("PA1"), "SUMMARY")).hasSize(2);

    CopyNumberSegmentMapper segments = session.getMapper(CopyNumberSegmentMapper.class);
    assertThat(segments.getSamplesWithCopyNumberSegments(List.of(STUDY_A), List.of("SA1"), null))
        .containsExactly(1001);
    assertThat(
            segments
                .getMetaCopyNumberSegments(List.of(STUDY_A), List.of("SA1"), null)
                .getTotalCount())
        .isEqualTo(1);
    assertThat(
            segments.getCopyNumberSegmentsBySampleListId(
                STUDY_A, STUDY_A + "_all", null, "SUMMARY"))
        .hasSize(1);

    TreatmentMapper treatments = session.getMapper(TreatmentMapper.class);
    assertThat(treatments.getAllSamples(List.of("SA1"), List.of(STUDY_A))).isNotEmpty();
    assertThat(treatments.getAllShallowSamples(List.of("SA1"), List.of(STUDY_A))).isNotEmpty();
    assertThat(treatments.hasTreatmentData(List.of("SA1"), List.of(STUDY_A), "AGENT")).isTrue();
  }

  private static void assertAssayAndAnalysisContracts(SqlSession session) throws Exception {
    GenericAssayMapper generic = session.getMapper(GenericAssayMapper.class);
    assertThat(generic.getGenericAssayMeta(List.of("GA_NUMERIC"))).hasSize(1);
    assertThat(generic.getGenericAssayAdditionalproperties(List.of("GA_NUMERIC"), List.of("UNIT")))
        .hasSize(1);
    assertThat(
            generic.getMolecularProfileInternalIdsByMolecularProfileIds(
                List.of(STUDY_A + "_response")))
        .containsExactly(105);
    assertThat(generic.getGeneticEntityIdsByMolecularProfileInternalIds(List.of(105)))
        .containsExactly(1001);
    assertThat(generic.getGenericAssayStableIdsByGeneticEntityIds(List.of(1001)))
        .containsExactly("GA_NUMERIC");

    GenesetMapper genesets = session.getMapper(GenesetMapper.class);
    assertThat(genesets.getGenesets("SUMMARY", null, null, null, null)).hasSize(1);
    assertThat(genesets.getMetaGenesets().getTotalCount()).isEqualTo(1);
    assertThat(genesets.getGenesetByGenesetId("HALLMARK_FIXTURE", "SUMMARY")).isNotNull();
    assertThat(genesets.fetchGenesets(List.of("HALLMARK_FIXTURE"))).hasSize(1);
    assertThat(genesets.getGenesByGenesetId("HALLMARK_FIXTURE", "SUMMARY")).hasSize(2);
    assertThat(genesets.getGenesetVersion()).isEqualTo("fixture");

    GenesetHierarchyMapper hierarchy = session.getMapper(GenesetHierarchyMapper.class);
    assertThat(hierarchy.getGenesetHierarchyParents(List.of("HALLMARK_FIXTURE"))).hasSize(1);
    assertThat(hierarchy.getGenesetHierarchyGenesets(2)).hasSize(1);
    assertThat(hierarchy.getGenesetHierarchySuperNodes(List.of("HALLMARK_FIXTURE"))).isNotEmpty();

    DiscreteCopyNumberMapper cna = session.getMapper(DiscreteCopyNumberMapper.class);
    assertThat(
            cna.getDiscreteCopyNumbersBySampleListId(
                STUDY_A + "_gistic", STUDY_A + "_all", List.of(7157), List.of(2), "SUMMARY"))
        .hasSize(1);
    assertThat(
            cna.getMetaDiscreteCopyNumbersBySampleListId(
                    STUDY_A + "_gistic", STUDY_A + "_all", List.of(7157), List.of(2))
                .getTotalCount())
        .isEqualTo(1);
    assertThat(
            cna.getDiscreteCopyNumbersBySampleIds(
                STUDY_A + "_gistic", List.of("SA1"), List.of(7157), List.of(2), "SUMMARY"))
        .hasSize(1);
    GeneFilterQuery geneQuery = new GeneFilterQuery();
    geneQuery.setEntrezGeneId(7157);
    geneQuery.setAlterations(List.of(CNA.AMP));
    assertThat(
            cna.getDiscreteCopyNumbersInMultipleMolecularProfilesByGeneQueries(
                List.of(STUDY_A + "_gistic"), List.of("SA1"), "SUMMARY", List.of(geneQuery)))
        .hasSize(1);
    assertThat(
            cna.getMetaDiscreteCopyNumbersBySampleIds(
                    STUDY_A + "_gistic", List.of("SA1"), List.of(7157), List.of(2))
                .getTotalCount())
        .isEqualTo(1);

    MolecularDataMapper molecular = session.getMapper(MolecularDataMapper.class);
    try (Cursor<GeneMolecularAlteration> cursor =
        molecular.getGeneMolecularAlterationsIterFast(STUDY_A + "_mrna")) {
      assertThat(cursor).isNotEmpty();
    }
    try (Cursor<GenericAssayMolecularAlteration> cursor =
        molecular.getGenericAssayMolecularAlterationsIter(
            STUDY_A + "_response", List.of("GA_NUMERIC"), "SUMMARY")) {
      assertThat(cursor).hasSize(1);
    }

    SignificantCopyNumberRegionMapper gistic =
        session.getMapper(SignificantCopyNumberRegionMapper.class);
    assertThat(gistic.getSignificantCopyNumberRegions(STUDY_A, "SUMMARY", null, null, null, null))
        .hasSize(1);
    assertThat(gistic.getMetaSignificantCopyNumberRegions(STUDY_A).getTotalCount()).isEqualTo(1);
    assertThat(gistic.getGenesOfRegions(List.of(1L))).hasSize(1);

    SignificantlyMutatedGeneMapper mutSig = session.getMapper(SignificantlyMutatedGeneMapper.class);
    assertThat(mutSig.getSignificantlyMutatedGenes(STUDY_A, "SUMMARY", null, null, null, null))
        .hasSize(1);
    assertThat(mutSig.getMetaSignificantlyMutatedGenes(STUDY_A).getTotalCount()).isEqualTo(1);

    VariantCountMapper variants = session.getMapper(VariantCountMapper.class);
    assertThat(
            variants.fetchVariantCounts(
                STUDY_A + "_mutations", List.of(7157), List.of("TP53 R175")))
        .hasSize(1);
    assertThat(
            session
                .getMapper(AlterationDriverAnnotationMapper.class)
                .getAlterationDriverAnnotations(List.of(STUDY_A + "_mutations")))
        .hasSize(2);
    assertThat(session.getMapper(InfoMapper.class).getInfo().getDbSchemaVersion())
        .isEqualTo("2.14.5");
    assertThat(
            session
                .getMapper(StaticDataTimestampMapper.class)
                .getTimestamps(List.of("gene"), "cbioportal"))
        .isNotEmpty();
  }

  private static void assertSecurityAndTokenContracts(SqlSession session) {
    SecurityMapper security = session.getMapper(SecurityMapper.class);
    User user = new User("fixture.user@example.org", "Fixture User", true);
    security.addPortalUser(user);
    security.addPortalUserAuthority(user.getEmail(), "ROLE_USER");
    assertThat(security.getPortalUser(user.getEmail()).getName()).isEqualTo("Fixture User");
    assertThat(security.getPortalUserAuthorities(user.getEmail()).getAuthorities())
        .contains("ROLE_USER");
    assertThat(security.getCancerStudyGroups(1)).isEqualTo("fixture");

    DataAccessTokenMapper tokens = session.getMapper(DataAccessTokenMapper.class);
    Date creation = Date.from(Instant.parse("2026-07-11T00:00:00Z"));
    Date expiration = Date.from(Instant.parse("2026-07-12T00:00:00Z"));
    tokens.addDataAccessToken(
        new DataAccessToken("fixture-token-1", user.getEmail(), expiration, creation));
    assertThat(tokens.getDataAccessToken("fixture-token-1")).isNotNull();
    assertThat(tokens.getAllDataAccessTokensForUsername(user.getEmail())).hasSize(1);
    tokens.removeDataAccessToken("fixture-token-1");
    assertThat(tokens.getDataAccessToken("fixture-token-1")).isNull();
    tokens.addDataAccessToken(
        new DataAccessToken("fixture-token-2", user.getEmail(), expiration, creation));
    tokens.removeAllDataAccessTokensForUsername(user.getEmail());
    assertThat(tokens.getAllDataAccessTokensForUsername(user.getEmail())).isEmpty();
  }
}
