package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.PortalApplication;
import org.cbioportal.application.rest.vcolumnstore.ColumnarStoreStudyViewController;
import org.cbioportal.domain.alteration.repository.AlterationRepository;
import org.cbioportal.domain.generic_assay.repository.GenericAssayRepository;
import org.cbioportal.domain.genomic_data.repository.GenomicDataRepository;
import org.cbioportal.domain.sample.Sample;
import org.cbioportal.domain.studyview.StudyViewFilterContext;
import org.cbioportal.domain.studyview.StudyViewFilterFactory;
import org.cbioportal.infrastructure.repository.starrocks.alteration.StarrocksAlterationMapper;
import org.cbioportal.infrastructure.repository.starrocks.alteration.StarrocksAlterationRepository;
import org.cbioportal.infrastructure.repository.starrocks.generic_assay.StarrocksGenericAssayMapper;
import org.cbioportal.infrastructure.repository.starrocks.generic_assay.StarrocksGenericAssayRepository;
import org.cbioportal.infrastructure.repository.starrocks.genomic_data.StarrocksGenomicDataMapper;
import org.cbioportal.infrastructure.repository.starrocks.genomic_data.StarrocksGenomicDataRepository;
import org.cbioportal.infrastructure.repository.starrocks.sample.StarrocksSampleMapper;
import org.cbioportal.legacy.model.AlterationCountByGene;
import org.cbioportal.legacy.model.GeneFilter;
import org.cbioportal.legacy.model.GeneFilterQuery;
import org.cbioportal.legacy.model.GenomicDataCount;
import org.cbioportal.legacy.model.GenomicDataCountItem;
import org.cbioportal.legacy.model.MolecularProfile;
import org.cbioportal.legacy.model.StructuralVariant;
import org.cbioportal.legacy.model.StructuralVariantFilterQuery;
import org.cbioportal.legacy.model.StructuralVariantGeneSubQuery;
import org.cbioportal.legacy.model.StructuralVariantQuery;
import org.cbioportal.legacy.model.StudyViewStructuralVariantFilter;
import org.cbioportal.legacy.model.util.Select;
import org.cbioportal.legacy.persistence.helper.AlterationFilterHelper;
import org.cbioportal.legacy.persistence.mybatis.StructuralVariantMapper;
import org.cbioportal.legacy.web.StructuralVariantController;
import org.cbioportal.legacy.web.parameter.DataFilterValue;
import org.cbioportal.legacy.web.parameter.GenomicDataBinFilter;
import org.cbioportal.legacy.web.parameter.GenomicDataCountFilter;
import org.cbioportal.legacy.web.parameter.GenomicDataFilter;
import org.cbioportal.legacy.web.parameter.MutationDataFilter;
import org.cbioportal.legacy.web.parameter.MutationOption;
import org.cbioportal.legacy.web.parameter.Projection;
import org.cbioportal.legacy.web.parameter.StructuralVariantFilter;
import org.cbioportal.legacy.web.parameter.StudyViewFilter;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksGenomicAggregateMapperTest {

  @Test
  void allAlterationAndGenomicAggregateStatementsExecuteAgainstFixture() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        StarrocksAlterationMapper alteration = session.getMapper(StarrocksAlterationMapper.class);
        StarrocksGenomicDataMapper genomic = session.getMapper(StarrocksGenomicDataMapper.class);
        StarrocksGenericAssayMapper genericAssay =
            session.getMapper(StarrocksGenericAssayMapper.class);
        StarrocksSampleMapper samples = session.getMapper(StarrocksSampleMapper.class);
        StructuralVariantMapper structuralVariants =
            session.getMapper(StructuralVariantMapper.class);
        StudyViewFilterContext studyA = context(studyFilter("sr_study_a"));
        AlterationFilterHelper allAlterations = AlterationFilterHelper.build(null);

        assertThat(alteration.getMutatedGenes(studyA, allAlterations))
            .extracting(
                AlterationCountByGene::getHugoGeneSymbol,
                AlterationCountByGene::getNumberOfAlteredCases)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("TP53", 2),
                org.assertj.core.groups.Tuple.tuple("EGFR", 1));
        assertThat(alteration.getCnaGenes(studyA, allAlterations))
            .singleElement()
            .satisfies(
                count -> {
                  assertThat(count.getHugoGeneSymbol()).isEqualTo("TP53");
                  assertThat(count.getAlteration()).isEqualTo(2);
                  assertThat(count.getNumberOfAlteredCases()).isEqualTo(1);
                });
        assertThat(alteration.getStructuralVariantGenes(studyA, allAlterations))
            .singleElement()
            .satisfies(
                count -> {
                  assertThat(count.getHugoGeneSymbol()).isEqualTo("TP53");
                  assertThat(count.getNumberOfAlteredCases()).isEqualTo(1);
                });

        MolecularProfile mutationProfile = new MolecularProfile();
        mutationProfile.setStableId("sr_study_a_mutations");
        assertThat(
                alteration.getTotalProfiledCounts(
                    studyA, "MUTATION_EXTENDED", List.of(mutationProfile)))
            .extracting(
                AlterationCountByGene::getHugoGeneSymbol,
                AlterationCountByGene::getNumberOfProfiledCases)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("EGFR", 3),
                org.assertj.core.groups.Tuple.tuple("TP53", 3));
        assertThat(alteration.getMatchingGenePanelIds(studyA, "MUTATION_EXTENDED"))
            .extracting(panel -> panel.getGenePanelId() + ":" + panel.getHugoGeneSymbol())
            .containsExactly(
                "WES:BRAF", "TEST_PANEL:EGFR", "WES:EGFR", "TEST_PANEL:TP53", "WES:TP53");
        assertThat(alteration.getSampleProfileCountWithoutPanelData(studyA, "MUTATION_EXTENDED"))
            .isEqualTo(1);
        assertThat(alteration.getGenePanelGenes()).hasSize(5);
        assertThat(
                alteration.getSampleToGenePanels(
                    List.of("sr_study_a_SA1"), List.of("sr_study_a_mutations")))
            .singleElement()
            .satisfies(panel -> assertThat(panel.getGenePanelId()).isEqualTo("TEST_PANEL"));
        assertThat(
                alteration.getPatientToGenePanels(
                    List.of("sr_study_a_PA1"), List.of("sr_study_a_mutations")))
            .singleElement()
            .satisfies(panel -> assertThat(panel.getGenePanelId()).isEqualTo("TEST_PANEL"));
        assertThat(
                alteration.getAlterationCountByGeneGivenSamplesAndMolecularProfiles(
                    List.of("sr_study_a_SA1", "sr_study_a_SA2"),
                    List.of("sr_study_a_mutations"),
                    allAlterations))
            .singleElement()
            .satisfies(count -> assertThat(count.getNumberOfAlteredCases()).isEqualTo(2));
        assertThat(
                alteration.getAlterationCountByGeneGivenPatientsAndMolecularProfiles(
                    List.of("sr_study_a_PA1"), List.of("sr_study_a_mutations"), allAlterations))
            .singleElement()
            .satisfies(count -> assertThat(count.getNumberOfAlteredCases()).isEqualTo(1));
        assertThat(alteration.getAllMolecularProfiles()).hasSize(12);

        assertGenomicStatements(genomic, studyA);
        assertMolecularProfileStatements(genericAssay, studyA);
        assertGenomicStudyViewFilters(samples);
        assertStructuralVariantStatements(structuralVariants);
      }
      assertApplicationContracts(cluster);
    }
  }

  private static void assertGenomicStatements(
      StarrocksGenomicDataMapper genomic, StudyViewFilterContext studyA) {
    assertThat(genomic.getMolecularProfileSampleCounts(studyA))
        .extracting(count -> count.getValue() + ":" + count.getCount())
        .containsExactly("gistic:4", "mrna:4", "mutations:4");

    GenomicDataBinFilter expression = new GenomicDataBinFilter();
    expression.setHugoGeneSymbol("TP53");
    expression.setProfileType("mrna");
    assertThat(genomic.getGenomicDataBinCounts(studyA, List.of(expression)))
        .extracting(count -> count.getValue() + ":" + count.getCount())
        .containsExactly("1.2:1", "NA:3");

    GenomicDataFilter cna = new GenomicDataFilter("TP53", "gistic");
    assertThat(genomic.getCNACounts(studyA, List.of(cna)))
        .singleElement()
        .satisfies(
            item ->
                assertThat(item.getCounts())
                    .extracting(count -> count.getValue() + ":" + count.getCount())
                    .containsExactly("0:1", "2:1", "NA:2"));

    GenomicDataFilter mutation = new GenomicDataFilter("TP53", "mutations");
    assertThat(genomic.getMutationCounts(studyA, mutation))
        .containsEntry("mutatedCount", 2)
        .containsEntry("notMutatedCount", 2)
        .containsEntry("notProfiledCount", 0);
    assertThat(genomic.getMutationCountsByType(studyA, List.of(mutation), false, null))
        .singleElement()
        .satisfies(
            item ->
                assertThat(item.getCounts())
                    .singleElement()
                    .satisfies(
                        count -> {
                          assertThat(count.getValue()).isEqualTo("Missense_Mutation");
                          assertThat(count.getCount()).isEqualTo(2);
                        }));

    List<GenomicDataCountItem> detailedTypes =
        genomic.getMutationCountsByType(studyA, List.of(mutation), true, "TP53");
    assertThat(detailedTypes).hasSize(3);
    assertThat(detailedTypes)
        .flatExtracting(GenomicDataCountItem::getCounts)
        .extracting(GenomicDataCount::getValue, GenomicDataCount::getCount)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple("Missense_Mutation", 2),
            org.assertj.core.groups.Tuple.tuple("NOT_MUTATED", 2),
            org.assertj.core.groups.Tuple.tuple("NOT_PROFILED", 0));
  }

  private static void assertMolecularProfileStatements(
      StarrocksGenericAssayMapper mapper, StudyViewFilterContext studyA) {
    assertThat(mapper.getGenericAssayProfiles()).hasSize(4);
    assertThat(mapper.getFilteredMolecularProfilesByAlterationType(studyA, "MUTATION_EXTENDED"))
        .extracting(MolecularProfile::getStableId)
        .containsExactly("sr_study_a_mutations");
  }

  private static void assertGenomicStudyViewFilters(StarrocksSampleMapper samples) {
    StudyViewFilter cna = studyFilter("sr_study_a");
    cna.setGenomicDataFilters(
        List.of(new GenomicDataFilter("TP53", "gistic", List.of(new DataFilterValue("2")))));
    assertSampleIds(samples, cna, "SA1");

    StudyViewFilter expression = studyFilter("sr_study_a");
    expression.setGenomicDataFilters(
        List.of(
            new GenomicDataFilter(
                "TP53",
                "mrna",
                List.of(new DataFilterValue(new BigDecimal("1"), new BigDecimal("2"))))));
    assertSampleIds(samples, expression, "SA1");

    GeneFilter geneFilter = new GeneFilter();
    geneFilter.setMolecularProfileIds(Set.of("sr_study_a_mutations"));
    GeneFilterQuery tp53 = new GeneFilterQuery();
    tp53.setHugoGeneSymbol("TP53");
    geneFilter.setGeneQueries(List.of(List.of(tp53)));
    StudyViewFilter gene = studyFilter("sr_study_a");
    gene.setGeneFilters(List.of(geneFilter));
    assertSampleIds(samples, gene, "SA1", "SA2");

    StudyViewFilter mutated = studyFilter("sr_study_a");
    mutated.setMutationDataFilters(
        List.of(mutationFilter("TP53", MutationOption.MUTATED, "MUTATED")));
    assertSampleIds(samples, mutated, "SA1", "SA2");

    StudyViewFilter notMutated = studyFilter("sr_study_a");
    notMutated.setMutationDataFilters(
        List.of(mutationFilter("TP53", MutationOption.MUTATED, "NOT_MUTATED")));
    assertSampleIds(samples, notMutated, "SA3", "SA4");

    StudyViewFilter notProfiled = studyFilter("sr_study_a");
    notProfiled.setMutationDataFilters(
        List.of(mutationFilter("BRAF", MutationOption.MUTATED, "NOT_PROFILED")));
    assertSampleIds(samples, notProfiled, "SA1", "SA2", "SA4");

    StudyViewFilter mutationType = studyFilter("sr_study_a");
    mutationType.setMutationDataFilters(
        List.of(mutationFilter("TP53", MutationOption.MUTATION_TYPE, "Missense_Mutation")));
    assertSampleIds(samples, mutationType, "SA1", "SA2");

    StructuralVariantFilterQuery tp53Egfr = structuralVariantFilterQuery(7157, 1956);
    StudyViewStructuralVariantFilter structuralVariantFilter =
        new StudyViewStructuralVariantFilter();
    structuralVariantFilter.setMolecularProfileIds(Set.of("sr_study_a_structural_variants"));
    structuralVariantFilter.setStructVarQueries(List.of(List.of(tp53Egfr)));
    StudyViewFilter structuralVariant = studyFilter("sr_study_a");
    structuralVariant.setStructuralVariantFilters(List.of(structuralVariantFilter));
    assertSampleIds(samples, structuralVariant, "SA1");

    StructuralVariantFilterQuery knownTierOnly = structuralVariantFilterQuery(7157, 1956);
    knownTierOnly.setIncludeUnknownTier(false);
    structuralVariantFilter.setStructVarQueries(List.of(List.of(knownTierOnly)));
    assertSampleIds(samples, structuralVariant);
  }

  private static void assertStructuralVariantStatements(StructuralVariantMapper mapper) {
    StructuralVariantQuery query =
        new StructuralVariantQuery(
            new StructuralVariantGeneSubQuery(7157), new StructuralVariantGeneSubQuery(1956));
    assertThat(
            mapper.fetchStructuralVariants(
                List.of("sr_study_a_structural_variants"),
                List.of("SA1"),
                List.of(7157),
                List.of(query)))
        .singleElement()
        .satisfies(StarrocksGenomicAggregateMapperTest::assertTp53EgfrStructuralVariant);

    assertThat(
            mapper.fetchStructuralVariants(
                List.of("sr_study_a_structural_variants", "sr_study_b_structural_variants"),
                List.of("SA1", "SB1"),
                List.of(),
                List.of()))
        .extracting(variant -> variant.getMolecularProfileId() + ":" + variant.getSampleId())
        .containsExactly(
            "sr_study_b_structural_variants:SB1", "sr_study_a_structural_variants:SA1");

    GeneFilterQuery tp53 = new GeneFilterQuery();
    tp53.setEntrezGeneId(7157);
    assertThat(
            mapper.fetchStructuralVariantsByGeneQueries(
                List.of("sr_study_a_structural_variants"), List.of("SA1"), List.of(tp53)))
        .singleElement()
        .satisfies(StarrocksGenomicAggregateMapperTest::assertTp53EgfrStructuralVariant);
    assertThat(
            mapper.fetchStructuralVariantsByStructVarQueries(
                List.of("sr_study_a_structural_variants"),
                List.of("SA1"),
                List.of(structuralVariantFilterQuery(7157, 1956))))
        .singleElement()
        .satisfies(StarrocksGenomicAggregateMapperTest::assertTp53EgfrStructuralVariant);
  }

  private static void assertApplicationContracts(StarrocksTestCluster cluster) throws Exception {
    try (ConfigurableApplicationContext context = applicationContext(cluster)) {
      assertThat(context.getBean(AlterationRepository.class))
          .isInstanceOf(StarrocksAlterationRepository.class);
      assertThat(context.getBean(GenomicDataRepository.class))
          .isInstanceOf(StarrocksGenomicDataRepository.class);
      assertThat(context.getBean(GenericAssayRepository.class))
          .isInstanceOf(StarrocksGenericAssayRepository.class);
      assertThatThrownBy(
              () ->
                  context
                      .getBean(GenericAssayRepository.class)
                      .getGenericAssayMetaByStableIds(List.of("score")))
          .isInstanceOf(StarrocksDomainNotImplementedException.class)
          .hasMessageContaining("getGenericAssayMetaByStableIds");

      ColumnarStoreStudyViewController controller =
          context.getBean(ColumnarStoreStudyViewController.class);
      assertThat(controller.fetchMutatedGenes(studyFilter("sr_study_a")).getBody())
          .extracting(AlterationCountByGene::getHugoGeneSymbol)
          .containsExactly("TP53", "EGFR");
      assertThat(controller.fetchCnaGenes(studyFilter("sr_study_a")).getBody())
          .singleElement()
          .satisfies(count -> assertThat(count.getHugoGeneSymbol()).isEqualTo("TP53"));
      assertThat(controller.fetchStructuralVariantGenes(studyFilter("sr_study_a")).getBody())
          .singleElement()
          .satisfies(count -> assertThat(count.getHugoGeneSymbol()).isEqualTo("TP53"));
      assertThat(controller.fetchMolecularProfileSampleCounts(studyFilter("sr_study_a")).getBody())
          .extracting(count -> count.getValue() + ":" + count.getCount())
          .containsExactlyInAnyOrder("gistic:4", "mrna:4", "mutations:4");

      GenomicDataCountFilter cnaCounts =
          countFilter(studyFilter("sr_study_a"), new GenomicDataFilter("TP53", "gistic"));
      assertThat(controller.fetchGenomicDataCounts(cnaCounts).getBody())
          .singleElement()
          .satisfies(item -> assertThat(item.getCounts()).hasSize(3));

      GenomicDataCountFilter mutationCounts =
          countFilter(studyFilter("sr_study_a"), new GenomicDataFilter("TP53", "mutations"));
      assertThat(
              controller
                  .fetchMutationDataCounts(Projection.SUMMARY, mutationCounts, false)
                  .getBody())
          .singleElement()
          .satisfies(
              item ->
                  assertThat(item.getCounts())
                      .extracting(GenomicDataCount::getValue, GenomicDataCount::getCount)
                      .containsExactly(
                          org.assertj.core.groups.Tuple.tuple("MUTATED", 2),
                          org.assertj.core.groups.Tuple.tuple("NOT_MUTATED", 2)));
      assertThat(
              controller
                  .fetchMutationDataCounts(
                      Projection.DETAILED,
                      countFilter(
                          studyFilter("sr_study_a"), new GenomicDataFilter("TP53", "mutations")),
                      true)
                  .getBody())
          .hasSize(3);

      StructuralVariantFilter publicFilter = new StructuralVariantFilter();
      publicFilter.setMolecularProfileIds(List.of("sr_study_a_structural_variants"));
      publicFilter.setEntrezGeneIds(List.of(7157));
      publicFilter.setStructuralVariantQueries(List.of());
      StructuralVariantController publicController =
          context.getBean(StructuralVariantController.class);
      assertThat(
              publicController.fetchStructuralVariants(null, publicFilter, publicFilter).getBody())
          .singleElement()
          .satisfies(StarrocksGenomicAggregateMapperTest::assertTp53EgfrStructuralVariant);
    }
  }

  private static GenomicDataCountFilter countFilter(
      StudyViewFilter studyViewFilter, GenomicDataFilter genomicDataFilter) {
    GenomicDataCountFilter filter = new GenomicDataCountFilter();
    filter.setStudyViewFilter(studyViewFilter);
    filter.setGenomicDataFilters(List.of(genomicDataFilter));
    return filter;
  }

  private static StructuralVariantFilterQuery structuralVariantFilterQuery(
      int site1EntrezGeneId, int site2EntrezGeneId) {
    return new StructuralVariantFilterQuery(
        "TP53",
        site1EntrezGeneId,
        "EGFR",
        site2EntrezGeneId,
        true,
        true,
        true,
        Select.all(),
        true,
        true,
        true,
        true);
  }

  private static void assertTp53EgfrStructuralVariant(StructuralVariant variant) {
    assertThat(variant.getStudyId()).isEqualTo("sr_study_a");
    assertThat(variant.getSampleId()).isEqualTo("SA1");
    assertThat(variant.getSite1HugoSymbol()).isEqualTo("TP53");
    assertThat(variant.getSite2HugoSymbol()).isEqualTo("EGFR");
    assertThat(variant.getSvStatus()).isEqualTo("SOMATIC");
  }

  private static MutationDataFilter mutationFilter(
      String gene, MutationOption option, String value) {
    MutationDataFilter filter = new MutationDataFilter();
    filter.setHugoGeneSymbol(gene);
    filter.setProfileType("mutations");
    filter.setCategorization(option);
    filter.setValues(List.of(List.of(new DataFilterValue(value))));
    return filter;
  }

  private static void assertSampleIds(
      StarrocksSampleMapper mapper, StudyViewFilter filter, String... expectedIds) {
    assertThat(mapper.getFilteredSamples(context(filter)))
        .extracting(Sample::stableId)
        .containsExactly(expectedIds);
  }

  private static StudyViewFilter studyFilter(String studyId) {
    StudyViewFilter filter = new StudyViewFilter();
    filter.setStudyIds(List.of(studyId));
    return filter;
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
    Resource[] starrocksResources =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:mappers/starrocks/**/*.xml");
    Resource[] mapperResources = Arrays.copyOf(starrocksResources, starrocksResources.length + 1);
    mapperResources[starrocksResources.length] =
        new ClassPathResource(
            "org/cbioportal/legacy/persistence/mybatis/StructuralVariantMapper.xml");
    factory.setMapperLocations(mapperResources);
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
