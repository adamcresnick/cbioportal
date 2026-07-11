package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.legacy.model.GeneMolecularAlteration;
import org.cbioportal.legacy.model.NamespaceAttribute;
import org.cbioportal.legacy.model.NamespaceAttributeCount;
import org.cbioportal.legacy.model.Sample;
import org.cbioportal.legacy.persistence.mybatis.ClinicalAttributeMapper;
import org.cbioportal.legacy.persistence.mybatis.ClinicalDataMapper;
import org.cbioportal.legacy.persistence.mybatis.CopyNumberSegmentMapper;
import org.cbioportal.legacy.persistence.mybatis.DiscreteCopyNumberMapper;
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
