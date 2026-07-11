package org.cbioportal.infrastructure.repository.starrocks.mutation;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.cbioportal.domain.mutation.repository.MutationRepository;
import org.cbioportal.legacy.model.Mutation;
import org.cbioportal.legacy.model.meta.MutationMeta;
import org.cbioportal.legacy.persistence.mybatis.MutationMapper;
import org.cbioportal.legacy.persistence.mybatis.util.PaginationCalculator;
import org.cbioportal.shared.MutationQueryOptions;
import org.cbioportal.shared.enums.ProjectionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksMutationRepository implements MutationRepository {

  private final MutationMapper mapper;

  public StarrocksMutationRepository(MutationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<Mutation> getMutationsInMultipleMolecularProfiles(
      List<String> molecularProfileIds,
      List<String> sampleIds,
      List<Integer> entrezGeneIds,
      MutationQueryOptions options) {
    if (CollectionUtils.isEmpty(molecularProfileIds)
        || options.projection() == ProjectionType.META) {
      return List.of();
    }
    validateProfileSamplePairs(molecularProfileIds, sampleIds);

    return mapper.getMutationsInMultipleMolecularProfiles(
        molecularProfileIds,
        sampleIds,
        entrezGeneIds,
        false,
        options.projection().name(),
        options.pageSize(),
        PaginationCalculator.offset(options.pageSize(), options.pageNumber()),
        options.sortBy(),
        options.direction() == null ? null : options.direction().name());
  }

  @Override
  public MutationMeta getMetaMutationsInMultipleMolecularProfiles(
      List<String> molecularProfileIds, List<String> sampleIds, List<Integer> entrezGeneIds) {
    if (CollectionUtils.isEmpty(molecularProfileIds)) {
      MutationMeta meta = new MutationMeta();
      meta.setTotalCount(0);
      meta.setSampleCount(0);
      return meta;
    }
    validateProfileSamplePairs(molecularProfileIds, sampleIds);
    return mapper.getMetaMutationsInMultipleMolecularProfiles(
        molecularProfileIds, sampleIds, entrezGeneIds, false);
  }

  private static void validateProfileSamplePairs(
      List<String> molecularProfileIds, List<String> sampleIds) {
    if (CollectionUtils.isEmpty(sampleIds)
        || molecularProfileIds.stream().distinct().count() == 1) {
      return;
    }
    if (molecularProfileIds.size() != sampleIds.size()) {
      throw new IllegalArgumentException(
          "Multiple molecular profiles require one molecular profile ID per sample ID");
    }
  }
}
