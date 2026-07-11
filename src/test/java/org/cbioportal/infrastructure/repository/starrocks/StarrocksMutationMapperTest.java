package org.cbioportal.infrastructure.repository.starrocks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.cbioportal.PortalApplication;
import org.cbioportal.application.rest.response.MutationDTO;
import org.cbioportal.application.rest.vcolumnstore.ColumnStoreMutationController;
import org.cbioportal.domain.mutation.repository.MutationRepository;
import org.cbioportal.infrastructure.repository.starrocks.mutation.StarrocksMutationRepository;
import org.cbioportal.legacy.model.GeneFilterQuery;
import org.cbioportal.legacy.model.GenomicDataCountItem;
import org.cbioportal.legacy.model.Mutation;
import org.cbioportal.legacy.model.MutationCountByPosition;
import org.cbioportal.legacy.model.meta.MutationMeta;
import org.cbioportal.legacy.persistence.mybatis.MutationMapper;
import org.cbioportal.legacy.web.MutationController;
import org.cbioportal.legacy.web.parameter.Direction;
import org.cbioportal.legacy.web.parameter.HeaderKeyConstants;
import org.cbioportal.legacy.web.parameter.MutationFilter;
import org.cbioportal.legacy.web.parameter.MutationMultipleStudyFilter;
import org.cbioportal.legacy.web.parameter.Projection;
import org.cbioportal.legacy.web.parameter.sort.MutationSortBy;
import org.cbioportal.shared.MutationQueryOptions;
import org.cbioportal.shared.enums.ProjectionType;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class StarrocksMutationMapperTest {

  @Test
  void allMutationStatementsAndControllerPathsExecuteAgainstFixture() throws Exception {
    try (StarrocksTestCluster cluster = new StarrocksTestCluster()) {
      cluster.start();
      cluster.recreateSchema();
      cluster.loadFixture();

      try (SqlSession session = sessionFactory(cluster).openSession()) {
        assertMapperContracts(session.getMapper(MutationMapper.class));
      }
      assertApplicationContracts(cluster);
    }
  }

  private static void assertMapperContracts(MutationMapper mapper) {
    List<Mutation> detailed =
        mapper.getMutationsBySampleListId(
            "sr_study_a_mutations",
            "sr_study_a_sequenced",
            List.of(),
            false,
            "DETAILED",
            100,
            0,
            "tumorAltCount",
            "DESC");
    assertThat(detailed).hasSize(3);
    assertDetailedR175h(detailed.getFirst());

    MutationMeta sampleListMeta =
        mapper.getMetaMutationsBySampleListId(
            "sr_study_a_mutations", "sr_study_a_sequenced", List.of(), false);
    assertMeta(sampleListMeta, 3, 3);

    List<String> pairedProfiles = List.of("sr_study_a_mutations", "sr_study_b_mutations");
    List<String> pairedSamples = List.of("SA1", "SB1");
    assertThat(
            mapper.getMutationsInMultipleMolecularProfiles(
                pairedProfiles, pairedSamples, List.of(), false, "SUMMARY", 100, 0, null, "ASC"))
        .extracting(mutation -> mutation.getMolecularProfileId() + ":" + mutation.getSampleId())
        .containsExactly("sr_study_a_mutations:SA1", "sr_study_b_mutations:SB1");

    assertThat(
            mapper.getMutationsInMultipleMolecularProfiles(
                List.of(), List.of(), List.of(), false, "ID", 100, 0, null, "ASC"))
        .isEmpty();

    GeneFilterQuery tp53 = new GeneFilterQuery();
    tp53.setEntrezGeneId(7157);
    assertThat(
            mapper.getMutationsInMultipleMolecularProfilesByGeneQueries(
                pairedProfiles,
                null,
                false,
                "SUMMARY",
                100,
                0,
                "proteinChange",
                "ASC",
                List.of(tp53)))
        .extracting(Mutation::getProteinChange)
        .containsExactly("R175H", "R175H", "R248Q");

    assertMeta(
        mapper.getMetaMutationsInMultipleMolecularProfiles(
            pairedProfiles, pairedSamples, List.of(), false),
        2,
        2);
    assertMeta(
        mapper.getMetaMutationsBySampleIds(
            "sr_study_a_mutations", List.of("SA1", "SA2"), List.of(7157), false),
        2,
        2);

    MutationCountByPosition position = mapper.getMutationCountByPosition(7157, 175, 175);
    assertThat(position.getCount()).isEqualTo(2);

    GenomicDataCountItem mutationTypes =
        mapper.getMutationCountsByType(
            pairedProfiles, pairedSamples, List.of(), false, "mutations");
    assertThat(mutationTypes.getProfileType()).isEqualTo("mutations");
    assertThat(mutationTypes.getCounts())
        .singleElement()
        .satisfies(
            count -> {
              assertThat(count.getValue()).isEqualTo("Missense_Mutation");
              assertThat(count.getLabel()).isEqualTo("Missense Mutation");
              assertThat(count.getCount()).isEqualTo(2);
              assertThat(count.getUniqueCount()).isEqualTo(2);
            });
  }

  private static void assertApplicationContracts(StarrocksTestCluster cluster) throws Exception {
    try (ConfigurableApplicationContext context = applicationContext(cluster)) {
      MutationRepository repository = context.getBean(MutationRepository.class);
      assertThat(repository).isInstanceOf(StarrocksMutationRepository.class);

      MutationQueryOptions detailedOptions =
          new MutationQueryOptions(ProjectionType.DETAILED, 1, 0, "tumorAltCount", Direction.DESC);
      assertThat(
              repository.getMutationsInMultipleMolecularProfiles(
                  List.of("sr_study_a_mutations"),
                  List.of("SA1", "SA2"),
                  List.of(),
                  detailedOptions))
          .singleElement()
          .satisfies(StarrocksMutationMapperTest::assertDetailedR175h);
      assertMeta(
          repository.getMetaMutationsInMultipleMolecularProfiles(
              List.of("sr_study_a_mutations"), List.of("SA1", "SA2"), List.of()),
          2,
          2);

      MutationMultipleStudyFilter filter = new MutationMultipleStudyFilter();
      filter.setMolecularProfileIds(List.of("sr_study_a_mutations"));
      filter.setEntrezGeneIds(List.of(7157));

      ColumnStoreMutationController domainController =
          context.getBean(ColumnStoreMutationController.class);
      List<MutationDTO> domainResponse =
          domainController
              .fetchMutationsInMultipleMolecularProfiles(
                  null,
                  filter,
                  ProjectionType.DETAILED,
                  100,
                  0,
                  MutationSortBy.proteinChange,
                  Direction.ASC)
              .getBody();
      assertThat(domainResponse)
          .hasSize(2)
          .allSatisfy(mutation -> assertThat(mutation.entrezGeneId()).isEqualTo(7157));
      assertThat(
              domainController
                  .fetchMutationsInMultipleMolecularProfiles(
                      null, filter, ProjectionType.META, 100, 0, null, Direction.ASC)
                  .getHeaders()
                  .getFirst(HeaderKeyConstants.TOTAL_COUNT))
          .isEqualTo("2");

      MutationController publicController = context.getBean(MutationController.class);
      List<Mutation> publicResponse =
          publicController
              .fetchMutationsInMultipleMolecularProfiles(
                  null,
                  filter,
                  filter,
                  Projection.DETAILED,
                  100,
                  0,
                  MutationSortBy.proteinChange,
                  Direction.ASC)
              .getBody();
      assertThat(publicResponse)
          .hasSize(2)
          .allSatisfy(mutation -> assertThat(mutation.getEntrezGeneId()).isEqualTo(7157));
      assertThat(
              publicController
                  .fetchMutationsInMultipleMolecularProfiles(
                      null, filter, filter, Projection.META, 100, 0, null, Direction.ASC)
                  .getHeaders()
                  .getFirst(HeaderKeyConstants.SAMPLE_COUNT))
          .isEqualTo("2");

      assertThat(
              publicController
                  .getMutationsInMolecularProfileBySampleListId(
                      "sr_study_a_mutations",
                      "sr_study_a_sequenced",
                      7157,
                      Projection.SUMMARY,
                      100,
                      0,
                      MutationSortBy.proteinChange,
                      Direction.ASC)
                  .getBody())
          .extracting(Mutation::getSampleId)
          .containsExactly("SA1", "SA2");

      MutationFilter sampleFilter = new MutationFilter();
      sampleFilter.setSampleIds(List.of("SA1"));
      sampleFilter.setEntrezGeneIds(List.of(7157));
      assertThat(
              publicController
                  .fetchMutationsInMolecularProfile(
                      "sr_study_a_mutations",
                      sampleFilter,
                      Projection.DETAILED,
                      100,
                      0,
                      null,
                      Direction.ASC)
                  .getBody())
          .singleElement()
          .satisfies(StarrocksMutationMapperTest::assertDetailedR175h);
    }
  }

  private static void assertDetailedR175h(Mutation mutation) {
    assertThat(mutation.getSampleId()).isEqualTo("SA1");
    assertThat(mutation.getProteinChange()).isEqualTo("R175H");
    assertThat(mutation.getTumorAltCount()).isEqualTo(40);
    assertThat(mutation.getDriverFilter()).isEqualTo("Putative_Driver");
    assertThat(mutation.getGene()).isNotNull();
    assertThat(mutation.getGene().getHugoGeneSymbol()).isEqualTo("TP53");
    assertThat(mutation.getAlleleSpecificCopyNumber()).isNotNull();
    assertThat(mutation.getAlleleSpecificCopyNumber().getAscnMethod()).isEqualTo("ABSOLUTE");
  }

  private static void assertMeta(MutationMeta meta, int totalCount, int sampleCount) {
    assertThat(meta.getTotalCount()).isEqualTo(totalCount);
    assertThat(meta.getSampleCount()).isEqualTo(sampleCount);
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
        new ClassPathResource("org/cbioportal/legacy/persistence/mybatis/MutationMapper.xml"));
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
