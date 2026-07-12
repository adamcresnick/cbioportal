package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.domain.studyview.StudyViewFilterFactory;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleMapper;
import org.cbioportal.legacy.model.TemporalRelation;
import org.cbioportal.legacy.web.parameter.ClinicalDataFilter;
import org.cbioportal.legacy.web.parameter.CustomSampleIdentifier;
import org.cbioportal.legacy.web.parameter.DataFilterValue;
import org.cbioportal.legacy.web.parameter.StudyViewFilter;
import org.cbioportal.legacy.web.parameter.filter.AndedPatientTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.AndedSampleTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.OredPatientTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.OredSampleTreatmentFilters;
import org.cbioportal.legacy.web.parameter.filter.PatientTreatmentFilter;
import org.cbioportal.legacy.web.parameter.filter.SampleTreatmentFilter;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksResidualStudyViewFilterMapperTest {

  private static final String STUDY = "sr_study_a";

  @Test
  void residualFilterFamiliesExecuteAgainstRealStarrocks() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        try (var statement = session.getConnection().createStatement()) {
          statement.executeUpdate(
              """
              INSERT INTO clinical_event_data_derived VALUES
                ('sr_study_a_PA1', 'AGENT', 'Combination, Alpha', 50, 75,
                 'TREATMENT', 'sr_study_a')
              """);
        }
        StarrocksSampleMapper mapper = session.getMapper(StarrocksSampleMapper.class);

        assertSamples(mapper, patientFilter("DrugA", TreatmentKey.AGENT), "SA1", "SA2");
        assertSamples(
            mapper, patientFilter("Combination, Alpha", TreatmentKey.AGENT), "SA1", "SA2");
        assertSamples(mapper, patientFilter("Alpha", TreatmentKey.AGENT));
        assertSamples(mapper, patientFilter("Chemotherapy", TreatmentKey.GROUP), "SA1", "SA2");
        assertSamples(mapper, patientFilter("EGFR", TreatmentKey.TARGET), "SA1", "SA2");
        assertSamples(mapper, patientFilter("GF", TreatmentKey.TARGET));
        assertSamples(mapper, emptyPatientTreatmentGroup(), "SA1", "SA2", "SA3", "SA4");

        assertSamples(
            mapper, sampleFilter("DrugA", TemporalRelation.Pre, TreatmentKey.AGENT), "SA1");
        assertSamples(mapper, sampleFilter("Alpha", TemporalRelation.Pre, TreatmentKey.AGENT));
        assertSamples(
            mapper, sampleFilter("Chemotherapy", TemporalRelation.Pre, TreatmentKey.GROUP), "SA1");
        assertSamples(
            mapper, sampleFilter("EGFR", TemporalRelation.Post, TreatmentKey.TARGET), "SA2");

        CustomSampleIdentifier selected = customIdentifier("SA1", false);
        CustomSampleIdentifier filteredOut = customIdentifier("SA2", true);
        assertSamples(mapper, customFilter(false, selected, filteredOut), "SA1");
        assertSamples(mapper, customFilter(true, selected, filteredOut), "SA1", "SA3", "SA4");
        assertSamples(mapper, customFilter(false));
        assertSamples(mapper, customFilter(true), "SA1", "SA2", "SA3", "SA4");
      }
    }
  }

  private static void assertSamples(
      StarrocksSampleMapper mapper, StudyViewFilterContext context, String... sampleIds) {
    assertThat(mapper.getFilteredSamples(context))
        .extracting(sample -> sample.stableId())
        .containsExactly(sampleIds);
    assertThat(mapper.getSampleCount(context)).isEqualTo(sampleIds.length);
  }

  private static StudyViewFilterContext patientFilter(String treatment, TreatmentKey key) {
    PatientTreatmentFilter filter = new PatientTreatmentFilter();
    filter.setTreatment(treatment);
    OredPatientTreatmentFilters ored = new OredPatientTreatmentFilters();
    ored.setFilters(List.of(filter));
    AndedPatientTreatmentFilters anded = new AndedPatientTreatmentFilters();
    anded.setFilters(List.of(ored));

    StudyViewFilter studyViewFilter = baseFilter();
    switch (key) {
      case AGENT -> studyViewFilter.setPatientTreatmentFilters(anded);
      case GROUP -> studyViewFilter.setPatientTreatmentGroupFilters(anded);
      case TARGET -> studyViewFilter.setPatientTreatmentTargetFilters(anded);
    }
    return context(studyViewFilter, null);
  }

  private static StudyViewFilterContext sampleFilter(
      String treatment, TemporalRelation time, TreatmentKey key) {
    SampleTreatmentFilter filter = new SampleTreatmentFilter();
    filter.setTreatment(treatment);
    filter.setTime(time);
    OredSampleTreatmentFilters ored = new OredSampleTreatmentFilters();
    ored.setFilters(List.of(filter));
    AndedSampleTreatmentFilters anded = new AndedSampleTreatmentFilters();
    anded.setFilters(List.of(ored));

    StudyViewFilter studyViewFilter = baseFilter();
    switch (key) {
      case AGENT -> studyViewFilter.setSampleTreatmentFilters(anded);
      case GROUP -> studyViewFilter.setSampleTreatmentGroupFilters(anded);
      case TARGET -> studyViewFilter.setSampleTreatmentTargetFilters(anded);
    }
    return context(studyViewFilter, null);
  }

  private static StudyViewFilterContext emptyPatientTreatmentGroup() {
    OredPatientTreatmentFilters emptyGroup = new OredPatientTreatmentFilters();
    emptyGroup.setFilters(List.of());
    AndedPatientTreatmentFilters anded = new AndedPatientTreatmentFilters();
    anded.setFilters(List.of(emptyGroup));
    StudyViewFilter filter = baseFilter();
    filter.setPatientTreatmentGroupFilters(anded);
    return context(filter, null);
  }

  private static StudyViewFilterContext customFilter(
      boolean includeNa, CustomSampleIdentifier... customSampleIdentifiers) {
    ClinicalDataFilter customFilter = new ClinicalDataFilter();
    customFilter.setAttributeId("CUSTOM_SCORE");
    customFilter.setValues(
        includeNa
            ? List.of(new DataFilterValue("selected"), new DataFilterValue("NA"))
            : List.of(new DataFilterValue("selected")));

    StudyViewFilter studyViewFilter = baseFilter();
    studyViewFilter.setCustomDataFilters(List.of(customFilter));
    return context(studyViewFilter, List.of(customSampleIdentifiers));
  }

  private static CustomSampleIdentifier customIdentifier(String sampleId, boolean filteredOut) {
    CustomSampleIdentifier identifier = new CustomSampleIdentifier();
    identifier.setStudyId(STUDY);
    identifier.setSampleId(sampleId);
    identifier.setIsFilteredOut(filteredOut);
    return identifier;
  }

  private static StudyViewFilter baseFilter() {
    StudyViewFilter filter = new StudyViewFilter();
    filter.setStudyIds(List.of(STUDY));
    return filter;
  }

  private static StudyViewFilterContext context(
      StudyViewFilter filter, List<CustomSampleIdentifier> customSampleIdentifiers) {
    return StudyViewFilterFactory.make(filter, customSampleIdentifiers, filter.getStudyIds(), null);
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

  private enum TreatmentKey {
    AGENT,
    GROUP,
    TARGET
  }
}
