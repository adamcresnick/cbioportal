package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.PortalApplication;
import org.cbioportal.application.rest.vcolumnstore.ColumnStoreSampleController;
import org.cbioportal.application.rest.vcolumnstore.ColumnStoreStudyController;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.domain.studyview.StudyViewFilterFactory;
import org.cbioportal.infrastructure.repository.starrocks.cancerstudy.StarrocksCancerStudyMapper;
import org.cbioportal.infrastructure.repository.starrocks.patient.StarrocksPatientMapper;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleMapper;
import org.cbioportal.legacy.web.parameter.Direction;
import org.cbioportal.legacy.web.parameter.SampleIdentifier;
import org.cbioportal.legacy.web.parameter.StudyViewFilter;
import org.cbioportal.legacy.web.parameter.sort.SampleSortBy;
import org.cbioportal.shared.SortAndSearchCriteria;
import org.cbioportal.shared.enums.ProjectionType;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksStudySampleMapperTest {

  @Test
  void allStudyPatientAndSampleStatementsExecuteAgainstFixture() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        StarrocksCancerStudyMapper studies = session.getMapper(StarrocksCancerStudyMapper.class);
        StarrocksPatientMapper patients = session.getMapper(StarrocksPatientMapper.class);
        StarrocksSampleMapper samples = session.getMapper(StarrocksSampleMapper.class);

        var detailedStudies =
            studies.getCancerStudiesMetadata(
                new SortAndSearchCriteria("", "name", "DESC", null, null), List.of());
        assertThat(detailedStudies)
            .extracting(study -> study.cancerStudyIdentifier())
            .containsExactly("sr_study_b", "sr_study_a");
        assertThat(detailedStudies.getFirst().allSampleCount()).isEqualTo(4);
        assertThat(detailedStudies.getFirst().treatmentCount()).isEqualTo(1);
        assertThat(detailedStudies.getFirst().structuralVariantCount()).isEqualTo(1);

        var summaryStudies =
            studies.getCancerStudiesMetadataSummary(
                new SortAndSearchCriteria("SR_A", null, null, null, null), List.of());
        assertThat(summaryStudies)
            .singleElement()
            .satisfies(
                study -> {
                  assertThat(study.cancerStudyIdentifier()).isEqualTo("sr_study_a");
                  assertThat(study.referenceGenome()).isEqualTo("hg38");
                });
        assertThat(studies.getCancerStudiesMetadataSummary(criteria(), List.of("missing")))
            .isEmpty();
        assertThat(studies.getResourceCountsForAllStudies())
            .extracting(resource -> resource.cancerStudyIdentifier())
            .containsOnly("sr_study_a");

        StudyViewFilterContext studyA = studyFilter(List.of("sr_study_a"));
        assertThat(studies.getFilteredStudyIds(studyA)).containsExactly("sr_study_a");
        assertThat(patients.getPatientCount(studyA)).isEqualTo(3);
        assertThat(patients.getCaseListDataCounts(studyA))
            .extracting(count -> count.getValue())
            .containsExactlyInAnyOrder("all", "sequenced", "cna");
        assertThat(samples.getSampleCount(studyA)).isEqualTo(4);
        assertThat(samples.getFilteredSamples(studyA))
            .extracting(sample -> sample.stableId())
            .containsExactly("SA1", "SA2", "SA3", "SA4");
        StudyViewFilter selectedLists = new StudyViewFilter();
        selectedLists.setStudyIds(List.of("sr_study_a"));
        selectedLists.setCaseLists(List.of(List.of("sequenced")));
        selectedLists.setGenomicProfiles(List.of(List.of("mutations")));
        StudyViewFilterContext selectedListContext =
            StudyViewFilterFactory.make(selectedLists, null, selectedLists.getStudyIds(), null);
        assertThat(samples.getFilteredSamples(selectedListContext))
            .extracting(sample -> sample.stableId())
            .containsExactly("SA1", "SA2", "SA3", "SA4");

        StudyViewFilterContext selectedSamples =
            sampleFilter(List.of(identifier("sr_study_a", "SA2"), identifier("sr_study_b", "SB1")));
        assertThat(samples.getFilteredSamples(selectedSamples))
            .extracting(sample -> sample.stableId())
            .containsExactly("SA2", "SB1");

        assertThat(samples.getMetaSamples(List.of("sr_study_a"), null, null, null).getTotalCount())
            .isEqualTo(4);
        assertThat(samples.getMetaSamplesBySampleListIds(List.of("sr_study_b_all")).getTotalCount())
            .isEqualTo(4);
        assertThat(
                samples
                    .getSamples(
                        List.of("sr_study_a", "sr_study_b"),
                        null,
                        List.of("SA1", "SB2"),
                        null,
                        0,
                        0,
                        null,
                        null)
                    .stream()
                    .map(sample -> sample.cancerStudyIdentifier() + "/" + sample.stableId()))
            .containsExactly("sr_study_a/SA1", "sr_study_b/SB2");
        assertThat(
                samples.getSamples(List.of("sr_study_a"), null, List.of(), null, 0, 0, null, null))
            .isEmpty();
        assertThat(
                samples.getSummarySamples(
                    List.of("sr_study_a"), null, null, null, 2, 1, "sampleType", "DESC"))
            .extracting(sample -> sample.stableId())
            .containsExactly("SA3", "SA4");
        assertThat(
                samples.getDetailedSamples(
                    List.of("sr_study_a"), "PA1", null, null, 0, 0, "stableId", "DESC"))
            .allSatisfy(
                sample -> {
                  assertThat(sample.patient()).isNotNull();
                  assertThat(sample.uniqueSampleKey()).isNotBlank();
                })
            .extracting(sample -> sample.stableId())
            .containsExactly("SA2", "SA1");
        assertThat(
                samples.getDetailedSamples(
                    List.of("sr_study_a"), "PA1", null, null, 0, 0, null, "ASC"))
            .extracting(sample -> sample.stableId())
            .containsExactly("SA1", "SA2");

        assertThat(samples.getSamplesBySampleListIds(List.of("sr_study_a_all"))).hasSize(4);
        assertThat(samples.getSummarySamplesBySampleListIds(List.of("sr_study_a_all")))
            .allSatisfy(sample -> assertThat(sample.sampleType()).isNotNull());
        assertThat(samples.getDetailedSamplesBySampleListIds(List.of("sr_study_a_all")))
            .allSatisfy(sample -> assertThat(sample.patient()).isNotNull());
        assertThat(samples.getSample("sr_study_b", "SB1"))
            .satisfies(
                sample -> {
                  assertThat(sample.patientStableId()).isEqualTo("PB1");
                  assertThat(sample.cancerStudyIdentifier()).isEqualTo("sr_study_b");
                });
      }

      try (ConfigurableApplicationContext context = applicationContext(cluster)) {
        var studyResponse =
            context
                .getBean(ColumnStoreStudyController.class)
                .getAllStudies(null, ProjectionType.SUMMARY, null, null, null, Direction.ASC);
        assertThat(studyResponse.getBody())
            .extracting(study -> study.studyId())
            .containsExactly("sr_study_a", "sr_study_b");
        var secondStudyPage =
            context
                .getBean(ColumnStoreStudyController.class)
                .getAllStudies(null, ProjectionType.SUMMARY, null, 1, 1, Direction.ASC);
        assertThat(secondStudyPage.getBody())
            .extracting(study -> study.studyId())
            .containsExactly("sr_study_b");

        var sampleResponse =
            context
                .getBean(ColumnStoreSampleController.class)
                .getAllSamplesInStudy(
                    "sr_study_a",
                    ProjectionType.SUMMARY,
                    2,
                    0,
                    SampleSortBy.sampleId,
                    Direction.DESC);
        assertThat(sampleResponse.getBody())
            .extracting(sample -> sample.sampleId())
            .containsExactly("SA4", "SA3");

        var patientSamplesResponse =
            context
                .getBean(ColumnStoreSampleController.class)
                .getAllSamplesOfPatientInStudy(
                    "sr_study_a", "PA1", ProjectionType.DETAILED, 100, 0, null, Direction.ASC);
        assertThat(patientSamplesResponse.getBody())
            .extracting(sample -> sample.sampleId())
            .containsExactly("SA1", "SA2");
      }
    }
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

  private static SortAndSearchCriteria criteria() {
    return new SortAndSearchCriteria(null, null, null, null, null);
  }

  private static StudyViewFilterContext studyFilter(List<String> studyIds) {
    StudyViewFilter filter = new StudyViewFilter();
    filter.setStudyIds(studyIds);
    return StudyViewFilterFactory.make(filter, null, studyIds, null);
  }

  private static StudyViewFilterContext sampleFilter(List<SampleIdentifier> sampleIdentifiers) {
    StudyViewFilter filter = new StudyViewFilter();
    filter.setSampleIdentifiers(sampleIdentifiers);
    return StudyViewFilterFactory.make(
        filter,
        null,
        sampleIdentifiers.stream().map(SampleIdentifier::getStudyId).distinct().toList(),
        null);
  }

  private static SampleIdentifier identifier(String studyId, String sampleId) {
    SampleIdentifier identifier = new SampleIdentifier();
    identifier.setStudyId(studyId);
    identifier.setSampleId(sampleId);
    return identifier;
  }
}
