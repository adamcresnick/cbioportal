package org.cbioportal.infrastructure.repository.starrocks.coexpression;

import java.util.Comparator;
import java.util.List;
import org.cbioportal.domain.coexpression.repository.CoExpressionRepository;
import org.cbioportal.infrastructure.repository.clickhouse.coexpression.CoExpressionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "columnstore.backend", havingValue = "starrocks")
public class StarrocksCoExpressionRepository implements CoExpressionRepository {

  private final StarrocksCoExpressionMapper mapper;

  public StarrocksCoExpressionRepository(StarrocksCoExpressionMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public List<CoExpressionResult> getCoExpressions(
      String cancerStudyIdentifierA,
      String profileTypeA,
      String cancerStudyIdentifierB,
      String profileTypeB,
      String hugoGeneSymbol,
      List<String> sampleUniqueIds,
      Double threshold) {
    double minimumCorrelation = threshold == null ? 0 : threshold;
    return mapper
        .getCoExpressionAggregates(
            cancerStudyIdentifierA,
            profileTypeA,
            cancerStudyIdentifierB,
            profileTypeB,
            hugoGeneSymbol,
            sampleUniqueIds)
        .stream()
        .map(row -> toResult(row, minimumCorrelation))
        .filter(result -> result != null)
        .sorted(Comparator.comparing(CoExpressionResult::getEntrezGeneId))
        .toList();
  }

  private static CoExpressionResult toResult(
      StarrocksCoExpressionAggregateRow row, double threshold) {
    boolean valid =
        row.getNumSamples() >= 3
            && row.getDistinctGeneValues() > 1
            && row.getDistinctGeneValues() > row.getNumSamples() / 2.0;

    Double correlation = null;
    if (valid) {
      correlation = parseCorrelation(row.getSpearmansCorrelation());
      if (correlation == null
          || !Double.isFinite(correlation)
          || Math.abs(correlation) < threshold) {
        return null;
      }
    }

    CoExpressionResult result = new CoExpressionResult();
    result.setEntrezGeneId(row.getEntrezGeneId());
    result.setSpearmansCorrelation(correlation);
    result.setNumSamples(row.getNumSamples());
    return result;
  }

  private static Double parseCorrelation(String value) {
    if (value == null) {
      return null;
    }
    if (value.equalsIgnoreCase("nan")) {
      return Double.NaN;
    }
    if (value.equalsIgnoreCase("inf") || value.equalsIgnoreCase("infinity")) {
      return Double.POSITIVE_INFINITY;
    }
    if (value.equalsIgnoreCase("-inf") || value.equalsIgnoreCase("-infinity")) {
      return Double.NEGATIVE_INFINITY;
    }
    return Double.valueOf(value);
  }
}
