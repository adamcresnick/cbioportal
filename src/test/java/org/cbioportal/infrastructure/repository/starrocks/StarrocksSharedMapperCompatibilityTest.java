package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.legacy.model.GeneMolecularAlteration;
import org.cbioportal.legacy.model.MolecularProfileCaseIdentifier;
import org.cbioportal.legacy.model.NamespaceAttribute;
import org.cbioportal.legacy.model.NamespaceAttributeCount;
import org.cbioportal.legacy.model.Sample;
import org.cbioportal.legacy.model.util.Select;
import org.cbioportal.legacy.persistence.mybatis.AlterationCountsMapper;
import org.cbioportal.legacy.persistence.mybatis.ClinicalAttributeMapper;
import org.cbioportal.legacy.persistence.mybatis.ClinicalDataMapper;
import org.cbioportal.legacy.persistence.mybatis.CopyNumberSegmentMapper;
import org.cbioportal.legacy.persistence.mybatis.DiscreteCopyNumberMapper;
import org.cbioportal.legacy.persistence.mybatis.GenePanelMapper;
import org.cbioportal.legacy.persistence.mybatis.MolecularDataMapper;
import org.cbioportal.legacy.persistence.mybatis.NamespaceMapper;
import org.cbioportal.legacy.persistence.mybatis.PatientMapper;
import org.cbioportal.legacy.persistence.mybatis.SampleMapper;
import org.cbioportal.legacy.persistence.mybatis.TreatmentMapper;
import org.cbioportal.legacy.persistence.mybatis.typehandler.SampleTypeTypeHandler;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksSharedMapperCompatibilityTest {

  private static final String STUDY_A = "sr_study_a";
  private static final String STUDY_B = "sr_study_b";

  @Test
  void sharedMappersExecutePortableBranchesAgainstRealStarrocks() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        assertEntityArrayBindings(session);
        assertEmptyArrayBindings(session);
        assertMolecularData(session);
        assertGenePanelData(session);
        assertAlterationCounts(session);
        assertClinicalNumericSort(session);
        assertCnaSegmentsAndTreatments(session);
        assertNamespaceJson(session);
      }
    }
  }

  private static void assertEntityArrayBindings(SqlSession session) {
    ClinicalAttributeMapper attributes = session.getMapper(ClinicalAttributeMapper.class);
    assertThat(
            attributes.getClinicalAttributeCountsBySampleIds(
                List.of(STUDY_A, STUDY_B), List.of("SA1", "SB1")))
        .isNotEmpty();

    PatientMapper patients = session.getMapper(PatientMapper.class);
    assertThat(
            patients.getPatients(
                List.of(STUDY_A, STUDY_B),
                List.of("PA1", "PB1"),
                null,
                "SUMMARY",
                null,
                null,
                null,
                null))
        .extracting(org.cbioportal.legacy.model.Patient::getStableId)
        .containsExactlyInAnyOrder("PA1", "PB1");
    assertThat(patients.getPatientsOfSamples(List.of(STUDY_A, STUDY_B), List.of("SA1", "SB1")))
        .extracting(org.cbioportal.legacy.model.Patient::getStableId)
        .containsExactlyInAnyOrder("PA1", "PB1");

    SampleMapper samples = session.getMapper(SampleMapper.class);
    assertThat(
            samples.getSamples(
                List.of(STUDY_A, STUDY_B),
                null,
                List.of("SA1", "SB1"),
                null,
                "SUMMARY",
                null,
                null,
                null,
                null))
        .extracting(Sample::getStableId)
        .containsExactlyInAnyOrder("SA1", "SB1");
    assertThat(
            samples.getSamplesOfPatientsInMultipleStudies(
                List.of(STUDY_A, STUDY_B), List.of("PA1", "PB1"), "SUMMARY"))
        .extracting(Sample::getStableId)
        .contains("SA1", "SB1");

    ClinicalDataMapper clinical = session.getMapper(ClinicalDataMapper.class);
    assertThat(
            clinical.getSampleClinicalData(
                List.of(STUDY_A, STUDY_B),
                List.of("SA1", "SB1"),
                List.of("PURITY"),
                "SUMMARY",
                null,
                null,
                null,
                null))
        .hasSize(2);
    assertThat(
            clinical.getPatientClinicalData(
                List.of(STUDY_A, STUDY_B),
                List.of("PA1", "PB1"),
                List.of("AGE"),
                "SUMMARY",
                null,
                null,
                null,
                null))
        .hasSize(2);
  }

  private static void assertMolecularData(SqlSession session) throws Exception {
    MolecularDataMapper mapper = session.getMapper(MolecularDataMapper.class);
    assertThat(mapper.getCommaSeparatedSampleIdsOfMolecularProfiles(Set.of(STUDY_A + "_mrna")))
        .singleElement();
    assertThat(mapper.getGeneMolecularAlterations(STUDY_A + "_mrna", List.of(7157), "SUMMARY"))
        .singleElement()
        .satisfies(
            alteration ->
                assertThat(alteration.getSplitValues())
                    .containsExactly("1.2346", "1.235e-5", "0", "NA"));
    assertThat(
            mapper.getGeneMolecularAlterationsInMultipleMolecularProfiles(
                Set.of(STUDY_A + "_gistic", STUDY_A + "_mrna"), List.of(7157), "SUMMARY"))
        .hasSize(2);
    assertThat(
            mapper.getGenericAssayMolecularAlterations(
                STUDY_A + "_response", List.of("GA_NUMERIC"), "SUMMARY"))
        .singleElement()
        .satisfies(
            alteration ->
                assertThat(alteration.getSplitValues()).containsExactly("0.1", "0.5", "0.9", "NA"));
    assertThat(
            mapper.getGenesetMolecularAlterations(STUDY_A + "_mrna", List.of("missing"), "SUMMARY"))
        .isEmpty();

    try (Cursor<GeneMolecularAlteration> cursor =
        mapper.getGeneMolecularAlterationsIter(STUDY_A + "_mrna", List.of(7157), "SUMMARY")) {
      assertThat(cursor).hasSize(1);
    }
  }

  private static void assertEmptyArrayBindings(SqlSession session) {
    assertThat(
            session
                .getMapper(ClinicalAttributeMapper.class)
                .getClinicalAttributeCountsBySampleIds(List.of(STUDY_A), List.of()))
        .isEmpty();
    assertThat(
            session
                .getMapper(PatientMapper.class)
                .getPatients(List.of(STUDY_A), List.of(), null, "SUMMARY", null, null, null, null))
        .isEmpty();
    assertThat(
            session
                .getMapper(PatientMapper.class)
                .getPatientsOfSamples(List.of(STUDY_A), List.of()))
        .isEmpty();
    assertThat(
            session
                .getMapper(SampleMapper.class)
                .getSamples(
                    List.of(STUDY_A), null, List.of(), null, "SUMMARY", null, null, null, null))
        .isEmpty();
    assertThat(
            session
                .getMapper(SampleMapper.class)
                .getSamplesOfPatientsInMultipleStudies(List.of(STUDY_A), List.of(), "SUMMARY"))
        .isEmpty();
    assertThat(
            session
                .getMapper(ClinicalDataMapper.class)
                .getSampleClinicalData(
                    List.of(STUDY_A),
                    List.of(),
                    List.of("PURITY"),
                    "SUMMARY",
                    null,
                    null,
                    null,
                    null))
        .isEmpty();
    assertThat(
            session
                .getMapper(DiscreteCopyNumberMapper.class)
                .getDiscreteCopyNumbersInMultipleMolecularProfiles(
                    List.of(), List.of(), null, null, "SUMMARY"))
        .isEmpty();
    assertThat(
            session
                .getMapper(TreatmentMapper.class)
                .getAllTreatments(List.of(), List.of(STUDY_A), "AGENT"))
        .isEmpty();
  }

  private static void assertGenePanelData(SqlSession session) {
    GenePanelMapper mapper = session.getMapper(GenePanelMapper.class);
    assertThat(mapper.getAllGenePanels("SUMMARY", null, null, null, null))
        .singleElement()
        .satisfies(panel -> assertThat(panel.getStableId()).isEqualTo("TEST_PANEL"));
    assertThat(mapper.getMetaGenePanels().getTotalCount()).isEqualTo(1);
    assertThat(mapper.getGenePanel("TEST_PANEL", "DETAILED").getDescription()).contains("fixture");
    assertThat(mapper.fetchGenePanels(List.of("TEST_PANEL"), "SUMMARY")).hasSize(1);
    assertThat(mapper.getGenesOfPanels(List.of("TEST_PANEL")))
        .extracting(org.cbioportal.legacy.model.GenePanelToGene::getHugoGeneSymbol)
        .containsExactlyInAnyOrder("EGFR", "TP53");

    assertThat(mapper.getGenePanelDataBySampleListId(STUDY_A + "_mutations", STUDY_A + "_all"))
        .hasSize(4);
    assertThat(mapper.getGenePanelDataBySampleIds(STUDY_A + "_mutations", List.of("SA1", "SA2")))
        .allSatisfy(data -> assertThat(data.getGenePanelId()).isEqualTo("TEST_PANEL"))
        .hasSize(2);
    assertThat(
            mapper.fetchGenePanelDataByMolecularProfileIds(
                Set.of(STUDY_A + "_mutations", STUDY_A + "_gistic")))
        .hasSize(8);

    var sampleIdentifiers =
        List.of(
            profileCase(STUDY_A + "_mutations", "SA1"), profileCase(STUDY_A + "_gistic", "SA2"));
    assertThat(mapper.fetchGenePanelDataInMultipleMolecularProfiles(sampleIdentifiers))
        .extracting(data -> data.getMolecularProfileId() + "/" + data.getSampleId())
        .containsExactlyInAnyOrder(STUDY_A + "_mutations/SA1", STUDY_A + "_gistic/SA2");

    var patientIdentifiers =
        List.of(
            profileCase(STUDY_A + "_mutations", "PA1"), profileCase(STUDY_B + "_gistic", "PB1"));
    assertThat(mapper.fetchGenePanelDataInMultipleMolecularProfilesByPatientIds(patientIdentifiers))
        .extracting(data -> data.getMolecularProfileId() + "/" + data.getPatientId())
        .containsOnly(STUDY_A + "_mutations/PA1", STUDY_B + "_gistic/PB1");
  }

  private static MolecularProfileCaseIdentifier profileCase(String profileId, String caseId) {
    MolecularProfileCaseIdentifier identifier = new MolecularProfileCaseIdentifier();
    identifier.setMolecularProfileId(profileId);
    identifier.setCaseId(caseId);
    return identifier;
  }

  private static void assertAlterationCounts(SqlSession session) {
    AlterationCountsMapper mapper = session.getMapper(AlterationCountsMapper.class);
    var mutationSamples =
        List.of(
            profileCase(STUDY_A + "_mutations", "SA1"), profileCase(STUDY_B + "_mutations", "SB1"));
    var cnaSamples =
        List.of(profileCase(STUDY_A + "_gistic", "SA1"), profileCase(STUDY_B + "_gistic", "SB1"));
    var structuralVariantSamples =
        List.of(
            profileCase(STUDY_A + "_structural_variants", "SA1"),
            profileCase(STUDY_B + "_structural_variants", "SB1"));
    assertThat(mapper.getMolecularProfileCaseInternalIdentifier(mutationSamples, "SAMPLE_ID"))
        .hasSize(2);

    var mutationSampleInternal = List.of(profileCase("101", "1001"), profileCase("201", "2001"));
    var cnaSampleInternal = List.of(profileCase("102", "1001"), profileCase("202", "2001"));
    var structuralVariantSampleInternal =
        List.of(profileCase("104", "1001"), profileCase("204", "2001"));

    assertThat(
            mapper.getSampleAlterationGeneCounts(
                mutationSampleInternal,
                cnaSampleInternal,
                structuralVariantSampleInternal,
                Select.all(),
                Select.all(),
                Select.all(),
                true,
                true,
                true,
                Select.all(),
                true,
                true,
                true,
                true))
        .isNotEmpty();
    assertThat(
            mapper.getSampleCnaGeneCounts(
                cnaSampleInternal,
                Select.all(),
                Select.all(),
                true,
                true,
                true,
                Select.all(),
                true))
        .isNotEmpty();
    assertThat(
            mapper.getSampleStructuralVariantCounts(
                structuralVariantSamples, true, true, true, Select.all(), true, true, true, true))
        .isNotEmpty();
    var structuralVariantPatients =
        List.of(
            profileCase(STUDY_A + "_structural_variants", "PA1"),
            profileCase(STUDY_B + "_structural_variants", "PB1"));
    var mutationPatientInternal = List.of(profileCase("101", "101"), profileCase("201", "201"));
    var cnaPatientInternal = List.of(profileCase("102", "101"), profileCase("202", "201"));
    var structuralVariantPatientInternal =
        List.of(profileCase("104", "101"), profileCase("204", "201"));
    assertThat(
            mapper.getPatientAlterationGeneCounts(
                mutationPatientInternal,
                cnaPatientInternal,
                structuralVariantPatientInternal,
                Select.all(),
                Select.all(),
                Select.all(),
                true,
                true,
                true,
                Select.all(),
                true,
                true,
                true,
                true))
        .isNotEmpty();
    assertThat(
            mapper.getPatientCnaGeneCounts(
                cnaPatientInternal,
                Select.all(),
                Select.all(),
                true,
                true,
                true,
                Select.all(),
                true))
        .isNotEmpty();
    assertThat(
            mapper.getPatientStructuralVariantCounts(
                structuralVariantPatients, true, true, true, Select.all(), true, true, true, true))
        .isNotEmpty();
  }

  private static void assertClinicalNumericSort(SqlSession session) {
    ClinicalDataMapper mapper = session.getMapper(ClinicalDataMapper.class);
    assertThat(
            mapper.getVisibleSampleInternalIdsForClinicalTable(
                List.of(STUDY_A, STUDY_A, STUDY_A, STUDY_A),
                List.of("SA1", "SA2", "SA3", "SA4"),
                "SUMMARY",
                null,
                null,
                null,
                "PURITY",
                true,
                false,
                "ASC"))
        .containsExactly(1002, 1004, 1001, 1003);
  }

  private static void assertCnaSegmentsAndTreatments(SqlSession session) {
    DiscreteCopyNumberMapper cna = session.getMapper(DiscreteCopyNumberMapper.class);
    assertThat(
            cna.getDiscreteCopyNumbersInMultipleMolecularProfiles(
                List.of(STUDY_A + "_gistic", STUDY_B + "_gistic"),
                List.of("SA1", "SB1"),
                null,
                null,
                "SUMMARY"))
        .hasSize(2);
    assertThat(
            cna.getSampleCountByGeneAndAlterationAndSampleIds(
                STUDY_A + "_gistic", List.of("SA1", "SA2"), List.of(7157, 1956), List.of(2, -2)))
        .extracting(count -> count.getEntrezGeneId() + "/" + count.getAlteration())
        .containsExactlyInAnyOrder("7157/2", "1956/-2");
    assertThat(
            cna.getSampleCountByGeneAndAlterationAndSampleIds(
                STUDY_A + "_gistic", List.of("SA1"), List.of(), List.of()))
        .isEmpty();

    CopyNumberSegmentMapper segments = session.getMapper(CopyNumberSegmentMapper.class);
    assertThat(
            segments.getCopyNumberSegments(
                List.of(STUDY_A, STUDY_B),
                List.of("SA1", "SB1"),
                null,
                "SUMMARY",
                null,
                null,
                null,
                null))
        .hasSize(2);

    TreatmentMapper treatments = session.getMapper(TreatmentMapper.class);
    assertThat(
            treatments.getAllTreatments(List.of("SA1", "SB1"), List.of(STUDY_A, STUDY_B), "AGENT"))
        .extracting(org.cbioportal.legacy.model.Treatment::getTreatment)
        .contains("DrugA", "DrugB");
    assertThat(treatments.hasSampleTimelineData(List.of("SA1", "SB1"), List.of(STUDY_A, STUDY_B)))
        .isTrue();
  }

  private static void assertNamespaceJson(SqlSession session) {
    NamespaceMapper mapper = session.getMapper(NamespaceMapper.class);
    assertThat(mapper.getNamespaceOuterKey(List.of())).isEmpty();
    assertThat(mapper.getNamespaceOuterKey(List.of(STUDY_A)))
        .extracting(NamespaceAttribute::getOuterKey)
        .contains("oncokb");
    assertThat(mapper.getNamespaceInnerKey("oncokb", List.of(STUDY_A)))
        .extracting(NamespaceAttribute::getInnerKey)
        .contains("impact");
    NamespaceAttribute attribute = new NamespaceAttribute("oncokb", "impact");
    assertThat(
            mapper.getNamespaceAttributeCountsBySampleIds(
                List.of(STUDY_A), List.of("SA3"), List.of(attribute)))
        .singleElement()
        .extracting(NamespaceAttributeCount::getCount)
        .isEqualTo(1);
    assertThat(mapper.getNamespaceDataCounts(List.of(STUDY_A), List.of("SA3"), "oncokb", "impact"))
        .singleElement()
        .satisfies(
            count -> {
              assertThat(count.getValue()).isEqualTo("HIGH");
              assertThat(count.getCount()).isEqualTo(1);
              assertThat(count.getTotalCount()).isEqualTo(1);
            });
    assertThat(mapper.getNamespaceData(List.of(STUDY_A), List.of("SA3"), "oncokb", "impact"))
        .singleElement()
        .satisfies(data -> assertThat(data.getAttrValue()).isEqualTo("HIGH"));
    assertThat(
            mapper.getNamespaceDataForComparison(
                List.of(STUDY_A), List.of("SA3"), "oncokb", "impact", "HIGH"))
        .singleElement()
        .satisfies(
            data -> {
              assertThat(data.getSampleId()).isEqualTo("SA3");
              assertThat(data.getPatientId()).isEqualTo("PA2");
            });
  }

  private static SqlSessionFactory sessionFactory(StarrocksTestCluster cluster) throws Exception {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(cluster.jdbcUrl());
    dataSource.setUsername(cluster.username());
    dataSource.setPassword(cluster.password());

    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setDatabaseIdProvider(ignored -> "starrocks");
    factory.setTypeHandlers(new SampleTypeTypeHandler());
    factory.setMapperLocations(
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:org/cbioportal/legacy/persistence/mybatis/*.xml"));
    SqlSessionFactory sessionFactory = factory.getObject();
    sessionFactory.getConfiguration().addMappers("org.cbioportal.legacy.persistence.mybatis");
    return sessionFactory;
  }
}
