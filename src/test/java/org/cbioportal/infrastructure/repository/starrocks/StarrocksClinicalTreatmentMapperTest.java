package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.PortalApplication;
import org.cbioportal.application.rest.vcolumnstore.ColumnStoreClinicalDataController;
import org.cbioportal.domain.clinical_attributes.repository.ClinicalAttributesRepository;
import org.cbioportal.domain.clinical_data.ClinicalDataType;
import org.cbioportal.domain.clinical_event.repository.ClinicalEventRepository;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.domain.studyview.StudyViewFilterFactory;
import org.cbioportal.domain.treatment.repository.TreatmentRepository;
import org.cbioportal.infrastructure.repository.starrocks.clinical_attributes.StarrocksClinicalAttributesMapper;
import org.cbioportal.infrastructure.repository.starrocks.clinical_data.StarrocksClinicalDataMapper;
import org.cbioportal.infrastructure.repository.starrocks.clinical_event.StarrocksClinicalEventMapper;
import org.cbioportal.infrastructure.repository.starrocks.treatment.StarrocksTreatmentMapper;
import org.cbioportal.legacy.model.TemporalRelation;
import org.cbioportal.legacy.web.parameter.ClinicalDataFilter;
import org.cbioportal.legacy.web.parameter.ClinicalDataIdentifier;
import org.cbioportal.legacy.web.parameter.ClinicalDataMultiStudyFilter;
import org.cbioportal.legacy.web.parameter.DataFilter;
import org.cbioportal.legacy.web.parameter.DataFilterValue;
import org.cbioportal.legacy.web.parameter.SampleIdentifier;
import org.cbioportal.legacy.web.parameter.StudyViewFilter;
import org.cbioportal.legacy.web.parameter.filter.AndedPatientTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.AndedSampleTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.OredPatientTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.OredSampleTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.PatientTreatmentFilter;
import org.cbioportal.legacy.web.parameter.filter.SampleTreatmentFilter;
import org.cbioportal.shared.enums.ProjectionType;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksClinicalTreatmentMapperTest {

  @Test
  void allClinicalEventAndTreatmentStatementsExecuteAgainstFixture() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        StarrocksClinicalAttributesMapper attributes =
            session.getMapper(StarrocksClinicalAttributesMapper.class);
        StarrocksClinicalDataMapper clinicalData =
            session.getMapper(StarrocksClinicalDataMapper.class);
        StarrocksClinicalEventMapper events = session.getMapper(StarrocksClinicalEventMapper.class);
        StarrocksTreatmentMapper treatments = session.getMapper(StarrocksTreatmentMapper.class);

        assertThat(attributes.getClinicalAttributes()).hasSize(13);
        assertThat(attributes.getClinicalAttributesForStudies(List.of("sr_study_a")))
            .hasSize(7)
            .allSatisfy(
                attribute -> assertThat(attribute.cancerStudyIdentifier()).isEqualTo("sr_study_a"));
        assertThat(attributes.getClinicalAttributesForStudiesDetailed(List.of("sr_study_a")))
            .filteredOn(attribute -> attribute.attrId().equals("AGE"))
            .singleElement()
            .satisfies(
                attribute -> {
                  assertThat(attribute.displayName()).isEqualTo("Age");
                  assertThat(attribute.cancerStudyId()).isEqualTo(1);
                });

        StudyViewFilterContext studyA = context(studyFilter("sr_study_a"));
        assertThat(clinicalData.getSampleClinicalDataByStudyViewFilter(studyA, List.of("SUBTYPE")))
            .extracting(data -> data.sampleId() + ":" + data.attrValue())
            .containsExactlyInAnyOrder("SA1:A", "SA2:B", "SA3:A", "SA4:C");
        assertThat(clinicalData.getPatientClinicalDataByStudyViewFilter(studyA, List.of("AGE")))
            .extracting(data -> data.patientId() + ":" + data.attrValue())
            .containsExactlyInAnyOrder("PA1:10", "PA2:12", "PA3:8");

        List<String> sampleIds = List.of("sr_study_a_SA1");
        List<String> patientIds = List.of("sr_study_a_PA1");
        assertThat(
                clinicalData.fetchClinicalDataId(
                    sampleIds, List.of("SUBTYPE"), List.of("sr_study_a"), "sample"))
            .singleElement()
            .satisfies(data -> assertThat(data.attrValue()).isNull());
        assertThat(
                clinicalData.fetchClinicalDataSummary(
                    sampleIds, List.of("SUBTYPE"), List.of("sr_study_a"), "sample"))
            .singleElement()
            .satisfies(data -> assertThat(data.attrValue()).isEqualTo("A"));
        assertThat(
                clinicalData.fetchClinicalDataSummaryForEnrichments(
                    sampleIds,
                    patientIds,
                    List.of("SUBTYPE"),
                    List.of("AGE"),
                    List.of("OS_STATUS")))
            .extracting(data -> data.attrId())
            .containsExactlyInAnyOrder("SUBTYPE", "AGE", "OS_STATUS");
        assertThat(
                clinicalData.fetchClinicalDataDetailed(
                    sampleIds, List.of("SUBTYPE"), List.of("sr_study_a"), "sample"))
            .singleElement()
            .satisfies(
                data -> {
                  assertThat(data.clinicalAttribute()).isNotNull();
                  assertThat(data.clinicalAttribute().displayName()).isEqualTo("Subtype");
                });
        assertThat(
                clinicalData.fetchClinicalDataMeta(
                    sampleIds, List.of("SUBTYPE"), List.of("sr_study_a"), "sample"))
            .isEqualTo(1);

        assertThat(
                clinicalData.getClinicalDataCountsByStudyViewFilter(
                    studyA, List.of("SUBTYPE"), List.of("AGE"), List.of()))
            .extracting(item -> item.getAttributeId())
            .containsExactlyInAnyOrder("SUBTYPE", "AGE");
        assertThat(
                clinicalData.getClinicalDataCountsByStudyViewFilter(
                    studyA, List.of("ALL_NA"), List.of(), List.of()))
            .singleElement()
            .satisfies(
                item ->
                    assertThat(item.getCounts())
                        .singleElement()
                        .satisfies(
                            count -> {
                              assertThat(count.getValue()).isEqualTo("NA");
                              assertThat(count.getCount()).isEqualTo(4);
                            }));
        assertThat(
                clinicalData.getClinicalDataCountsForEnrichments(
                    sampleIds,
                    patientIds,
                    List.of("SUBTYPE"),
                    List.of("AGE"),
                    List.of("OS_STATUS")))
            .extracting(item -> item.getAttributeId())
            .containsExactlyInAnyOrder("SUBTYPE", "AGE", "OS_STATUS");

        assertThat(events.getClinicalEventTypeCounts(studyA))
            .extracting(event -> event.getEventType() + ":" + event.getCount())
            .containsExactly("SPECIMEN:1", "TREATMENT:1");
        assertThat(treatments.getPatientTreatments(studyA))
            .singleElement()
            .satisfies(
                treatment -> {
                  assertThat(treatment.treatment()).isEqualTo("DrugA");
                  assertThat(treatment.count()).isEqualTo(1);
                });
        assertThat(treatments.getPatientTreatmentCounts(studyA)).isEqualTo(1);
        assertThat(treatments.getTotalSampleTreatmentCounts(studyA)).isEqualTo(2);
        assertThat(treatments.getSampleTreatmentRows(studyA))
            .extracting(row -> row.sampleId() + ":" + row.temporalRelation())
            .containsExactly("SA2:Post", "SA1:Pre");

        assertStudyViewFilters(clinicalData, treatments);
      }

      assertApplicationContracts(cluster);
    }
  }

  private static void assertStudyViewFilters(
      StarrocksClinicalDataMapper clinicalData, StarrocksTreatmentMapper treatments) {
    StudyViewFilter subtype = studyFilter("sr_study_a");
    subtype.setClinicalDataFilters(List.of(categoricalFilter("SUBTYPE", "B")));
    assertThat(
            clinicalData.getSampleClinicalDataByStudyViewFilter(
                context(subtype), List.of("SUBTYPE")))
        .extracting(data -> data.sampleId())
        .containsExactly("SA2");

    StudyViewFilter age = studyFilter("sr_study_a");
    age.setClinicalDataFilters(List.of(numericFilter("AGE", "12", "12")));
    assertThat(
            clinicalData.getSampleClinicalDataByStudyViewFilter(context(age), List.of("SUBTYPE")))
        .extracting(data -> data.sampleId())
        .containsExactly("SA3");

    StudyViewFilter mixedAge = studyFilter("sr_study_a");
    ClinicalDataFilter mixedAgeFilter = new ClinicalDataFilter();
    mixedAgeFilter.setAttributeId("AGE");
    mixedAgeFilter.setValues(
        List.of(
            new DataFilterValue(new BigDecimal("8"), new BigDecimal("12")),
            new DataFilterValue("unknown")));
    mixedAge.setClinicalDataFilters(List.of(mixedAgeFilter));
    assertThat(
            clinicalData.getSampleClinicalDataByStudyViewFilter(
                context(mixedAge), List.of("SUBTYPE")))
        .extracting(data -> data.sampleId())
        .containsExactly("SA1", "SA2", "SA3");

    StudyViewFilter oneSiblingSample = studyFilter("sr_study_a");
    oneSiblingSample.setSampleIdentifiers(List.of(identifier("sr_study_a", "SA1")));
    assertThat(
            clinicalData.getClinicalDataCountsByStudyViewFilter(
                context(oneSiblingSample), List.of(), List.of(), List.of("AGE")))
        .singleElement()
        .satisfies(
            item ->
                assertThat(item.getCounts())
                    .singleElement()
                    .satisfies(
                        count -> {
                          assertThat(count.getValue()).isEqualTo("10");
                          assertThat(count.getCount()).isEqualTo(1);
                        }));

    StudyViewFilter missing = studyFilter("sr_study_a");
    missing.setClinicalDataFilters(List.of(categoricalFilter("SPECIAL", "NA")));
    assertThat(
            clinicalData.getSampleClinicalDataByStudyViewFilter(
                context(missing), List.of("SUBTYPE")))
        .extracting(data -> data.sampleId())
        .containsExactlyInAnyOrder("SA1", "SA2", "SA3");

    StudyViewFilter event = studyFilter("sr_study_a");
    DataFilter eventFilter = new DataFilter();
    eventFilter.setValues(List.of(new DataFilterValue("treatment")));
    event.setClinicalEventFilters(List.of(eventFilter));
    assertThat(
            clinicalData.getSampleClinicalDataByStudyViewFilter(context(event), List.of("SUBTYPE")))
        .extracting(data -> data.sampleId())
        .containsExactly("SA1", "SA2");

    StudyViewFilter patientTreatment = studyFilter("sr_study_a");
    patientTreatment.setPatientTreatmentFilters(patientTreatmentFilter("DrugA"));
    assertThat(treatments.getPatientTreatmentCounts(context(patientTreatment))).isEqualTo(1);

    StudyViewFilter preTreatment = studyFilter("sr_study_a");
    preTreatment.setSampleTreatmentFilters(sampleTreatmentFilter("DrugA", TemporalRelation.Pre));
    assertThat(treatments.getTotalSampleTreatmentCounts(context(preTreatment))).isEqualTo(1);
    assertThat(treatments.getSampleTreatmentRows(context(preTreatment)))
        .extracting(row -> row.sampleId())
        .containsExactly("SA1");
  }

  private static void assertApplicationContracts(StarrocksTestCluster cluster) {
    try (ConfigurableApplicationContext context = applicationContext(cluster)) {
      ColumnStoreClinicalDataController clinicalData =
          context.getBean(ColumnStoreClinicalDataController.class);
      ClinicalDataIdentifier identifier = new ClinicalDataIdentifier();
      identifier.setStudyId("sr_study_a");
      identifier.setEntityId("SA1");
      ClinicalDataMultiStudyFilter filter = new ClinicalDataMultiStudyFilter();
      filter.setIdentifiers(List.of(identifier));
      filter.setAttributeIds(List.of("SUBTYPE"));
      assertThat(
              clinicalData
                  .fetchClinicalData(ClinicalDataType.SAMPLE, filter, ProjectionType.SUMMARY)
                  .getBody())
          .singleElement()
          .satisfies(
              data -> {
                assertThat(data.sampleId()).isEqualTo("SA1");
                assertThat(data.value()).isEqualTo("A");
              });

      StudyViewFilterContext studyA = context(studyFilter("sr_study_a"));
      assertThat(
              context
                  .getBean(ClinicalAttributesRepository.class)
                  .getClinicalAttributesForStudies(List.of("sr_study_a")))
          .hasSize(7);
      assertThat(context.getBean(ClinicalEventRepository.class).getClinicalEventTypeCounts(studyA))
          .hasSize(2);
      TreatmentRepository treatments = context.getBean(TreatmentRepository.class);
      assertThat(treatments.getPatientTreatments(studyA))
          .singleElement()
          .satisfies(treatment -> assertThat(treatment.treatment()).isEqualTo("DrugA"));
      assertThat(treatments.getSampleTreatments(studyA, ProjectionType.DETAILED))
          .singleElement()
          .satisfies(
              treatment -> {
                assertThat(treatment.preSamples()).hasSize(1);
                assertThat(treatment.postSamples()).hasSize(1);
              });
    }
  }

  private static ClinicalDataFilter categoricalFilter(String attributeId, String value) {
    ClinicalDataFilter filter = new ClinicalDataFilter();
    filter.setAttributeId(attributeId);
    filter.setValues(List.of(new DataFilterValue(value)));
    return filter;
  }

  private static ClinicalDataFilter numericFilter(String attributeId, String start, String end) {
    ClinicalDataFilter filter = new ClinicalDataFilter();
    filter.setAttributeId(attributeId);
    filter.setValues(List.of(new DataFilterValue(new BigDecimal(start), new BigDecimal(end))));
    return filter;
  }

  private static AndedPatientTreatmentFilters patientTreatmentFilter(String treatment) {
    PatientTreatmentFilter filter = new PatientTreatmentFilter();
    filter.setTreatment(treatment);
    OredPatientTreatmentFilters ored = new OredPatientTreatmentFilters();
    ored.setFilters(List.of(filter));
    AndedPatientTreatmentFilters anded = new AndedPatientTreatmentFilters();
    anded.setFilters(List.of(ored));
    return anded;
  }

  private static AndedSampleTreatmentFilters sampleTreatmentFilter(
      String treatment, TemporalRelation relation) {
    SampleTreatmentFilter filter = new SampleTreatmentFilter();
    filter.setTreatment(treatment);
    filter.setTime(relation);
    OredSampleTreatmentFilters ored = new OredSampleTreatmentFilters();
    ored.setFilters(List.of(filter));
    AndedSampleTreatmentFilters anded = new AndedSampleTreatmentFilters();
    anded.setFilters(List.of(ored));
    return anded;
  }

  private static StudyViewFilter studyFilter(String studyId) {
    StudyViewFilter filter = new StudyViewFilter();
    filter.setStudyIds(List.of(studyId));
    return filter;
  }

  private static SampleIdentifier identifier(String studyId, String sampleId) {
    SampleIdentifier identifier = new SampleIdentifier();
    identifier.setStudyId(studyId);
    identifier.setSampleId(sampleId);
    return identifier;
  }

  private static StudyViewFilterContext context(StudyViewFilter filter) {
    return StudyViewFilterFactory.make(filter, null, filter.getStudyIds(), null);
  }

  private static SqlSessionFactory sessionFactory(StarrocksTestCluster cluster) throws Exception {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(cluster.jdbcUrl());
    dataSource.setUsername(cluster.username());
    dataSource.setPassword(cluster.password());

    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setMapperLocations(
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:mappers/starrocks/**/*.xml"));
    return factory.getObject();
  }

  private static ConfigurableApplicationContext applicationContext(StarrocksTestCluster cluster) {
    return new SpringApplicationBuilder(PortalApplication.class)
        .web(WebApplicationType.NONE)
        .run(
            "--columnstore.backend=starrocks",
            "--spring.datasource.url=" + cluster.jdbcUrl(),
            "--spring.datasource.username=" + cluster.username(),
            "--spring.datasource.password=" + cluster.password(),
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--authenticate=false",
            "--bitly.access.token=",
            "--patient_view.url=",
            "--sample_view.url=");
  }
}
